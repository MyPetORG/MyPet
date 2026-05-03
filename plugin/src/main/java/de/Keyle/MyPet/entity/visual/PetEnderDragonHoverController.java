package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>Per-pet scheduling follows {@link WitherAutonomousAttackSuppressor}:
 * {@code mob.getScheduler().runAtFixedRate(...)} runs on the entity's
 * region thread on Folia and on the main thread on Paper, and is
 * canceled on despawn / pet-type conversion / removal.
 */
public final class PetEnderDragonHoverController {

    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    /** Hover this many blocks above the owner so the dragon doesn't crush them. */
    private static final double HOVER_HEIGHT = 6.0;

    /** Squared distance from desired pose below which we stop nudging. */
    private static final double SETTLE_DISTANCE_SQ = 1.0; // 1 block

    /** Beyond this raw distance the dragon snaps to the desired pose instead of lerping. */
    private static final double SNAP_DISTANCE = 32.0;

    /** Per-tick lerp step toward the desired pose (blocks/tick). */
    private static final double LERP_STEP = 0.6;

    private PetEnderDragonHoverController() {
    }

    public static void startForPet(MyPet pet) {
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

    private static void tick(EnderDragon dragon, MyPet pet) {
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
        if (pet.getMyPetTarget() != null && !pet.getMyPetTarget().isDead()) return;

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

        dragon.teleportAsync(next);
    }

    public static void stopForPet(MyPet pet) {
        ScheduledTask task = tasks.remove(pet.getUUID());
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }
}
