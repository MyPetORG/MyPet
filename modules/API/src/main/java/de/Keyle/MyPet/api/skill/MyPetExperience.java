/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetExpEvent;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.skill.experience.Experience;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class MyPetExperience {

    private Experience expMode = null;
    private final MyPet myPet;
    private double exp = 0;
    private double levelCapExp = 0;

    public MyPetExperience(MyPet pet, Experience expMode) {
        this.myPet = pet;
        this.expMode = expMode;

        reset();
    }

    public void reset() {
        levelCapExp = getExpByLevel(Configuration.LevelSystem.Experience.LEVEL_CAP);
        exp = 0;
        Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, getLevel(), 0, true));
    }

    public void setExp(double exp) {
        exp = Math.max(0, exp);
        exp = Math.min(levelCapExp, exp);
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, exp);
        Bukkit.getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled()) {
            return;
        }
        int tmplvl = getLevel();
        this.exp = expEvent.getExp();

        if (tmplvl != getLevel()) {
            Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, getLevel(), tmplvl, true));
        }
    }

    public double getMaxExp() {
        return levelCapExp;
    }

    public double getExp() {
        return this.exp;
    }

    public double addExp(double exp) {
        MyPetExpEvent event = new MyPetExpEvent(myPet, this.exp, Math.min(levelCapExp, this.exp + exp));
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }
        int tmpLvl = getLevel();
        this.exp = event.getExp();

        if (tmpLvl < getLevel()) {
            Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, getLevel(), tmpLvl));
        }

        return event.getNewExp() - event.getOldExp();
    }

    public double addExp(EntityType type) {
        MonsterExperience monsterExperience = MonsterExperience.getMonsterExperience(type);
        if (monsterExperience.getEntityType() != EntityType.UNKNOWN) {
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, Math.min(levelCapExp, monsterExperience.getRandomExp() + this.exp));
            Bukkit.getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled()) {
                return 0;
            }
            int tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            if (tmpLvl < getLevel()) {
                Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, getLevel(), tmpLvl));
            }

            return expEvent.getNewExp() - expEvent.getOldExp();
        }
        return 0;
    }

    public double addExp(EntityType type, int percent) {
        MonsterExperience monsterExperience = MonsterExperience.getMonsterExperience(type);
        if (monsterExperience.getEntityType() != EntityType.UNKNOWN) {
            double exp = monsterExperience.getRandomExp() / 100. * percent;
            MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, Math.min(levelCapExp, this.exp + exp));
            Bukkit.getServer().getPluginManager().callEvent(expEvent);
            if (expEvent.isCancelled()) {
                return 0;
            }
            int tmpLvl = getLevel();
            this.exp = expEvent.getExp();

            if (tmpLvl < getLevel()) {
                Bukkit.getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(myPet, getLevel(), tmpLvl));
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
        Bukkit.getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled()) {
            return;
        }
        this.exp = expEvent.getExp();
    }

    public void removeExp(double exp) {
        exp = this.exp - exp < 0 ? this.exp : exp;
        MyPetExpEvent expEvent = new MyPetExpEvent(myPet, this.exp, this.exp - exp);
        Bukkit.getServer().getPluginManager().callEvent(expEvent);
        if (expEvent.isCancelled()) {
            return;
        }
        this.exp = expEvent.getExp();
    }

    public double getCurrentExp() {
        double currentExp = expMode.getCurrentExp(this.exp);
        if (!expMode.isUsable()) {
            return 0;
        }
        int skilltreeMaxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : 0;
        if (skilltreeMaxLevel != 0 && getLevel() >= skilltreeMaxLevel) {
            return 0;
        }
        return currentExp;
    }

    public int getLevel() {
        int currentLevel = expMode.getLevel(this.exp);
        if (!expMode.isUsable()) {
            return 1;
        }
        int skilltreeMaxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : 0;
        if (skilltreeMaxLevel != 0 && currentLevel > skilltreeMaxLevel) {
            return skilltreeMaxLevel;
        }
        return currentLevel;
    }

    public double getRequiredExp() {
        double requiredExp = expMode.getRequiredExp(this.exp);
        if (!expMode.isUsable()) {
            return 0;
        }
        int skilltreeMaxLevel = myPet.getSkilltree() != null ? myPet.getSkilltree().getMaxLevel() : 0;
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
            damageMap = new WeakHashMap<>();
            damageMap.put(damager, victim.getHealth() < damage ? victim.getHealth() : damage);
            victim.setMetadata("DamageCount", new FixedMetadataValue(MyPetApi.getPlugin(), damageMap));
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
        Map<Entity, Double> damagePercentMap = new HashMap<>();
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