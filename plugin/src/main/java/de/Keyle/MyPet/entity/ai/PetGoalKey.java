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

package de.Keyle.MyPet.entity.ai;

import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;

/**
 * Central registry of {@link GoalKey} constants for every MyPet-owned Paper
 * {@link com.destroystokyo.paper.entity.ai.Goal} implementation.
 *
 * <p>Paper's goal selector uses {@code GoalKey} as the stable identity of a
 * goal instance attached to a {@link Mob}. Having one canonical constant per
 * goal (instead of inlining {@code GoalKey.of(...)} at each use site) keeps
 * the {@link NamespacedKey} string in one place and enables cross-goal lookup
 * via {@code Bukkit.getMobGoals().getGoal(mob, key)} — used by
 * {@code PetRandomStrollGoal} to avoid strolling while {@code PetControlGoal}
 * is driving and by {@code PetDuelTargetGoal} to wire sibling duelists.
 *
 * <p>All keys are namespaced under {@code "mypet"} so they never collide with
 * Paper's built-in vanilla goal keys (which use the {@code "minecraft"}
 * namespace) or with keys added by other plugins.
 *
 * <p>This class is a constants holder and is not instantiable.
 */
public final class PetGoalKey {

    private PetGoalKey() {
    }

    /** Keeps a pet seated until its owner toggles sitting off. */
    public static final GoalKey<Mob> SIT = GoalKey.of(Mob.class, new NamespacedKey("mypet", "sit"));
    /** Drives movement toward a location chosen via the owner's control item. */
    public static final GoalKey<Mob> CONTROL = GoalKey.of(Mob.class, new NamespacedKey("mypet", "control"));
    /** Temporarily boosts navigation speed when the Sprint skill triggers on a fresh target. */
    public static final GoalKey<Mob> SPRINT = GoalKey.of(Mob.class, new NamespacedKey("mypet", "sprint"));
    /** Periodically rotates the pet's head toward the nearest player. */
    public static final GoalKey<Mob> LOOK_AT_PLAYER = GoalKey.of(Mob.class, new NamespacedKey("mypet", "look_at_player"));
    /** Randomly glances around when idle, for natural head-motion when no player is nearby. */
    public static final GoalKey<Mob> RANDOM_LOOKAROUND = GoalKey.of(Mob.class, new NamespacedKey("mypet", "random_lookaround"));
    /** Picks a hostile target while the owner is directing the pet via the control item. */
    public static final GoalKey<Mob> CONTROL_TARGET = GoalKey.of(Mob.class, new NamespacedKey("mypet", "control_target"));
    /** Picks a nearby target when the Behavior skill is set to {@code Aggressive}. */
    public static final GoalKey<Mob> AGGRESSIVE_TARGET = GoalKey.of(Mob.class, new NamespacedKey("mypet", "aggressive_target"));
    /** Picks a nearby hostile monster when the Behavior skill is set to {@code Farm}. */
    public static final GoalKey<Mob> FARM_TARGET = GoalKey.of(Mob.class, new NamespacedKey("mypet", "farm_target"));
    /** Picks another Pet in {@code Duel} mode to fight one-on-one. */
    public static final GoalKey<Mob> DUEL_TARGET = GoalKey.of(Mob.class, new NamespacedKey("mypet", "duel_target"));
    /** Lifts the pet to the water / lava surface when submerged. */
    public static final GoalKey<Mob> FLOAT = GoalKey.of(Mob.class, new NamespacedKey("mypet", "float"));
    /** Makes sheared sheep pets eat grass blocks to regrow their wool. */
    public static final GoalKey<Mob> EAT_GRASS = GoalKey.of(Mob.class, new NamespacedKey("mypet", "eat_grass"));
    /** Walks up to the current target and performs melee attacks on cooldown. */
    public static final GoalKey<Mob> MELEE_ATTACK = GoalKey.of(Mob.class, new NamespacedKey("mypet", "melee_attack"));
    /** Keeps distance from the current target and fires projectiles on cooldown. */
    public static final GoalKey<Mob> RANGED_ATTACK = GoalKey.of(Mob.class, new NamespacedKey("mypet", "ranged_attack"));
    /** Retaliates against whoever last damaged the owner. */
    public static final GoalKey<Mob> OWNER_HURT_BY_TARGET = GoalKey.of(Mob.class, new NamespacedKey("mypet", "owner_hurt_by_target"));
    /** Retaliates against whoever last damaged the pet itself. */
    public static final GoalKey<Mob> HURT_BY_TARGET = GoalKey.of(Mob.class, new NamespacedKey("mypet", "hurt_by_target"));
    /** Moves the pet toward its owner when it falls too far behind. */
    public static final GoalKey<Mob> FOLLOW_OWNER = GoalKey.of(Mob.class, new NamespacedKey("mypet", "follow_owner"));
    /** Cube-mob (Slime, MagmaCube) follow goal — drives hop cadence and facing because SlimeMoveControl ignores standard navigation. */
    public static final GoalKey<Mob> FOLLOW_OWNER_CUBE_MOB = GoalKey.of(Mob.class, new NamespacedKey("mypet", "follow_owner_cube_mob"));
    /** Wanders within a small radius of the owner when idle (ground pets). */
    public static final GoalKey<Mob> RANDOM_STROLL = GoalKey.of(Mob.class, new NamespacedKey("mypet", "random_stroll"));
    /** Wanders within a small 3-D radius of the owner when idle (flying pets). */
    public static final GoalKey<Mob> RANDOM_FLY = GoalKey.of(Mob.class, new NamespacedKey("mypet", "random_fly"));
    /** Wanders within a small 3-D water radius of the owner when idle (aquatic pets). */
    public static final GoalKey<Mob> RANDOM_SWIM = GoalKey.of(Mob.class, new NamespacedKey("mypet", "random_swim"));
    /** Applies per-tick velocity and rotation for flying pets. */
    public static final GoalKey<Mob> FLYING_MOVEMENT = GoalKey.of(Mob.class, new NamespacedKey("mypet", "flying_movement"));
    /** Applies per-tick Y-axis velocity and body rotation for aquatic pets in water. */
    public static final GoalKey<Mob> AQUATIC_MOVEMENT = GoalKey.of(Mob.class, new NamespacedKey("mypet", "aquatic_movement"));
}
