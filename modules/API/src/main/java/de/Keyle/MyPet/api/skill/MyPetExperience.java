/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.event.MyPetExpEvent;
import de.Keyle.MyPet.api.event.MyPetLevelDownEvent;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class MyPetExperience {

    public static final GlobalModifier GLOBAL_MODIFIER = new GlobalModifier();

    @Getter protected final MyPet myPet;
    @Getter protected int level = 1;
    @Getter protected double exp = 0;
    @Getter protected double maxExp = Double.MAX_VALUE;
    protected ExperienceCache cache;
    protected ExperienceCalculator expCalculator;
    protected Map<String, ExperienceModifier> modifier = new HashMap<>();

    public MyPetExperience(MyPet pet) {
        this.myPet = pet;
        this.expCalculator = MyPetApi.getServiceManager()
                .getService(ExperienceCalculatorManager.class).get()
                .getCalculator();
        cache = MyPetApi.getServiceManager().getService(ExperienceCache.class).get();

        this.modifier.put("Global", GLOBAL_MODIFIER);
        this.modifier.put("Permission", new PermissionModifier(myPet));
    }

    protected double modifyExp(double exp) {
        double returnVal = exp;
        for (ExperienceModifier modifier : modifier.values()) {
            returnVal = modifier.modify(returnVal, exp);
        }
        return returnVal;
    }

    public void addModifier(String id, ExperienceModifier modifier) {
        this.modifier.put(id, modifier);
    }

    public ExperienceModifier removeModifier(String id) {
        return this.modifier.remove(id);
    }

    public double setMaxLevel(int level) {
        this.maxExp = getExpByLevel(level);
        if (this.exp > this.maxExp) {
            return setExp(this.maxExp);
        }
        return 0;
    }

    public double setExp(double exp) {
        exp = exp - this.exp;
        return uppdateExp(exp, true);
    }

    public double addExp(double exp) {
        return this.addExp(exp, false);
    }

    public double addExp(double exp, boolean modify) {
        if (modify) {
            exp = modifyExp(exp);
        }
        return uppdateExp(exp, false);
    }

    public double addExp(Entity entity) {
        return this.addExp(entity, false);
    }

    public double addExp(Entity entity, boolean modify) {
        MonsterExperience monsterExperience = MonsterExperience.getMonsterExperience(entity);
        if (monsterExperience != MonsterExperience.UNKNOWN) {
            double exp = monsterExperience.getRandomExp();
            if (modify) {
                exp = modifyExp(exp);
            }
            return uppdateExp(exp, false);
        }
        return 0;
    }

    public double addExp(Entity entity, int percent) {
        return addExp(entity, percent, false);
    }

    public double addExp(Entity entity, int percent, boolean modify) {
        MonsterExperience monsterExperience = MonsterExperience.getMonsterExperience(entity);
        if (monsterExperience != MonsterExperience.UNKNOWN) {
            double exp = monsterExperience.getRandomExp();
            if (modify) {
                exp = modifyExp(exp);
            }
            exp = exp * percent / 100.;
            return uppdateExp(exp, false);
        }
        return 0;
    }

    public double removeCurrentExp(double exp) {
        if (exp > getCurrentExp()) {
            exp = getCurrentExp();
        }
        return uppdateExp(-exp, false);
    }

    public double removeExp(double exp) {
        exp = this.exp - exp < 0 ? this.exp : exp;
        return uppdateExp(-exp, false);
    }

    protected double uppdateExp(double exp, boolean quiet) {
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, exp);
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
                Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, this.level, oldLvl, quiet));
            } else {
                Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelDownEvent(myPet, this.level, oldLvl, quiet));
            }
        }
        return this.exp - oldExp;
    }

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

    public double getCurrentExp() {
        double currentLevelExp = this.getExpByLevel(level);
        return exp - currentLevelExp;
    }

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

    @SuppressWarnings("unchecked")
    public static void addDamageToEntity(LivingEntity damager, LivingEntity victim, double damage) {
        Map<UUID, Double> damageMap;
        if (victim.hasMetadata("MyPetDamageCount")) {
            for (MetadataValue value : victim.getMetadata("MyPetDamageCount")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    damageMap = (Map<UUID, Double>) value.value();
                    if (damageMap.containsKey(damager)) {
                        double oldDamage = damageMap.get(damager);
                        damageMap.put(damager.getUniqueId(), victim.getHealth() < damage ? victim.getHealth() + oldDamage : damage + oldDamage);
                    } else {
                        damageMap.put(damager.getUniqueId(), victim.getHealth() < damage ? victim.getHealth() : damage);
                    }
                    break;
                }
            }
        } else {
            damageMap = new WeakHashMap<>();
            damageMap.put(damager.getUniqueId(), victim.getHealth() < damage ? victim.getHealth() : damage);
            victim.setMetadata("MyPetDamageCount", new FixedMetadataValue(MyPetApi.getPlugin(), damageMap));
        }
    }

    @SuppressWarnings("unchecked")
    public static double getDamageToEntity(LivingEntity damager, LivingEntity victim) {
        for (MetadataValue value : victim.getMetadata("MyPetDamageCount")) {
            if (value.getOwningPlugin().getName().equals("MyPet")) {
                Map<UUID, Double> damageMap = (Map<UUID, Double>) value.value();
                if (damageMap.containsKey(damager.getUniqueId())) {
                    return damageMap.get(damager.getUniqueId());
                }
                return 0;
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static double getDamageToEntityPercent(LivingEntity damager, LivingEntity victim) {
        if (victim.hasMetadata("MyPetDamageCount")) {
            UUID damagerUUID = damager.getUniqueId();
            for (MetadataValue value : victim.getMetadata("MyPetDamageCount")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    Map<UUID, Double> damageMap = (Map<UUID, Double>) value.value();
                    double allDamage = 0;
                    double damagerDamage = damageMap.containsKey(damagerUUID) ? damageMap.get(damagerUUID) : 0;
                    for (UUID entity : damageMap.keySet()) {
                        allDamage += damageMap.get(entity);
                    }
                    return damagerDamage / allDamage;
                }
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static Map<UUID, Double> getDamageToEntityPercent(LivingEntity victim) {
        Map<UUID, Double> damagePercentMap = new HashMap<>();
        if (victim.hasMetadata("MyPetDamageCount")) {
            for (MetadataValue value : victim.getMetadata("MyPetDamageCount")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    Map<UUID, Double> damageMap = (Map<UUID, Double>) value.value();
                    double allDamage = 0;
                    for (Double damage : damageMap.values()) {
                        allDamage += damage;
                    }
                    if (allDamage <= 0) {
                        return damagePercentMap;
                    }
                    for (UUID entity : damageMap.keySet()) {
                        damagePercentMap.put(entity, damageMap.get(entity) / allDamage);
                    }
                    return damagePercentMap;
                }
            }
        }
        return damagePercentMap;
    }
}