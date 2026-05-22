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
 * Accessed globally via {@code MyPetApi.getPetInfo()}. The concrete
 * implementation ({@code PetInfoImpl} in the plugin module) adds
 * leashability checks and any version-gated type filtering.
 */
public abstract class PetInfo {

    private final Map<PetType, Double> startHP = new HashMap<>();
    private final Map<PetType, Double> startSpeed = new HashMap<>();
    private final Map<PetType, Boolean> overrideFlySpeed = new HashMap<>();
    private final Map<PetType, Double> startFlySpeed = new HashMap<>();
    private final ArrayListMultimap<PetType, ConfigItem> food = ArrayListMultimap.create();
    private final ArrayListMultimap<PetType, Settings> leashFlagSettings = ArrayListMultimap.create();
    private final Map<PetType, Integer> customRespawnTimeFactor = new HashMap<>();
    private final Map<PetType, Integer> customRespawnTimeFixed = new HashMap<>();
    private final Map<PetType, Boolean> releaseOnDeath = new HashMap<>();
    private final Map<PetType, Boolean> removeAfterRelease = new HashMap<>();
    private final Map<PetType, ConfigItem> leashItem = new HashMap<>();

    /**
     * Per-level multiplier added to the base respawn timer. Total respawn
     * time = {@code fixed + (factor * petLevel)}.
     */
    public int getCustomRespawnTimeFactor(PetType type) {
        return customRespawnTimeFactor.getOrDefault(type, 0);
    }

    public void setCustomRespawnTimeFactor(PetType type, int factor) {
        customRespawnTimeFactor.put(type, factor);
    }

    /** Flat respawn time (seconds) added regardless of pet level. */
    public int getCustomRespawnTimeFixed(PetType type) {
        return customRespawnTimeFixed.getOrDefault(type, 0);
    }

    public void setCustomRespawnTimeFixed(PetType type, int factor) {
        customRespawnTimeFixed.put(type, factor);
    }

    /** Returns the list of items that restore saturation for this type. */
    public List<ConfigItem> getFood(PetType type) {
        return food.get(type);
    }

    public void clearFood(PetType type) {
        food.removeAll(type);
    }

    /** Adds a food item if not already registered (duplicate-safe). */
    public void addFood(PetType type, ConfigItem foodToAdd) {
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
    public List<Settings> getLeashFlagSettings(PetType type) {
        return leashFlagSettings.get(type);
    }

    public void addLeashFlagSetting(PetType type, Settings setting) {
        if (!leashFlagSettings.get(type).contains(setting)) {
            leashFlagSettings.put(type, setting);
        }
    }

    public void clearLeashFlagSettings(PetType petType) {
        if (leashFlagSettings.containsKey(petType)) {
            leashFlagSettings.get(petType).clear();
        }
    }

    /** Starting max health for freshly tamed pets of this type. */
    public double getStartHP(PetType type) {
        return startHP.getOrDefault(type, 20.0);
    }

    public void setStartHP(PetType type, double hp) {
        startHP.put(type, hp);
    }

    /**
     * The item the player must use to leash (tame) this mob type, or
     * {@code null} if the default (lead) applies.
     */
    public ConfigItem getLeashItem(PetType type) {
        return leashItem.get(type);
    }

    public void setLeashItem(PetType type, ConfigItem configItem) {
        leashItem.put(type, configItem);
    }

    /**
     * Base walk speed (Bukkit MOVEMENT_SPEED attribute value). Falls back
     * to {@code 0.3} if no override is configured.
     */
    public double getSpeed(PetType petType) {
        if (petType == null) {
            return 0.3;
        }
        return startSpeed.getOrDefault(petType, 0.3);
    }

    public void setSpeed(PetType type, double speed) {
        startSpeed.put(type, speed);
    }

    /**
     * Per-pet toggle controlling whether the Ride skill's flight controller
     * uses {@link #getFlySpeed(PetType)} verbatim or derives ride speed
     * from the mob's vanilla {@code FLYING_SPEED} / {@code MOVEMENT_SPEED}
     * attribute. Default is {@code false} (derive from vanilla); set
     * {@code true} via {@link #setOverrideFlySpeed(PetType, boolean)} (or
     * the {@code MyPet.Pets.<Type>.OverrideFlySpeed} YAML key, or the
     * {@code @DefaultInfo#overrideFlySpeed} annotation field) to lock the
     * ride speed to the configured {@code FlySpeed} value.
     */
    public boolean isOverrideFlySpeed(PetType petType) {
        if (petType == null) {
            return false;
        }
        return overrideFlySpeed.getOrDefault(petType, false);
    }

    public void setOverrideFlySpeed(PetType type, boolean override) {
        overrideFlySpeed.put(type, override);
    }

    /**
     * Per-pet fly-speed value used by the Ride skill's flight controller
     * when {@link #isOverrideFlySpeed(PetType)} returns {@code true}.
     * Authored in direct-per-tick-velocity units. When the toggle is
     * {@code false}, this value is ignored and the controller derives
     * speed from the live Bukkit mob's {@code FLYING_SPEED} /
     * {@code MOVEMENT_SPEED} attribute. Falls back to {@code 0.6} if
     * no override is configured.
     */
    public double getFlySpeed(PetType petType) {
        if (petType == null) {
            return 0.6;
        }
        return startFlySpeed.getOrDefault(petType, 0.6);
    }

    public void setFlySpeed(PetType type, double flySpeed) {
        startFlySpeed.put(type, flySpeed);
    }

    /**
     * Returns whether a given Bukkit {@link EntityType} can be leashed as
     * a pet at all. The implementation typically checks version gates and
     * the registered {@link PetType} set.
     */
    public abstract boolean isLeashableEntityType(EntityType type);

    /**
     * If {@code true}, the pet is permanently released (deleted) when it
     * dies instead of entering the respawn timer.
     */
    public void setReleaseOnDeath(PetType petType, boolean releaseOnDeath) {
        this.releaseOnDeath.put(petType, releaseOnDeath);
    }

    /**
     * If {@code true}, the Bukkit entity is removed from the world
     * immediately after releasing the pet (rather than leaving the mob
     * alive as a wild entity).
     */
    public void setRemoveAfterRelease(PetType petType, boolean removeAfterRelease) {
        this.removeAfterRelease.put(petType, removeAfterRelease);
    }

    public boolean getReleaseOnDeath(PetType petType) {
        if (petType == null) {
            return false;
        }
        return releaseOnDeath.getOrDefault(petType, false);
    }

    public boolean getRemoveAfterRelease(PetType petType) {
        if (petType == null) {
            return false;
        }
        return removeAfterRelease.getOrDefault(petType, false);
    }
}