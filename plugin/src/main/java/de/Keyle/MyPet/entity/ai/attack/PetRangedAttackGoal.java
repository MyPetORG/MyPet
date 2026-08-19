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

package de.Keyle.MyPet.entity.ai.attack;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.model.PetModelAnimation;
import de.Keyle.MyPet.entity.model.PetModelService;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Ranged;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} that keeps distance from the pet's current target and
 * fires projectiles on a fixed cooldown.
 *
 * <p>Uses Paper's {@code Mob.launchProjectile(...)} together with
 * {@link PersistentDataContainer} tagging to attach per-shot state to the
 * fired projectile. Arrows and tridents carry their damage natively via
 * {@link AbstractArrow#setDamage(double)}; every other projectile type
 * (throwables, fireballs, llama spit) carries the damage as the float
 * {@link #PROJECTILE_DAMAGE_KEY} PDC tag and is resolved at hit time by
 * {@link PetProjectileHitListener}. Every Pet-fired projectile also
 * carries the owner's UUID as the string {@link #PROJECTILE_OWNER_KEY}
 * tag so downstream listeners can attribute damage back to the owning
 * pet via {@link #getSourcePet(org.bukkit.entity.Projectile)}.
 *
 * <p>The goal cooperates with {@link PetMeleeAttackGoal}: both are selected
 * as {@link GoalType#MOVE}, but each defers to the other whenever its
 * partner's damage stat is higher and the target is on the "wrong" side of
 * {@link #MELEE_PREFERENCE_RANGE_SQ}. This prevents a ranged-capable pet
 * from standing still shooting an enemy at point-blank and keeps a
 * melee-capable pet from chasing an enemy it could shoot from here. The
 * threshold must match on both sides to avoid a dead zone between melee and
 * ranged.
 *
 * <p>Activation also requires the pet's movement to be allowed and the
 * {@link Behavior} skill to permit the hit (Friendly never attacks; Raid
 * spares tamed mobs, players, and other pets).
 *
 * <p>Ranged supports a graceful degradation path: if the {@code Ranged}
 * skill instance is momentarily {@code null} (e.g. during a skilltree
 * hot-reload that clears the skills container but leaves
 * {@code Pet.getRangedDamage()} returning a cached value), the goal falls
 * back to a 1-second cooldown and the default {@link Ranged.Projectile#Arrow} type
 * rather than crashing.
 */
public class PetRangedAttackGoal implements Goal<Mob> {

    /** Float PDC key carrying the damage a non-arrow projectile should inflict on hit. */
    public static final NamespacedKey PROJECTILE_DAMAGE_KEY = new NamespacedKey("mypet", "projectile_damage");
    /** String PDC key carrying the UUID of the owning player, used for attribution and friendly-fire checks. */
    public static final NamespacedKey PROJECTILE_OWNER_KEY = new NamespacedKey("mypet", "projectile_owner");

    // Squared distance at which ranged defers to melee if melee damage is higher.
    // 25 = 5 blocks; comfortably within melee reach so pet closes the gap instead of
    // standing still shooting a mob that's right in front of it. Must match the
    // threshold in PetMeleeAttackGoal to avoid a dead zone between the two goals.
    static final double MELEE_PREFERENCE_RANGE_SQ = 25.0;

    // Lifetime of a Pet-fired projectile that vanilla only removes on impact.
    // The goal engages at 12 blocks and fireballs cover that in well under a
    // second, so 5s is pure slack before a missed shot is reclaimed.
    private static final long PROJECTILE_LIFETIME_TICKS = 100L;

    private final Pet pet;
    private final Mob mob;
    private final float walkSpeedModifier;
    private final double rangeSq;
    private LivingEntity target;
    private int shootTimer = -1;
    private int lastSeenTimer = 0;
    private boolean cachedCanSee = false;
    private int canSeeRefreshIn = 0;

    /**
     * @param petEntity         the pet that will fire projectiles
     * @param walkSpeedModifier multiplicative navigation speed boost applied while closing to the fire window
     * @param range             maximum engagement distance in blocks (stored internally as its square for
     *                          per-tick distance comparisons)
     */
    public PetRangedAttackGoal(Pet pet, Mob mob, float walkSpeedModifier, float range) {
        this.pet = pet;
        this.mob = mob;
        this.walkSpeedModifier = walkSpeedModifier;
        this.rangeSq = range * range;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (pet.getRangedDamage() <= 0) {
            return false;
        }
        if (!pet.canMove() || !pet.hasTarget()) {
            return false;
        }
        LivingEntity target = pet.getPetTarget();
        if (target == null || target.isDead() || target instanceof ArmorStand) {
            return false;
        }
        // A target that changed world since it was set is unreachable, and measuring
        // distance to it would throw.
        if (PetGoalWorlds.isCrossWorld(mob, target)) {
            return false;
        }
        // Defer to melee when target is within melee reach AND melee damage is higher.
        // rangedSkill may legitimately be null during skilltree hot-reload even when
        // pet.getRangedDamage() still returns a cached non-zero value, so the
        // comparison is gated on skill presence rather than crashing.
        double meleeDamage = pet.getDamage();
        if (meleeDamage > 0 && mob.getLocation().distanceSquared(target.getLocation()) < MELEE_PREFERENCE_RANGE_SQ) {
            Ranged rangedSkill = pet.getSkills().get(Ranged.class);
            if (rangedSkill != null && meleeDamage > rangedSkill.getDamage().getValue().doubleValue()) {
                return false;
            }
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
                if (target instanceof Tameable t && t.isTamed()) return false;
                if (PetEntityMarker.isMarked(target)) return false;
                if (target instanceof Player) return false;
            }
        }
        this.target = target;
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!pet.hasTarget() || pet.getRangedDamage() <= 0 || !pet.canMove()) {
            return false;
        }
        LivingEntity current = pet.getPetTarget();
        if (current == null || !current.equals(target)) {
            return false;
        }
        // See the matching check in shouldActivate().
        if (PetGoalWorlds.isCrossWorld(mob, target)) {
            return false;
        }
        // Defer to melee when target is within melee reach AND melee damage is higher.
        // See the matching comment in shouldActivate() for the rationale behind the
        // null gate on rangedSkill.
        double meleeDamage = pet.getDamage();
        if (meleeDamage > 0 && mob.getLocation().distanceSquared(target.getLocation()) < MELEE_PREFERENCE_RANGE_SQ) {
            Ranged rangedSkill = pet.getSkills().get(Ranged.class);
            if (rangedSkill != null && meleeDamage > rangedSkill.getDamage().getValue().doubleValue()) {
                return false;
            }
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) return false;
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
                if (target instanceof Tameable t && t.isTamed()) return false;
                if (PetEntityMarker.isMarked(target)) return false;
                if (target instanceof Player) return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        // Force a fresh line-of-sight raycast on the first tick for the new target.
        this.canSeeRefreshIn = 0;
        this.cachedCanSee = false;
    }

    @Override
    public void stop() {
        this.target = null;
        this.lastSeenTimer = 0;
        pet.getPetNavigation().getParameters().removeSpeedModifier("RangedAttack");
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        if (PetGoalWorlds.isCrossWorld(mob, target)) {
            return;
        }
        double distSq = mob.getLocation().distanceSquared(target.getLocation());
        // The line-of-sight raycast is amortized over 3 ticks (like vanilla's Sensing
        // cache); lastSeenTimer accumulates off the cached value, so its 20-tick unseen
        // threshold may drift by at most 2 ticks — acceptable.
        if (--canSeeRefreshIn <= 0) {
            canSeeRefreshIn = 3;
            // hasLineOfSight touches both entities' NMS handles; on Folia this fails when the
            // target is in a different region from the pet. Treat cross-region targets as
            // "out of sight" — the goal will chase until they're in the same region again.
            cachedCanSee = Bukkit.isOwnedByCurrentRegion(target) && mob.hasLineOfSight(target);
        }
        boolean canSee = cachedCanSee;

        if (canSee) {
            lastSeenTimer++;
        } else {
            lastSeenTimer = 0;
        }

        if (distSq <= rangeSq && lastSeenTimer >= 20) {
            pet.getPetNavigation().getParameters().removeSpeedModifier("RangedAttack");
            pet.getPetNavigation().stop();
        } else {
            pet.getPetNavigation().getParameters().addSpeedModifier("RangedAttack", walkSpeedModifier);
            pet.getPetNavigation().navigateTo(target.getLocation());
        }

        mob.lookAt(target, 30.0F, 30.0F);

        if (--shootTimer <= 0) {
            if (distSq < rangeSq && canSee) {
                shootProjectile(target, (float) pet.getRangedDamage(), getProjectile());
                Ranged rangedSkill = pet.getSkills().get(Ranged.class);
                // Fall back to a 1-second cooldown if the skill instance is
                // momentarily unavailable (e.g., during skilltree hot-reload).
                shootTimer = rangedSkill != null ? rangedSkill.getRateOfFire().getValue() : 20;
            }
        }
    }

    private Ranged.Projectile getProjectile() {
        Ranged rangedSkill = pet.getSkills().get(Ranged.class);
        if (rangedSkill != null && rangedSkill.isActive()) {
            return rangedSkill.getProjectile().getValue();
        }
        return Ranged.Projectile.Arrow;
    }

    private void shootProjectile(LivingEntity target, float damage, Ranged.Projectile projectile) {
        PetModelService.playAnimation(pet, PetModelAnimation.ATTACK);
        Location petLoc = mob.getLocation();
        Location targetLoc = target.getLocation();
        World world = petLoc.getWorld();
        Player owner = pet.getOwner().getPlayer();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Compute velocity from pet's eye level to target's body center
        // launchProjectile() spawns from the mob's eye position, so we calculate
        // direction from eye-to-eye (not feet-to-feet) to avoid aiming too high
        Location petEye = petLoc.clone().add(0, mob.getEyeHeight(), 0);
        Location targetBody = targetLoc.clone().add(0, target.getHeight() * 0.5, 0);
        Vector direction = targetBody.subtract(petEye).toVector();
        double horizDist = Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ());
        direction.setY(direction.getY() + horizDist * 0.2);

        switch (projectile) {
            case Arrow -> {
                Arrow arrow = mob.launchProjectile(Arrow.class, direction.normalize().multiply(1.6), a -> {
                    a.setDamage(damage);
                    a.setCritical(false);
                    a.setShooter(owner != null ? owner : mob);
                    a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    // Mark as Pet-fired so listeners can apply friendly-fire / duel
                    // logic (see PetRangedAttackGoal.getSourcePet). Arrows don't use
                    // the PROJECTILE_DAMAGE_KEY path — setDamage() is native — but the
                    // owner tag is still needed to distinguish pet arrows from
                    // player-fired arrows.
                    if (owner != null) {
                        a.getPersistentDataContainer().set(PROJECTILE_OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
                    }
                });
                world.playSound(petLoc, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F / (rng.nextFloat() * 0.4F + 0.8F));
            }
            case Trident -> {
                Trident trident = mob.launchProjectile(Trident.class, direction.normalize().multiply(1.6), t -> {
                    t.setDamage(damage);
                    t.setShooter(owner != null ? owner : mob);
                    t.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    // See Arrow branch comment.
                    if (owner != null) {
                        t.getPersistentDataContainer().set(PROJECTILE_OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
                    }
                });
                world.playSound(petLoc, Sound.ITEM_TRIDENT_THROW, 1.0F, 1.0F / (rng.nextFloat() * 0.4F + 0.8F));
            }
            case Snowball -> {
                launchThrowable(mob, Snowball.class, direction, damage, owner,
                        Sound.ENTITY_ARROW_SHOOT, 0.5F, 0.4F / (rng.nextFloat() * 0.4F + 0.8F));
            }
            case Egg -> {
                launchThrowable(mob, Egg.class, direction, damage, owner,
                        Sound.ENTITY_ARROW_SHOOT, 0.5F, 0.4F / (rng.nextFloat() * 0.4F + 0.8F));
            }
            case EnderPearl -> {
                launchThrowable(mob, EnderPearl.class, direction, damage, owner,
                        Sound.ENTITY_ENDER_PEARL_THROW, 1.0F, 1.0F / (rng.nextFloat() * 0.4F + 0.8F));
            }
            case LlamaSpit -> {
                // LlamaSpit uses different aim (lower target point) and lower speed
                Location spitTarget = targetLoc.clone().add(0, target.getHeight() * 0.33, 0);
                Vector spitDir = spitTarget.subtract(petEye).toVector();
                double spitHoriz = Math.sqrt(spitDir.getX() * spitDir.getX() + spitDir.getZ() * spitDir.getZ());
                spitDir.setY(spitDir.getY() + spitHoriz * 0.2);

                launchThrowable(mob, LlamaSpit.class, spitDir, damage, owner,
                        Sound.ENTITY_LLAMA_SPIT, 1.0F, 1.0F / (rng.nextFloat() * 0.4F + 0.8F));
            }
            case LargeFireball -> {
                launchFireball(mob, LargeFireball.class, target, damage, owner,
                        Sound.ENTITY_GHAST_SHOOT, 1.0F + rng.nextFloat(), rng.nextFloat() * 0.7F + 0.3F);
            }
            case SmallFireball -> {
                launchFireball(mob, SmallFireball.class, target, damage, owner,
                        Sound.ENTITY_GHAST_SHOOT, 1.0F + rng.nextFloat(), rng.nextFloat() * 0.7F + 0.3F);
            }
            case WitherSkull -> {
                launchFireball(mob, WitherSkull.class, target, damage, owner,
                        Sound.ENTITY_WITHER_SHOOT, 1.0F + rng.nextFloat(), rng.nextFloat() * 0.7F + 0.3F);
            }
            case DragonFireball -> {
                launchFireball(mob, DragonFireball.class, target, damage, owner,
                        Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0F + rng.nextFloat(), rng.nextFloat() * 0.7F + 0.3F);
            }
        }
    }

    private <T extends Projectile> void launchThrowable(Mob mob, Class<T> type,
                                                                          Vector direction, float damage, Player owner, Sound sound, float volume, float pitch) {
        T projectile = mob.launchProjectile(type, direction.normalize().multiply(1.6), p -> {
            p.setShooter(owner != null ? owner : mob);
            PersistentDataContainer pdc = p.getPersistentDataContainer();
            pdc.set(PROJECTILE_DAMAGE_KEY, PersistentDataType.FLOAT, damage);
            if (owner != null) {
                pdc.set(PROJECTILE_OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
            }
        });
        scheduleDespawn(projectile);
        mob.getLocation().getWorld().playSound(mob.getLocation(), sound, volume, pitch);
    }

    private <T extends Fireball> void launchFireball(Mob mob, Class<T> type,
                                                     LivingEntity target, float damage, Player owner, Sound sound, float volume, float pitch) {
        Location petLoc = mob.getLocation();
        Vector dir = target.getLocation().add(0, target.getHeight() / 2.0, 0)
                .subtract(petLoc.clone().add(0, mob.getHeight() / 2.0 + 0.5, 0)).toVector();

        T fireball = mob.launchProjectile(type, dir.normalize(), fb -> {
            fb.setShooter(owner != null ? owner : mob);
            fb.setYield(0); // no explosion
            fb.setIsIncendiary(false);
            PersistentDataContainer pdc = fb.getPersistentDataContainer();
            pdc.set(PROJECTILE_DAMAGE_KEY, PersistentDataType.FLOAT, damage);
            if (owner != null) {
                pdc.set(PROJECTILE_OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
            }
        });
        scheduleDespawn(fireball);
        mob.getLocation().getWorld().playSound(petLoc, sound, volume, pitch);
    }

    /**
     * Reclaims a Pet-fired projectile that never hit anything.
     *
     * <p>Vanilla only discards fireballs and throwables from inside their own
     * {@code tick()} — on impact, or when their chunk is gone. A shot that
     * misses keeps flying until it leaves the entity-ticking area, where it
     * stops ticking and is never removed again, so every miss leaks one entity
     * for as long as that chunk stays loaded. The entity scheduler is ticked
     * for every loaded entity regardless of chunk ticking state, so this still
     * fires for a projectile frozen outside simulation distance.
     */
    private void scheduleDespawn(Projectile projectile) {
        projectile.getScheduler().runDelayed(MyPetApi.getPlugin(),
                task -> projectile.remove(), null, PROJECTILE_LIFETIME_TICKS);
    }

    /**
     * Resolves the Pet that fired a given projectile, or {@code null} if the
     * projectile was not fired by a Pet (or its owner is offline / has no
     * active pet when the projectile lands).
     *
     * <p>Identification relies on the {@link #PROJECTILE_OWNER_KEY} PDC tag set
     * at launch time in {@link #shootProjectile} and its helper methods. Every
     * Pet-fired projectile type — Arrow, Trident, throwables, fireballs —
     * carries this tag. Paper-native projectiles (e.g., an arrow fired by a
     * player with a regular bow) do NOT carry the tag, so this method cleanly
     * distinguishes the two.
     *
     * <p>Used by {@code PetPvPListener} and {@code PlayerListener} to
     * apply friendly-fire prevention, owner protection, and duel-mode
     * exemptions for projectile-dealt damage.
     *
     * @param projectile the projectile that dealt damage
     * @return the source pet, or {@code null} if not Pet-fired / unresolvable
     */
    public static Pet getSourcePet(Projectile projectile) {
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        String ownerUuidStr = pdc.get(PROJECTILE_OWNER_KEY, PersistentDataType.STRING);
        if (ownerUuidStr == null) {
            return null;
        }
        UUID ownerUuid;
        try {
            ownerUuid = UUID.fromString(ownerUuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null) {
            return null;
        }
        if (!MyPetApi.getPlayerManager().isMyPetPlayer(owner)) {
            return null;
        }
        MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(owner);
        // Primary pet only: this resolves a player back to "their" pet for projectile
        // attribution, which has no unambiguous answer once several are out.
        // Phase 2 -- MyPetORG/MyPet#1435.
        return (myPetPlayer != null && myPetPlayer.hasPet()) ? myPetPlayer.getPet() : null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.RANGED_ATTACK;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
