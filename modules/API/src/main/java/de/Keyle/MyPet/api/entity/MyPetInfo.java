/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.entity;

import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.api.util.ConfigItem;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MyPetInfo {
    private Map<MyPetType, Double> startHP = new HashMap<>();
    private Map<MyPetType, Double> startSpeed = new HashMap<>();
    private ArrayListMultimap<MyPetType, ConfigItem> food = ArrayListMultimap.create();
    private ArrayListMultimap<MyPetType, LeashFlag> leashFlags = ArrayListMultimap.create();
    private Map<MyPetType, Integer> customRespawnTimeFactor = new HashMap<>();
    private Map<MyPetType, Integer> customRespawnTimeFixed = new HashMap<>();
    private Map<MyPetType, ConfigItem> leashItem = new HashMap<>();

    public int getCustomRespawnTimeFactor(MyPetType type) {
        if (customRespawnTimeFactor.containsKey(type)) {
            return customRespawnTimeFactor.get(type);
        }
        return 0;
    }

    public void setCustomRespawnTimeFactor(MyPetType type, int factor) {
        customRespawnTimeFactor.put(type, factor);
    }

    public int getCustomRespawnTimeFixed(MyPetType type) {
        if (customRespawnTimeFixed.containsKey(type)) {
            return customRespawnTimeFixed.get(type);
        }
        return 0;
    }

    public void setCustomRespawnTimeFixed(MyPetType type, int factor) {
        customRespawnTimeFixed.put(type, factor);
    }

    public List<ConfigItem> getFood(MyPetType type) {
        return food.get(type);
    }

    public void setFood(MyPetType type, ConfigItem foodToAdd) {
        for (ConfigItem configItem : food.get(type)) {
            if (configItem.compare(foodToAdd.getItem())) {
                return;
            }
        }
        food.put(type, foodToAdd);
    }

    public boolean hasLeashFlag(MyPetType type, LeashFlag flag) {
        return leashFlags.get(type).contains(flag);
    }

    public List<LeashFlag> getLeashFlags(MyPetType type) {
        return leashFlags.get(type);
    }

    public void setLeashFlags(MyPetType type, LeashFlag leashFlagToAdd) {
        if (!leashFlags.get(type).contains(leashFlagToAdd)) {
            leashFlags.put(type, leashFlagToAdd);
        }
    }

    public double getStartHP(MyPetType type) {
        if (startHP.containsKey(type)) {
            return startHP.get(type);
        }
        return 20;
    }

    public void setStartHP(MyPetType type, double hp) {
        startHP.put(type, hp);
    }

    public ConfigItem getLeashItem(MyPetType type) {
        return leashItem.get(type);
    }

    public void setLeashItem(MyPetType type, ConfigItem configItem) {
        leashItem.put(type, configItem);
    }

    public double getSpeed(MyPetType myPetType) {
        if (myPetType != null) {
            return startSpeed.get(myPetType);
        }
        return 0.3F;
    }

    public void setSpeed(MyPetType type, double speed) {
        startSpeed.put(type, speed);
    }

    public abstract boolean isLeashableEntityType(EntityType type);
}