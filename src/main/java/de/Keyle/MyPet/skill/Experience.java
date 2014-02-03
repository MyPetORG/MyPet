/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.event.MyPetExpEvent;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.experience.Default;
import de.Keyle.MyPet.skill.experience.JavaScript;
import de.Keyle.MyPet.util.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.Map;

public class Experience {
    public static int LOSS_PERCENT = 0;
    public static double LOSS_FIXED = 0;
    public static boolean DROP_LOST_EXP = true;
    public static boolean GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS = true;
    public static String CALCULATION_MODE = "Default";
    public static boolean DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = false;
    public static boolean FIREWORK_ON_LEVELUP = true;

    private final MyPet myPet;

    private double exp = 0;
    de.Keyle.MyPet.skill.experience.Experience expMode;

    public Experience(MyPet pet) {
        this.myPet = pet;

        if (CALCULATION_MODE.equalsIgnoreCase("JS") || CALCULATION_MODE.equalsIgnoreCase("JavaScript")) {
            expMode = new JavaScript(myPet);
        } else {
            expMode = new Default(myPet);
        }
        if (!expMode.isUsable()) {
            expMode = new Default(myPet);
            CALCULATION_MODE = "Default";
        }

        for (int i = 1; i <= getLevel(); i++) {
            Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i, true));
        }
    }

    public void reset() {
        exp = 0;

        for (int i = 1; i <= getLevel(); i++) {
            Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i, true));
        }
    }

    public void setExp(double exp) {
        exp = exp < 0 ? 0 : exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.getExp(), exp);
        if (Configuration.ENABLE_EVENTS) {
            Bukkit.getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled()) {
                return;
            }
        }
        int tmplvl = getLevel();
        this.exp = expEvent.getExp();

        for (int i = tmplvl; i < getLevel(); i++) {
            Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i + 1, true));
        }
    }

    public double getExp() {
        return this.exp;
    }

    public double addExp(double exp) {
        MyPetExpEvent event = new MyPetExpEvent(myPet, this.exp, this.exp + exp);
        if (Configuration.ENABLE_EVENTS) {
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return 0;
            }
        }
        int tmpLvl = getLevel();
        this.exp = event.getExp();

        for (int i = tmpLvl; i < getLevel(); i++) {
            boolean quiet = i != getLevel() - 1;
            Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i + 1, quiet));
        }
        return event.getNewExp() - event.getOldExp();
    }

    public double addExp(EntityType type) {
        if (MonsterExperience.hasMonsterExperience(type)) {
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, MonsterExperience.getMonsterExperience(type).getRandomExp() + this.exp);
            if (Configuration.ENABLE_EVENTS) {
                Bukkit.getServer().getPluginManager().callEvent(expEvent);
                if (expEvent.isCancelled()) {
                    return 0;
                }
            }
            int tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            for (int i = tmpLvl; i < getLevel(); i++) {
                boolean quiet = i != getLevel() - 1;
                Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i + 1, quiet));
            }
            return expEvent.getNewExp() - expEvent.getOldExp();
        }
        return 0;
    }

    public double addExp(EntityType type, int percent) {
        if (MonsterExperience.hasMonsterExperience(type)) {
            double exp = MonsterExperience.getMonsterExperience(type).getRandomExp() / 100. * percent;
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, exp + this.exp);
            if (Configuration.ENABLE_EVENTS) {
                Bukkit.getServer().getPluginManager().callEvent(expEvent);
                if (expEvent.isCancelled()) {
                    return 0;
                }
            }
            int tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            for (int i = tmpLvl; i < getLevel(); i++) {
                boolean quiet = i != getLevel() - 1;
                Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, i + 1, quiet));
            }
            return expEvent.getNewExp() - expEvent.getOldExp();
        }
        return 0;
    }

    public void removeCurrentExp(double exp) {
        if (exp > getCurrentExp()) {
            exp = getCurrentExp();
        }
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, this.exp - exp);
        if (Configuration.ENABLE_EVENTS) {
            Bukkit.getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled()) {
                return;
            }
        }
        this.exp = expEvent.getExp();
    }

    public void removeExp(double exp) {
        exp = this.exp - exp < 0 ? this.exp : exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, this.exp - exp);
        if (Configuration.ENABLE_EVENTS) {
            Bukkit.getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled()) {
                return;
            }
        }
        this.exp = expEvent.getExp();
    }

    public double getCurrentExp() {
        double currentExp = expMode.getCurrentExp(this.exp);
        if (!expMode.isUsable()) {
            expMode = new Default(myPet);
            return expMode.getCurrentExp(this.exp);
        }
        int skilltreeMaxLevel = myPet.getSkillTree() != null ? myPet.getSkillTree().getMaxLevel() : 0;
        if (skilltreeMaxLevel != 0 && getLevel() >= skilltreeMaxLevel) {
            return 0;
        }
        return currentExp;
    }

    public int getLevel() {
        int currentLevel = expMode.getLevel(this.exp);
        if (!expMode.isUsable()) {
            expMode = new Default(myPet);
            return expMode.getLevel(this.exp);
        }
        int skilltreeMaxLevel = myPet.getSkillTree() != null ? myPet.getSkillTree().getMaxLevel() : 0;
        if (skilltreeMaxLevel != 0 && currentLevel > skilltreeMaxLevel) {
            return skilltreeMaxLevel;
        }
        return currentLevel;
    }

    public double getRequiredExp() {
        double requiredExp = expMode.getRequiredExp(this.exp);
        if (!expMode.isUsable()) {
            expMode = new Default(myPet);
            return expMode.getRequiredExp(this.exp);
        }
        int skilltreeMaxLevel = myPet.getSkillTree() != null ? myPet.getSkillTree().getMaxLevel() : 0;
        if (skilltreeMaxLevel != 0 && getLevel() >= skilltreeMaxLevel) {
            return 0;
        }
        return requiredExp;
    }

    public double getExpByLevel(int level) {
        return expMode.getExpByLevel(level);
    }

    @SuppressWarnings("unchecked")
    public static void addDamageToEntity(LivingEntity damager, LivingEntity victim, double damage) {
        Map<Entity, Double> damageMap;
        if (victim.hasMetadata("DamageCount")) {
            for (MetadataValue value : victim.getMetadata("DamageCount")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    damageMap = (Map<Entity, Double>) value.value();
                    if (damageMap.containsKey(damager)) {
                        double oldDamage = damageMap.get(damager);
                        damageMap.put(damager, victim.getHealth() < damage ? victim.getHealth() + oldDamage : damage + oldDamage);
                    } else {
                        damageMap.put(damager, victim.getHealth() < damage ? victim.getHealth() : damage);
                    }
                    break;
                }
            }
        } else {
            damageMap = new HashMap<Entity, Double>();
            damageMap.put(damager, victim.getHealth() < damage ? victim.getHealth() : damage);
            victim.setMetadata("DamageCount", new FixedMetadataValue(MyPetPlugin.getPlugin(), damageMap));
        }
    }

    @SuppressWarnings("unchecked")
    public static double getDamageToEntity(LivingEntity damager, LivingEntity victim) {
        for (MetadataValue value : victim.getMetadata("DamageCount")) {
            if (value.getOwningPlugin().getName().equals("MyPet")) {
                Map<Entity, Double> damageMap = (Map<Entity, Double>) value.value();
                if (damageMap.containsKey(damager)) {
                    return damageMap.get(damager);
                }
                return 0;
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static double getDamageToEntityPercent(LivingEntity damager, LivingEntity victim) {
        if (victim.hasMetadata("DamageCount")) {
            for (MetadataValue value : victim.getMetadata("DamageCount")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    Map<Entity, Double> damageMap = (Map<Entity, Double>) value.value();
                    double allDamage = 0;
                    double damagerDamage = 0;
                    for (Entity entity : damageMap.keySet()) {
                        if (entity == damager) {
                            damagerDamage = damageMap.get(damager);
                        }
                        allDamage += damageMap.get(entity);
                    }
                    return damagerDamage / allDamage;
                }
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static Map<Entity, Double> getDamageToEntityPercent(LivingEntity victim) {
        Map<Entity, Double> damagePercentMap = new HashMap<Entity, Double>();
        if (victim.hasMetadata("DamageCount")) {
            for (MetadataValue value : victim.getMetadata("DamageCount")) {
                if (value.getOwningPlugin().getName().equals("MyPet")) {
                    Map<Entity, Double> damageMap = (Map<Entity, Double>) value.value();
                    double allDamage = 0;
                    for (Double damage : damageMap.values()) {
                        allDamage += damage;
                    }

                    if (allDamage <= 0) {
                        return damagePercentMap;
                    }

                    for (Entity entity : damageMap.keySet()) {
                        damagePercentMap.put(entity, damageMap.get(entity) / allDamage);
                    }

                    return damagePercentMap;
                }
            }
        }
        return damagePercentMap;
    }
}