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

package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.PetExpEvent;
import de.Keyle.MyPet.api.event.PetLevelDownEvent;
import de.Keyle.MyPet.api.event.PetLevelUpEvent;
import de.Keyle.MyPet.api.skill.experience.ExperienceCache;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculator;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.api.skill.experience.modifier.ExperienceModifier;
import de.Keyle.MyPet.api.skill.experience.modifier.GlobalModifier;
import de.Keyle.MyPet.api.skill.experience.modifier.PermissionModifier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Manages a pet's experience points, level progression, and the damage-tracking maps
 * used to attribute kills for XP distribution. Each {@link MyPet} owns one instance of
 * this class.
 *
 * <p>Experience gain flows through {@link #addExp} overloads, which optionally apply
 * registered {@link ExperienceModifier}s (global, permission-based, or custom), fire a
 * {@link PetExpEvent}, and then recalculate the pet's level. Level changes trigger
 * {@link PetLevelUpEvent} or {@link PetLevelDownEvent} as appropriate.
 *
 * <p>The static damage-map methods ({@link #addDamageToEntity}, {@link #getDamageToEntityPercent},
 * etc.) track how much damage each entity has dealt to a victim. This is used by the
 * experience distribution system to award partial XP proportional to damage contribution
 * when multiple pets/players assist a kill.
 *
 * @see ExperienceModifier
 * @see ExperienceCalculator
 * @see ExperienceCache
 */
public class MyPetExperience {

    /** Singleton global XP modifier applied to all pets (e.g. event multiplier). */
    public static final GlobalModifier GLOBAL_MODIFIER = new GlobalModifier();

    /**
     * Weak-keyed map tracking cumulative damage dealt to each living entity, keyed by
     * damager UUID. Entries are automatically cleared when the victim is garbage-collected.
     */
    private static final Map<LivingEntity, Map<UUID, Double>> DAMAGE_MAPS = new WeakHashMap<>();

    @Getter
    protected final MyPet myPet;
    @Getter
    protected int level = 1;
    @Getter
    protected double exp = 0;
    @Getter
    protected double maxExp = Double.MAX_VALUE;
    protected final ExperienceCache cache;
    protected final ExperienceCalculator expCalculator;
    protected final Map<String, ExperienceModifier> modifier = new HashMap<>();

    /**
     * Creates the experience tracker for the given pet, initializing the calculator
     * and registering the default modifiers (global and permission-based).
     *
     * @param pet the pet that owns this experience instance
     */
    public MyPetExperience(MyPet pet) {
        this.myPet = pet;
        this.expCalculator = MyPetApi.getServiceManager()
                .getService(ExperienceCalculatorManager.class).orElseThrow()
                .getCalculator();
        cache = MyPetApi.getServiceManager().getService(ExperienceCache.class).orElseThrow();

        this.modifier.put("Global", GLOBAL_MODIFIER);
        this.modifier.put("Permission", new PermissionModifier(myPet));
    }

    /** Returns the raw damage map for a victim, or {@code null} if none exists. */
    private static Map<UUID, Double> getDamageMap(LivingEntity victim) {
        return DAMAGE_MAPS.get(victim);
    }

    /**
     * Records damage dealt by {@code damager} to {@code victim}. The damage is
     * clamped to the victim's current health (to avoid over-counting overkill) and
     * accumulated across multiple hits.
     *
     * @param damager the entity dealing damage
     * @param victim  the entity receiving damage
     * @param damage  the raw damage amount
     */
    public static void addDamageToEntity(LivingEntity damager, LivingEntity victim, double damage) {
        Map<UUID, Double> damageMap = DAMAGE_MAPS.computeIfAbsent(victim, k -> new HashMap<>());
        damageMap.merge(damager.getUniqueId(), Math.min(victim.getHealth(), damage),
                (oldDamage, newDamage) -> (Math.min(victim.getHealth(), damage)) + oldDamage);
    }

    /** Removes all accumulated damage records for the given victim (e.g. on death). */
    public static void clearDamageMap(LivingEntity victim) {
        DAMAGE_MAPS.remove(victim);
    }

    /**
     * Returns the total damage that {@code damager} has dealt to {@code victim},
     * or {@code 0} if no damage has been recorded.
     */
    public static double getDamageToEntity(LivingEntity damager, LivingEntity victim) {
        Map<UUID, Double> damageMap = getDamageMap(victim);
        if (damageMap == null) {
            return 0;
        }
        return damageMap.getOrDefault(damager.getUniqueId(), 0.0);
    }

    /**
     * Returns the fraction (0.0 to 1.0) of total tracked damage to {@code victim}
     * that was dealt by {@code damager}.
     */
    public static double getDamageToEntityPercent(LivingEntity damager, LivingEntity victim) {
        Map<UUID, Double> damageMap = getDamageMap(victim);
        if (damageMap == null) {
            return 0;
        }
        double damagerDamage = damageMap.getOrDefault(damager.getUniqueId(), 0.0);
        double allDamage = 0;
        for (double d : damageMap.values()) {
            allDamage += d;
        }
        return damagerDamage / allDamage;
    }

    /**
     * Returns a map from damager UUID to their damage fraction (0.0 to 1.0) of
     * total tracked damage to the given victim. Useful for splitting XP rewards
     * proportionally among multiple attackers.
     */
    public static Map<UUID, Double> getDamageToEntityPercent(LivingEntity victim) {
        Map<UUID, Double> damagePercentMap = new HashMap<>();
        Map<UUID, Double> damageMap = getDamageMap(victim);
        if (damageMap == null) {
            return damagePercentMap;
        }
        double allDamage = 0;
        for (double d : damageMap.values()) {
            allDamage += d;
        }
        if (allDamage <= 0) {
            return damagePercentMap;
        }
        for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
            damagePercentMap.put(entry.getKey(), entry.getValue() / allDamage);
        }
        return damagePercentMap;
    }

    /**
     * Applies all registered experience modifiers to a raw XP value. Each modifier
     * receives both the running total and the original raw value, allowing both
     * additive and multiplicative adjustments.
     *
     * @param exp the raw experience before modification
     * @return the modified experience value
     */
    protected double modifyExp(double exp) {
        double returnVal = exp;
        for (ExperienceModifier modifier : modifier.values()) {
            returnVal = modifier.modify(returnVal, exp);
        }
        return returnVal;
    }

    /**
     * Registers (or replaces) a named experience modifier.
     *
     * @param id       unique identifier for this modifier
     * @param modifier the modifier instance
     */
    public void addModifier(String id, ExperienceModifier modifier) {
        this.modifier.put(id, modifier);
    }

    /**
     * Removes a named experience modifier.
     *
     * @param id the identifier of the modifier to remove
     * @return the removed modifier, or {@code null} if none was registered under that id
     */
    public ExperienceModifier removeModifier(String id) {
        return this.modifier.remove(id);
    }

    /**
     * Sets the maximum achievable level for this pet. The corresponding max XP is
     * calculated and stored; if the pet already exceeds it, XP is clamped down.
     *
     * @param level the maximum level (inclusive)
     */
    public void setMaxLevel(int level) {
        this.maxExp = getExpByLevel(level);
        if (this.exp > this.maxExp) {
            setExp(this.maxExp);
        }
    }

    /**
     * Sets the pet's total experience to an absolute value, firing level-change
     * events if the resulting level differs from the current one.
     *
     * @param exp the desired total XP
     */
    public void setExp(double exp) {
        exp = exp - this.exp;
        updateExp(exp, true);
    }

    /**
     * Adds raw experience without applying modifiers.
     *
     * @param exp the XP to add
     * @return the actual amount of XP gained (after event cancellation / clamping)
     */
    public double addExp(double exp) {
        return this.addExp(exp, false);
    }

    /**
     * Adds experience, optionally passing it through all registered modifiers first.
     *
     * @param exp    the base XP to add
     * @param modify if {@code true}, modifiers are applied before adding
     * @return the actual amount of XP gained
     */
    public double addExp(double exp, boolean modify) {
        if (modify) {
            exp = modifyExp(exp);
        }
        return updateExp(exp, false);
    }

    /**
     * Awards experience based on the given entity's mob-type XP value (looked up via
     * {@link MonsterExperience}). Does not apply modifiers.
     *
     * @param entity the killed entity to derive XP from
     * @return the actual amount of XP gained
     */
    public double addExp(Entity entity) {
        return this.addExp(entity, false);
    }

    /**
     * Awards experience based on the given entity's mob-type XP value, optionally
     * applying modifiers.
     *
     * @param entity the killed entity to derive XP from
     * @param modify if {@code true}, modifiers are applied
     * @return the actual amount of XP gained
     */
    public double addExp(Entity entity, boolean modify) {
        MonsterExperience monsterExperience = MonsterExperience.getMonsterExperience(entity);
        if (monsterExperience != MonsterExperience.UNKNOWN) {
            double exp = monsterExperience.getRandomExp();
            if (modify) {
                exp = modifyExp(exp);
            }
            return updateExp(exp, false);
        }
        return 0;
    }

    /**
     * Awards a percentage of the entity's mob-type XP value without modifiers.
     *
     * @param entity  the killed entity
     * @param percent the percentage of the mob's XP to award (0-100)
     * @return the actual amount of XP gained
     */
    public double addExp(Entity entity, int percent) {
        return addExp(entity, percent, false);
    }

    /**
     * Awards a percentage of the entity's mob-type XP value, optionally with modifiers.
     *
     * @param entity  the killed entity
     * @param percent the percentage of the mob's XP to award (0-100)
     * @param modify  if {@code true}, modifiers are applied before percentage scaling
     * @return the actual amount of XP gained
     */
    public double addExp(Entity entity, int percent, boolean modify) {
        MonsterExperience monsterExperience = MonsterExperience.getMonsterExperience(entity);
        if (monsterExperience != MonsterExperience.UNKNOWN) {
            double exp = monsterExperience.getRandomExp();
            if (modify) {
                exp = modifyExp(exp);
            }
            exp = exp * percent / 100.;
            return updateExp(exp, false);
        }
        return 0;
    }

    /**
     * Removes experience from the current level's progress only (will not drop below
     * the current level's XP threshold).
     *
     * @param exp the amount to remove (clamped to current-level progress)
     * @return the actual amount of XP removed (as a negative delta)
     */
    public double removeCurrentExp(double exp) {
        if (exp > getCurrentExp()) {
            exp = getCurrentExp();
        }
        return updateExp(-exp, false);
    }

    /**
     * Removes experience from total XP, potentially causing level-down events. The
     * removal is clamped so total XP never drops below zero.
     *
     * @param exp the amount to remove
     * @return the actual amount of XP removed (as a negative delta)
     */
    public double removeExp(double exp) {
        exp = this.exp - exp < 0 ? this.exp : exp;
        return updateExp(-exp, false);
    }

    /**
     * Core XP mutation method. Fires a {@link PetExpEvent} (which may be cancelled),
     * updates the stored total XP, recalculates the level, and dispatches
     * {@link PetLevelUpEvent} or {@link PetLevelDownEvent} if the level changed.
     *
     * @param exp   the XP delta (positive to gain, negative to lose)
     * @param quiet if {@code true}, level-change events are marked as quiet (suppressing
     *              messages to the player)
     * @return the actual XP change applied
     */
    protected double updateExp(double exp, boolean quiet) {
        PetExpEvent expEvent = new PetExpEvent(myPet, exp, quiet);
        Bukkit.getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled()) {
            return 0;
        }

        int oldLvl = this.level;
        double oldExp = this.exp;
        this.exp += expEvent.getExp();
        this.exp = Math.max(0, Math.min(maxExp, this.exp));
        int lvl = cache.getLevel(myPet.getWorldGroup(), myPet.getPetType(), this.exp);
        if (lvl != 0) {
            this.level = lvl;
        } else {
            this.level = calculateLevel();
        }
        if (oldLvl != this.level) {
            if (oldLvl < this.level) {
                Bukkit.getServer().getPluginManager().callEvent(new PetLevelUpEvent(myPet, this.level, oldLvl, quiet));
            } else {
                Bukkit.getServer().getPluginManager().callEvent(new PetLevelDownEvent(myPet, this.level, oldLvl, quiet));
            }
        }
        return this.exp - oldExp;
    }

    /**
     * Recalculates the level by scanning up or down from the current level until the
     * XP thresholds bracket the pet's total XP. Used as a fallback when the
     * {@link ExperienceCache} does not have a pre-computed level for the current XP.
     *
     * @return the computed level
     */
    protected int calculateLevel() {
        int currentLevel = this.level;

        if (this.exp >= getExpByLevel(currentLevel + 1)) {
            double expForNextLevel = getExpByLevel(currentLevel + 1);
            while (this.exp >= expForNextLevel) {
                expForNextLevel = getExpByLevel(++currentLevel + 1);
            }
        } else {
            double expForCurrentLevel = getExpByLevel(currentLevel);
            if (this.exp < expForCurrentLevel) {
                while (this.exp < expForCurrentLevel) {
                    expForCurrentLevel = getExpByLevel(--currentLevel);
                }
            }
        }
        return currentLevel;
    }

    /**
     * Returns the experience gained within the current level (total XP minus the
     * threshold to reach the current level). This is the "progress bar" value.
     */
    public double getCurrentExp() {
        double currentLevelExp = this.getExpByLevel(level);
        return exp - currentLevelExp;
    }

    /**
     * Returns the total experience required to advance from the current level to the
     * next level (the "progress bar" maximum). Logs a warning and returns
     * {@link Double#MAX_VALUE} if consecutive levels share the same XP threshold
     * (misconfigured calculator).
     */
    public double getRequiredExp() {
        double requiredExp = this.getExpByLevel(level + 1);
        double prevRequiredExp = this.getExpByLevel(level);
        requiredExp = requiredExp - prevRequiredExp;
        if (requiredExp == 0) {
            MyPetApi.getLogger().warning("Level " + level + " and " + (level + 1) + " require the same amount of XP. Please change that.");
            requiredExp = Double.MAX_VALUE;
        }
        return requiredExp;
    }

    /**
     * Returns the cumulative XP required to reach the given level. Results are cached
     * in the {@link ExperienceCache}; on a cache miss, the {@link ExperienceCalculator}
     * is invoked and the result stored for future lookups.
     *
     * @param level the target level (1-based; level 1 always returns 0)
     * @return the total XP threshold for reaching {@code level}
     */
    public double getExpByLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        double prev = 0, exp, next;
        try {
            if (level > 2) {
                prev = cache.getExp(myPet.getWorldGroup(), myPet.getPetType(), level - 1);
            }
            exp = cache.getExp(myPet.getWorldGroup(), myPet.getPetType(), level);
            next = cache.getExp(myPet.getWorldGroup(), myPet.getPetType(), level + 1);
        } catch (ExperienceCache.LevelNotCalculatedException e) {
            if (level > 2) {
                prev = expCalculator.getExpByLevel(this.getMyPet(), level - 1);
                cache.insertExp(myPet.getWorldGroup(), myPet.getPetType(), level - 1, prev);
            }
            exp = expCalculator.getExpByLevel(this.getMyPet(), level);
            next = expCalculator.getExpByLevel(this.getMyPet(), level + 1);
            cache.insertExp(myPet.getWorldGroup(), myPet.getPetType(), level, exp);
            cache.insertExp(myPet.getWorldGroup(), myPet.getPetType(), level + 1, next);
        }
        if (prev == exp) {
            MyPetApi.getLogger().warning("Level " + (level - 1) + " and " + level + " require the same amount of XP. Please change that.");
            exp = Double.MAX_VALUE;
        }
        if (exp == next) {
            MyPetApi.getLogger().warning("Level " + level + " and " + (level + 1) + " require the same amount of XP. Please change that.");
            exp = Double.MAX_VALUE;
        }
        return exp;
    }
}