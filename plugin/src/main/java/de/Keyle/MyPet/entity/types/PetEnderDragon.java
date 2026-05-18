/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorHelpers;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.PetLavaEntity;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@DefaultInfo(food = {Material.END_STONE}, leashFlags = {"Impossible"}, fallbackIconMaterial = "DRAGON_EGG")
public class PetEnderDragon extends PetImpl implements PetLavaEntity, PetFlyingEntity {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("EnderDragon", "CanFly", true);
    public static final ConfigKey<Boolean> GRANT_END_ADVANCEMENT_ON_KILL = ConfigKey.bool("EnderDragon", "GrantEndAdvancementOnKill", false);
    public static final ConfigKey<Boolean> ALLOW_BLOCK_DAMAGE = ConfigKey.bool("EnderDragon", "AllowBlockDamage", false);
    public static final ConfigKey<Boolean> ALLOW_PLAYER_CONTACT_DAMAGE = ConfigKey.bool("EnderDragon", "AllowPlayerContactDamage", false);
    public static final ConfigKey<Boolean> ALLOW_ENTITY_CONTACT_DAMAGE = ConfigKey.bool("EnderDragon", "AllowEntityContactDamage", false);

    /**
     * Suppresses block destruction caused by a pet EnderDragon brushing
     * terrain. Vanilla {@code EnderDragon#aiStep} runs {@code checkWalls}
     * for every sub-entity (head, neck, body, three tails, two wings) every
     * tick and removes non-{@code DRAGON_IMMUNE} blocks their bounding
     * boxes overlap. Paper fires this as an {@link EntityExplodeEvent}
     * with the dragon as source.
     */
    public static final PetBehavior<EntityExplodeEvent> BRUSH_DESTRUCTION_SUPPRESS =
            PetBehaviorHelpers.onPetExplodes("EnderDragon", EventPriority.LOW, true,
                    (event, pet, mob) -> {
                        if (!ALLOW_BLOCK_DAMAGE.get()) {
                            event.setCancelled(true);
                        }
                    });

