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

import de.Keyle.MyPet.api.config.PetConfigKeys;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.PetAttributes;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} that walks the pet up to a {@link Pet#getPetTarget() selected target}
 * and performs melee attacks on a fixed cooldown.
 *
 * <p>Activation is gated by several conditions, all of which must hold:
 * <ul>
 *   <li>The pet has non-zero melee damage ({@code Pet#getDamage()}).</li>
 *   <li>A target exists, is alive, and is not an {@link ArmorStand}.</li>
 *   <li>The pet's {@link Behavior} skill, if active, does not forbid the hit
 *       (Friendly never attacks; Raid spares tamed mobs, players, and other
 *       pets).</li>
 *   <li>When both melee and ranged damage are available, melee only runs at
 *       close range; beyond {@link PetRangedAttackGoal#MELEE_PREFERENCE_RANGE_SQ}
 *       melee defers to {@link PetRangedAttackGoal} whenever the ranged damage
 *       is the higher of the two. This threshold must match the one in
 *       {@code PetRangedAttackGoal} to avoid a dead zone between the two
 *       goals.</li>
 * </ul>
 *
 * <p>While active, the goal periodically (every 4–10 ticks) asks the
 * {@link de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation pet
 * navigation} to close on the target, applies a speed modifier keyed
 * {@code "MeleeAttack"}, and — once the target is within an
 * entity-height-adjusted reach, the attacker's line of sight is clear, and the
 * attacker's eyes are not inside a solid block — fires
 * {@link #applyPetDamage(Pet, LivingEntity, double)} on the cooldown
 * configured at construction time.
 *
 * <p>The line-of-sight and eye-in-block checks are deliberate: the raycast
 * underlying {@link Mob#hasLineOfSight} skips the starting block, which
 * would otherwise let a pet pressed against a wall hit targets through it.
 * The cooldown counter is also only decremented while the hit is actually
 * eligible, preventing a "stored" attack from firing the instant line of
 * sight clears.
 *
 * <p>Declares {@link GoalType#MOVE} so the goal selector treats it as
 * mutually-exclusive with other MOVE-bucket goals (stroll, follow owner).
 */
public class PetMeleeAttackGoal implements Goal<Mob> {

    private final int ticksUntilNextHit;
    private final Pet pet;
    private final Mob mob;
    private final double range;
    private final float walkSpeedModifier;
    private LivingEntity targetEntity;
    private int ticksUntilNextHitLeft = 0;
    private int timeUntilNextNavigationUpdate;

    /**
     * @param petEntity         the pet that will perform the attack
     * @param walkSpeedModifier multiplicative speed boost applied to the navigation while closing on the target
     * @param range             linear melee reach in blocks (<em>not</em> squared — the tick loop squares it
     *                          before comparing with {@code distanceSquared}); typically the pet's bounding-box
     *                          width plus a small constant
     * @param ticksUntilNextHit cooldown in ticks between successful hits
     */
    public PetMeleeAttackGoal(Pet pet, Mob mob, float walkSpeedModifier, double range, int ticksUntilNextHit) {
        this.pet = pet;
        this.mob = mob;
        this.walkSpeedModifier = walkSpeedModifier;
        this.range = range;
        this.ticksUntilNextHit = ticksUntilNextHit;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (pet.getDamage() <= 0) {
            return false;
        }
        if (!pet.hasTarget()) {
            return false;
        }
        LivingEntity target = pet.getPetTarget();
        if (target == null || target.isDead()) {
            return false;
        }
        if (target instanceof ArmorStand) {
            return false;
        }
        // Defer to ranged when target is far AND ranged damage is higher (mirrors
        // the logic in PetRangedAttackGoal — must use the same distance threshold)
        double rangedDamage = pet.getRangedDamage();
        if (rangedDamage > 0 && mob.getLocation().distanceSquared(target.getLocation()) >= PetRangedAttackGoal.MELEE_PREFERENCE_RANGE_SQ) {
            if (rangedDamage >= pet.getDamage()) {
                return false;
            }
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
                if (target instanceof Tameable tameable && tameable.isTamed()) {
                    return false;
                }
                if (PetEntityMarker.isMarked(target)) {
                    return false;
                }
                if (target instanceof Player) {
                    return false;
                }
            }
        }
        this.targetEntity = target;
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!pet.hasTarget() || !pet.canMove()) {
            return false;
        }
        LivingEntity currentTarget = pet.getPetTarget();
        if (currentTarget == null || !currentTarget.equals(targetEntity)) {
            return false;
        }
        // Defer to ranged when target is far AND ranged damage is higher
        double stayRangedDamage = pet.getRangedDamage();
        if (stayRangedDamage > 0 && mob.getLocation().distanceSquared(targetEntity.getLocation()) >= PetRangedAttackGoal.MELEE_PREFERENCE_RANGE_SQ) {
            if (stayRangedDamage >= pet.getDamage()) {
                return false;
            }
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == Behavior.BehaviorMode.Raid) {
                if (targetEntity instanceof Tameable tameable && tameable.isTamed()) {
                    return false;
                }
                if (PetEntityMarker.isMarked(targetEntity)) {
                    return false;
                }
                if (targetEntity instanceof Player) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void start() {
        pet.getPetNavigation().getParameters().addSpeedModifier("MeleeAttack", walkSpeedModifier);
        pet.getPetNavigation().navigateTo(this.targetEntity);
        this.timeUntilNextNavigationUpdate = 0;
    }

    @Override
    public void stop() {
        pet.getPetNavigation().getParameters().removeSpeedModifier("MeleeAttack");
        this.targetEntity = null;
        pet.getPetNavigation().stop();
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        mob.lookAt(targetEntity, 30.0F, 30.0F);
        if (--this.timeUntilNextNavigationUpdate <= 0) {
            this.timeUntilNextNavigationUpdate = 4 + ThreadLocalRandom.current().nextInt(7);
            pet.getPetNavigation().navigateTo(targetEntity);
        }
        double distSq = mob.getLocation().distanceSquared(targetEntity.getLocation());
        // Range is passed as a linear distance (pet bbWidth + 1.3). Square it
        // before comparing against distSq so melee reach resolves to ~3 blocks
        // for a player-sized target rather than ~1.6 blocks.
        double adjustedRange = this.range + (targetEntity.getHeight() * (2.0 / 3.0));
        if (distSq <= adjustedRange * adjustedRange) {
            // Check eyes aren't inside a solid block (prevents attacks through
            // walls when the pet is pressed against one — the raycast backing
            // hasLineOfSight skips the starting block, so it would otherwise
            // return true through the wall).
            Location eyeLoc = mob.getLocation().add(0, mob.getEyeHeight(), 0);
            // Skip line-of-sight check when the target is in a different Folia region
            // (hasLineOfSight touches the target's NMS handle and would trip the thread check).
            if (eyeLoc.getBlock().isPassable() && Bukkit.isOwnedByCurrentRegion(targetEntity) && mob.hasLineOfSight(targetEntity)) {
                // Only decrement the cooldown while the attack is actually
                // eligible (in range + line of sight). Previously the counter
                // ticked down unconditionally inside the outer `distSq`
                // check, so a pet blocked by a wall would drift the counter
                // to -N over many ticks and then fire an attack instantly as
                // soon as LoS cleared.
                if (this.ticksUntilNextHitLeft-- <= 0) {
                    this.ticksUntilNextHitLeft = ticksUntilNextHit;
                    applyPetDamage(pet, targetEntity, pet.getDamage());
                }
            }
        }
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.MELEE_ATTACK;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    /**
     * Applies damage from a Pet to a target entity using Paper's
     * {@link DamageSource} builder. When the owner is online and in the same
     * world, {@code withCausingEntity(owner)} routes kill credit through
     * vanilla's {@code lastHurtByPlayer} tracking so mob drops and XP land
     * on the owner. {@code withDirectEntity(mob)} keeps the pet as the
     * visible attacker for death messages and knockback.
     *
     * <p>Owner attribution is deliberately skipped when the target is a
     * {@link Player} — the drops/XP routing rationale only applies to mob
     * kills (players drop their own XP orbs on death regardless of
     * {@code lastHurtByPlayer}), and attributing PvP kills to the owner
     * makes the pet's Aggressive mode read as the owner killing the victim
     * in death messages and in {@code Player#getKiller()} for any combat
     * / kill-tracking plugins.
     *
     * <p>The damage type is {@link DamageType#MOB_ATTACK_NO_AGGRO} rather
     * than {@code MOB_ATTACK}: it sits in vanilla's {@code no_anger}
     * damage-type tag, so {@code LivingEntity#hurtServer} skips
     * {@code setLastHurtByMob(...)} and the victim's brain {@code
     * HURT_BY_ENTITY} memory stays empty. Without that skip the
     * {@code withCausingEntity(owner)} above would seed the memory with
     * the player owner and vanilla retaliation tasks would re-target the
     * mob onto the player (Cluster G). The Iron Golem accidental-villager
     * path uses the same damage type in vanilla for the same reason.
     * {@code lastHurtByPlayer} is gated on the causing entity being a
     * {@link Player}, not on the {@code no_anger} tag, so kill-credit
     * routing through {@code withCausingEntity(owner)} is unaffected.
     */
    private static void applyPetDamage(Pet pet, LivingEntity target, double damage) {
        if (pet == null || target == null || target.isDead()) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;

        Player owner = pet.getOwner() != null ? pet.getOwner().getPlayer() : null;

        DamageSource.Builder builder = DamageSource.builder(DamageType.MOB_ATTACK_NO_AGGRO)
                .withDirectEntity(mob);
        if (owner != null && owner.isOnline() && owner.getWorld().equals(mob.getWorld())
                && !(target instanceof Player)) {
            builder = builder.withCausingEntity(owner);
        }

        double healthBefore = target.getHealth();
        try {
            target.damage(damage, builder.build());
        } catch (IllegalStateException ignored) {
            // Paper may reject a damage source if the target is invulnerable
            // or the damage type was registered after world load. Fall back
            // to a plain damage call.
            target.damage(damage, mob);
        }
        mob.swingMainHand();

        // Toss-up for IronGolem pets. Vanilla applies this inside
        // IronGolem#doHurtTarget, but we bypass that by calling
        // target.damage(...) directly — the defender's hurt path doesn't carry
        // attacker-specific knockback. Health-drop check approximates vanilla's
        // "only on a successful hit" guard (skips canceled events and i-frames).
        if (mob instanceof IronGolem && PetConfigKeys.IronGolem.CAN_TOSS_UP.get()
                && target.getHealth() < healthBefore) {
            applyIronGolemTossUp(target);
        }
    }

    private static void applyIronGolemTossUp(LivingEntity target) {
        double knockbackResistance = 0.0;
        AttributeInstance attribute = target.getAttribute(PetAttributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            knockbackResistance = attribute.getValue();
        }
        double scale = Math.max(0.0, 1.0 - knockbackResistance);
        if (scale <= 0.0) return;
        Vector velocity = target.getVelocity();
        target.setVelocity(velocity.setY(velocity.getY() + 0.4 * scale));
    }
}
