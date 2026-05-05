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

/**
 * Per-type metadata registry. Stores the configurable attributes that
 * vary by pet type — starting HP, walk speed, food items, leash flags,
 * respawn timers, and death behaviour. Populated at startup from
 * {@link DefaultInfo} annotations (hardcoded defaults) then overwritten
 * by values loaded from {@code pet-config.yml}.
 * <p>
 * Accessed globally via {@code MyPetApi.getMyPetInfo()}. The concrete
 * implementation ({@code MyPetInfoImpl} in the plugin module) adds
 * leashability checks and any version-gated type filtering.
 */
public abstract class MyPetInfo {

    private final Map<MyPetType, Double> startHP = new HashMap<>();
    private final Map<MyPetType, Double> startSpeed = new HashMap<>();
    private final ArrayListMultimap<MyPetType, ConfigItem> food = ArrayListMultimap.create();
    private final ArrayListMultimap<MyPetType, Settings> leashFlagSettings = ArrayListMultimap.create();
    private final Map<MyPetType, Integer> customRespawnTimeFactor = new HashMap<>();
    private final Map<MyPetType, Integer> customRespawnTimeFixed = new HashMap<>();
    private final Map<MyPetType, Boolean> releaseOnDeath = new HashMap<>();
    private final Map<MyPetType, Boolean> removeAfterRelease = new HashMap<>();
    private final Map<MyPetType, ConfigItem> leashItem = new HashMap<>();

    /**
     * Per-level multiplier added to the base respawn timer. Total respawn
     * time = {@code fixed + (factor * petLevel)}.
     */
    public int getCustomRespawnTimeFactor(MyPetType type) {
        return customRespawnTimeFactor.getOrDefault(type, 0);
    }

    public void setCustomRespawnTimeFactor(MyPetType type, int factor) {
        customRespawnTimeFactor.put(type, factor);
    }

    /** Flat respawn time (seconds) added regardless of pet level. */
    public int getCustomRespawnTimeFixed(MyPetType type) {
        return customRespawnTimeFixed.getOrDefault(type, 0);
    }

    public void setCustomRespawnTimeFixed(MyPetType type, int factor) {
        customRespawnTimeFixed.put(type, factor);
    }

    /** Returns the list of items that restore saturation for this type. */
    public List<ConfigItem> getFood(MyPetType type) {
        return food.get(type);
    }

    public void clearFood(MyPetType type) {
        food.removeAll(type);
    }

    /** Adds a food item if not already registered (duplicate-safe). */
    public void addFood(MyPetType type, ConfigItem foodToAdd) {
        for (ConfigItem configItem : food.get(type)) {
            if (configItem.compare(foodToAdd.getItem())) {
                return;
            }
        }
        food.put(type, foodToAdd);
    }

    /**
     * Returns the configured leash flag settings for a pet type — each
     * entry corresponds to one flag with its per-flag parameters (e.g.,
     * chance percentage, required size).
     */
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

    /** Starting max health for freshly tamed pets of this type. */
    public double getStartHP(MyPetType type) {
        return startHP.getOrDefault(type, 20.0);
    }

    public void setStartHP(MyPetType type, double hp) {
        startHP.put(type, hp);
    }

    /**
     * The item the player must use to leash (tame) this mob type, or
     * {@code null} if the default (lead) applies.
     */
    public ConfigItem getLeashItem(MyPetType type) {
        return leashItem.get(type);
    }

    public void setLeashItem(MyPetType type, ConfigItem configItem) {
        leashItem.put(type, configItem);
    }

    /**
     * Base walk speed (Bukkit MOVEMENT_SPEED attribute value). Falls back
     * to {@code 0.3} if no override is configured.
     */
    public double getSpeed(MyPetType myPetType) {
        if (myPetType == null) {
            return 0.3;
        }
        return startSpeed.getOrDefault(myPetType, 0.3);
    }

    public void setSpeed(MyPetType type, double speed) {
        startSpeed.put(type, speed);
    }

    /**
     * Returns whether a given Bukkit {@link EntityType} can be leashed as
     * a pet at all. The implementation typically checks version gates and
     * the registered {@link MyPetType} set.
     */
    public abstract boolean isLeashableEntityType(EntityType type);

    /**
     * If {@code true}, the pet is permanently released (deleted) when it
     * dies instead of entering the respawn timer.
     */
    public void setReleaseOnDeath(MyPetType myPetType, boolean releaseOnDeath) {
        this.releaseOnDeath.put(myPetType, releaseOnDeath);
    }

    /**
     * If {@code true}, the Bukkit entity is removed from the world
     * immediately after releasing the pet (rather than leaving the mob
     * alive as a wild entity).
     */
    public void setRemoveAfterRelease(MyPetType myPetType, boolean removeAfterRelease) {
        this.removeAfterRelease.put(myPetType, removeAfterRelease);
    }

    public boolean getReleaseOnDeath(MyPetType myPetType) {
        if (myPetType == null) {
            return false;
        }
        return releaseOnDeath.getOrDefault(myPetType, false);
    }

    public boolean getRemoveAfterRelease(MyPetType myPetType) {
        if (myPetType == null) {
            return false;
        }
        return removeAfterRelease.getOrDefault(myPetType, false);
    }
}