    /**
     * Suppresses contact damage dealt by a pet EnderDragon. Vanilla
     * {@code EnderDragon#aiStep} damages entities its body brushes against;
     * modern Paper fires this once per victim. Splits the decision by
     * victim type (player vs non-player) via {@link #ALLOW_PLAYER_CONTACT_DAMAGE}
     * / {@link #ALLOW_ENTITY_CONTACT_DAMAGE}.
     */
    public static final PetBehavior<EntityDamageByEntityEvent> CONTACT_DAMAGE_GATE =
            PetBehaviorHelpers.onPetDamages("EnderDragon", EventPriority.HIGH, true,
                    (event, pet, mob) -> {
                        boolean victimIsPlayer = event.getEntity() instanceof Player;
                        if (victimIsPlayer
                                ? ALLOW_PLAYER_CONTACT_DAMAGE.get()
                                : ALLOW_ENTITY_CONTACT_DAMAGE.get()) {
                            return;
                        }
                        event.setCancelled(true);
                    });

    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "EnderDragon",
            HoverController::startForPet,
            HoverController::stopForPet
    );

    public static final Supplier<Listener> ADVANCEMENT_LISTENER =
            PetListenerRegistry.register(AdvancementListener::new);

    public PetEnderDragon(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Suppresses the {@code minecraft:end/kill_dragon} ("Free the End")
     * advancement when a player kills an EnderDragon pet.
     *
     * <p>Vanilla's kill-trigger fires on any {@code ender_dragon} entity death,
     * regardless of whether it's the boss, a custom-summoned dragon, or a pet.
     * The trigger fires inside {@code LivingEntity#die}, which runs <i>before</i>
     * {@link org.bukkit.event.entity.EntityDeathEvent} — so we can't gate from
     * the death listener; by then the advancement is already granted.
     *
     * <p>The flow is therefore:
     * <ol>
     *   <li>{@link EntityDamageByEntityEvent} fires when a player damages a
     *   pet EnderDragon. We mark the player UUID in {@link #recentPetDragonKillers}
     *   regardless of whether the hit is lethal — false positives on the
     *   advancement-grant side are harmless because the only check is
     *   "did the player damage a pet dragon recently AND are they being granted
     *   the dragon-kill criterion right now."</li>
     *
     *   <li>The marking is cleared after 5 ticks via
     *   {@code player.getScheduler().runDelayed} so unrelated future grants
     *   (a real dragon kill in the End some time later) are unaffected.</li>
     *
     *   <li>{@link PlayerAdvancementCriterionGrantEvent} cancels when the
     *   advancement key matches {@link #END_KILL_DRAGON} and the player is
     *   in the recent-killer set.</li>
     * </ol>
     *
     * <p>The {@link PetEnderDragon#GRANT_END_ADVANCEMENT_ON_KILL} flag is checked
     * at both event handlers — when {@code true}, both bail early so vanilla
     * behavior is preserved.
     */
    public static final class AdvancementListener implements Listener {

        private static final NamespacedKey END_KILL_DRAGON = NamespacedKey.minecraft("end/kill_dragon");

        private static final Set<UUID> recentPetDragonKillers = ConcurrentHashMap.newKeySet();

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPetDragonDamage(EntityDamageByEntityEvent event) {
            if (GRANT_END_ADVANCEMENT_ON_KILL.get()) return;
            if (!(event.getEntity() instanceof EnderDragon)) return;
            if (!PetEntityMarker.isMarked(event.getEntity())) return;

            Player killer = resolvePlayerDamager(event.getDamager());
            if (killer == null) return;

            UUID id = killer.getUniqueId();
            recentPetDragonKillers.add(id);
            killer.getScheduler().runDelayed(MyPetApi.getPlugin(),
                    t -> recentPetDragonKillers.remove(id),
                    () -> recentPetDragonKillers.remove(id),
                    5L);
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
            if (GRANT_END_ADVANCEMENT_ON_KILL.get()) return;
            if (!END_KILL_DRAGON.equals(event.getAdvancement().getKey())) return;
            if (!recentPetDragonKillers.contains(event.getPlayer().getUniqueId())) return;
            event.setCancelled(true);
        }

        private static Player resolvePlayerDamager(Entity damager) {
            if (damager instanceof Player p) return p;
            if (damager instanceof Projectile projectile) {
                ProjectileSource source = projectile.getShooter();
                if (source instanceof Player p) return p;
            }
            return null;
        }
    }

    /**
     * Drives an EnderDragon pet's movement and phase state every tick.
     *
     * <p><b>Why this exists:</b> EnderDragon is the only vanilla mob whose
     * {@code aiStep()} is a complete override that does NOT call
     * {@code super.aiStep()}. As a result, {@code Mob#serverAiStep} —
     * and with it the {@code goalSelector.tick()} call — never runs on the
     * dragon, so MyPet's {@code PetFollowOwnerGoal} (and every other goal
     * installed by {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller}) is
     * silently dead. All movement intent has to come from outside the
     * goal pipeline; this controller is that source.
     *
     * <p>Why teleport-based, not velocity-based: the vanilla flight code in
     * {@code EnderDragon#aiStep} (lines 251-292 of the decompiled source)
     * tracks {@code phaseManager.getCurrentPhase().getFlyTargetLocation()}
     * and corrects {@code deltaMovement} every tick to fly the dragon back
     * toward that target. {@link EnderDragon.Phase#HOVER}'s target is
     * captured from the dragon's position the first tick after a phase
     * transition and then never updated — so any external {@code
     * setVelocity} call that displaces the dragon is undone on the next
     * tick by the flight tracker pulling it back to its capture point. The
     * Bukkit API doesn't expose a way to update HOVER's captured target,
     * and the only Bukkit-visible phases that don't run the flight tracker
     * are the SITTING phases — which freeze the dragon (no movement at
     * all) and break sub-entity (head/wing/tail) position tracking. So
     * we drive position directly via per-tick teleport instead, lerping
     * toward a position above the owner.
     *
     * <p>Phase suppression is still applied: HOVER prevents the default
     * {@code HOLDING_PATTERN} from flying the dragon to the End podium
     * ({@code (0, 60, 0)}) between teleport ticks. The captured-target
     * anchor is harmless because our teleport overrides position anyway.
     *
     * <p>Per-pet scheduling follows {@link PetWither.AutonomousAttackSuppressor}:
     * {@code mob.getScheduler().runAtFixedRate(...)} runs on the entity's
     * region thread on Folia and on the main thread on Paper, and is
     * canceled on despawn / pet-type conversion / removal.
     */
    public static final class HoverController {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        /**
         * Vertical offset (in blocks) between the owner's feet and the dragon's
         * feet. Bukkit positions entities by their bounding-box bottom, so this is
         * the visible empty-air gap. Capped at 3 so the dragon's nearest sub-part
         * stays within survival reach (eye height ~1.62 + reach ~3 = 4.62 blocks
         * above feet) for right-click interactions like the Ride-skill mount.
         */
        private static final double HOVER_HEIGHT = 3.75;

        /** Squared distance from desired pose below which we stop nudging. */
        private static final double SETTLE_DISTANCE_SQ = 1.0; // 1 block

        /** Beyond this raw distance the dragon snaps to the desired pose instead of lerping. */
        private static final double SNAP_DISTANCE = 32.0;

        /** Per-tick lerp step toward the desired pose (blocks/tick). */
        private static final double LERP_STEP = 0.6;

        /** Base per-tick teleport step while ridden, before Ride-skill speed bonus. */
        private static final double RIDE_BASE_STEP = 0.6;

        /** Vertical step (blocks/tick) when the rider holds jump/sneak while flying. */
        private static final double RIDE_VERTICAL_STEP = 0.5;

        /**
         * Vertical span (in blocks) of the collision check column at the proposed
         * teleport position. Covers the dragon's body (~3-4 blocks tall around the
         * position anchor) so the dragon can't phase its body through walls/ceilings/
         * floors. Wings/tail are intentionally excluded — they extend horizontally
         * past the body and would make any movement near terrain feel sticky, while
         * checkWalls suppression already prevents them from destroying blocks.
         */
        private static final int COLLISION_CHECK_HEIGHT = 4;

        private HoverController() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof EnderDragon dragon)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    tick(dragon, pet);
                } catch (Throwable ignored) {
                }
            }, null, 1L, 1L);
            if (task != null) {
                tasks.put(key, task);
            }
        }

        private static void tick(EnderDragon dragon, Pet pet) {
            if (dragon.isDead()) return;

            // Phase suppression — keep HOVER active so the phase manager
            // doesn't fly the dragon back to the End podium.
            if (dragon.getPhase() != EnderDragon.Phase.HOVER) {
                dragon.setPhase(EnderDragon.Phase.HOVER);
            }

            if (pet.getOwner() == null) return;
            Player owner = pet.getOwner().getPlayer();
            if (owner == null || !owner.isOnline()) return;
            if (!owner.getWorld().equals(dragon.getWorld())) return;
            if (!pet.canMove()) return;
            if (pet.getPetTarget() != null && !pet.getPetTarget().isDead()) return;

            // Branch on passenger presence. While ridden, the rider's location is
            // ON the dragon, so the follow logic ("teleport to HOVER_HEIGHT above
            // owner") would jet the dragon endlessly skyward. We instead drive
            // movement from the rider's WASD/jump/sneak input — see tickRide.
            if (!dragon.getPassengers().isEmpty()) {
                tickRide(dragon, pet);
                return;
            }

            Location ownerLoc = owner.getLocation();
            Location dragonLoc = dragon.getLocation();
            Location desired = ownerLoc.clone().add(0, HOVER_HEIGHT, 0);

            Vector toDesired = desired.toVector().subtract(dragonLoc.toVector());
            double distSq = toDesired.lengthSquared();
            if (distSq < SETTLE_DISTANCE_SQ) return;

            double dist = Math.sqrt(distSq);

            Location next;
            if (dist > SNAP_DISTANCE) {
                next = desired;
            } else {
                double step = Math.min(LERP_STEP, dist);
                Vector stepVec = toDesired.multiply(step / dist);
                next = dragonLoc.clone().add(stepVec);
            }

            Vector toOwner = ownerLoc.toVector().subtract(next.toVector());
            if (toOwner.lengthSquared() > 1.0E-4) {
                // EnderDragon's vanilla aiStep treats (sin(yaw), _, -cos(yaw)) as
                // forward — at yaw=0 that's -Z. Standard Bukkit yaw convention is
                // yaw=0 = +Z. The model's head therefore renders 180° opposite of
                // Location.setDirection's view vector, so invert before passing.
                next.setDirection(toOwner.multiply(-1));
            } else {
                next.setYaw(dragonLoc.getYaw());
                next.setPitch(dragonLoc.getPitch());
            }

            safeTeleport(dragon, next);
        }

        /**
         * Drives the dragon while ridden. Translates rider WASD/jump/sneak input
         * into a per-tick teleport. Yaw is set to {@code riderYaw + 180} to match
         * the EnderDragon model's reversed forward axis (same convention as the
         * follow path's {@code setDirection(toOwner.multiply(-1))}). Pitch on the
         * dragon is forced flat — the rider's pitch instead steers vertical
         * movement of the body so looking up climbs and looking down dives.
         *
         * <p>{@code RideSkillFlightController} skips EnderDragons because its
         * {@code setVelocity} approach is damped by EnderDragon's vanilla aiStep
         * friction (~0.8/tick). Teleport sidesteps that.
         */
        private static void tickRide(EnderDragon dragon, Pet pet) {
            Entity passenger = dragon.getPassengers().get(0);
            if (!(passenger instanceof Player rider)) return;

            Ride rideSkill;
            try {
                rideSkill = pet.getSkills().get(Ride.class);
            } catch (Throwable t) {
                return;
            }
            if (rideSkill == null) return;
            if (rideSkill.getActive() == null || rideSkill.getActive().getValue() == null
                    || !rideSkill.getActive().getValue()) {
                return;
            }

            Input input;
            try {
                input = rider.getCurrentInput();
            } catch (Throwable t) {
                return;
            }
            if (input == null) return;

            int speedIncrease = rideSkill.getSpeedIncrease() != null
                    && rideSkill.getSpeedIncrease().getValue() != null
                    ? rideSkill.getSpeedIncrease().getValue() : 0;
            double step = RIDE_BASE_STEP * (1.0 + speedIncrease / 100.0);

            float yaw = rider.getLocation().getYaw();
            float pitch = rider.getLocation().getPitch();

            // Local input axes: fx = forward/back, fz = strafe.
            double fx = 0;
            double fz = 0;
            if (input.isForward()) fx += 1;
            if (input.isBackward()) fx -= 0.5;
            if (input.isLeft()) fz -= 0.5;
            if (input.isRight()) fz += 0.5;

            double radYaw = Math.toRadians(yaw);
            double radPitch = Math.toRadians(pitch);

            // Forward vector includes pitch so look-up climbs, look-down dives.
            // Bukkit yaw=0 → +Z, but EnderDragon's "forward" is -Z; we apply the
            // 180° rotation to the model only (setYaw below), keeping the world
            // movement aligned with the rider's facing.
            double cosPitch = Math.cos(radPitch);
            double horizontalForward = fx * cosPitch;
            double verticalForward = -fx * Math.sin(radPitch);

            double worldX = -Math.sin(radYaw) * horizontalForward - Math.cos(radYaw) * fz;
            double worldZ = Math.cos(radYaw) * horizontalForward - Math.sin(radYaw) * fz;
            double worldY = verticalForward;

            if (input.isJump()) worldY += RIDE_VERTICAL_STEP / step;
            if (input.isSneak()) worldY -= RIDE_VERTICAL_STEP / step;

            Location next = dragon.getLocation();
            next.add(worldX * step, worldY * step, worldZ * step);
            next.setYaw(yaw + 180);
            next.setPitch(0);

            safeTeleport(dragon, next);
        }

        /**
         * Teleport guarded by a destination-column collision check. Per-tick
         * movement is bounded (LERP_STEP / RIDE_BASE_STEP ≈ 0.6-1 blocks), well
         * below 1, so a destination check is sufficient — there's no path-skipping
         * to worry about. If the destination column contains a solid block, the
         * positional component is dropped and only rotation is applied; the dragon
         * still turns to face where it would have moved, but doesn't phase
         * through the wall.
         */
        private static void safeTeleport(EnderDragon dragon, Location next) {
            if (wouldCollideAt(dragon.getWorld(), next)) {
                Location rotationOnly = dragon.getLocation();
                rotationOnly.setYaw(next.getYaw());
                rotationOnly.setPitch(next.getPitch());
                dragon.teleportAsync(rotationOnly);
                return;
            }
            dragon.teleportAsync(next);
        }

        private static boolean wouldCollideAt(World world, Location loc) {
            int x = loc.getBlockX();
            int z = loc.getBlockZ();
            int y0 = loc.getBlockY();
            for (int dy = 0; dy < COLLISION_CHECK_HEIGHT; dy++) {
                if (world.getBlockAt(x, y0 + dy, z).getType().isSolid()) {
                    return true;
                }
            }
            return false;
        }

        public static void stopForPet(Pet pet) {
            ScheduledTask task = tasks.remove(pet.getUUID());
            if (task != null) {
                try {
                    task.cancel();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
