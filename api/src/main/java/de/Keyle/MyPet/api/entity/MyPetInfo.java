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

package de.Keyle.MyPet.api.entity;

import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MyPetInfo {

    private Map<MyPetType, Double> startHP = new HashMap<>();
    private Map<MyPetType, Double> startSpeed = new HashMap<>();
    private ArrayListMultimap<MyPetType, ConfigItem> food = ArrayListMultimap.create();
    private ArrayListMultimap<MyPetType, Settings> leashFlagSettings = ArrayListMultimap.create();
    private Map<MyPetType, Integer> customRespawnTimeFactor = new HashMap<>();
    private Map<MyPetType, Integer> customRespawnTimeFixed = new HashMap<>();
    private Map<MyPetType, Boolean> releaseOnDeath = new HashMap<>();
    private Map<MyPetType, Boolean> removeAfterRelease = new HashMap<>();
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

    public void clearFood(MyPetType type) {
        food.removeAll(type);
    }

    public void addFood(MyPetType type, ConfigItem foodToAdd) {
        for (ConfigItem configItem : food.get(type)) {
            if (configItem.compare(foodToAdd.getItem())) {
                return;
            }
        }
        food.put(type, foodToAdd);
    }

    public List<Settings> getLeashFlagSettings(MyPetType type) {
        return leashFlagSettings.get(type);
    }

    public void addLeashFlagSetting(MyPetType type, Settings setting) {
        if (!leashFlagSettings.get(type).contains(setting)) {
            leashFlagSettings.put(type, setting);
        }
    }

    public void clearLeashFlagSettings(MyPetType petType) {
        if (leashFlagSettings.containsKey(petType)) {
            leashFlagSettings.get(petType).clear();
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

    public void setReleaseOnDeath(MyPetType myPetType, boolean releaseOnDeath) {
        this.releaseOnDeath.put(myPetType, releaseOnDeath);
    }

    public void setRemoveAfterRelease(MyPetType myPetType, boolean removeAfterRelease) {
        this.removeAfterRelease.put(myPetType, removeAfterRelease);
    }

    public boolean getReleaseOnDeath(MyPetType myPetType) {
        if (myPetType != null && releaseOnDeath.containsKey(myPetType)) {
            return releaseOnDeath.get(myPetType);
        }
        return false;
    }

    public boolean getRemoveAfterRelease(MyPetType myPetType) {
        if (myPetType != null && removeAfterRelease.containsKey(myPetType)) {
            return removeAfterRelease.get(myPetType);
        }
        return false;
    }
}