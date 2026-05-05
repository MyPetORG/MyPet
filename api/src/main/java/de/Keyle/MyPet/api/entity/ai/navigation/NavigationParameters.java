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

package de.Keyle.MyPet.api.entity.ai.navigation;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable parameter bag that controls how a pet navigates the world.
 * <p>
 * Holds the base speed, per-goal speed modifiers, and environmental flags
 * (water avoidance). Goals manipulate these values between ticks; the
 * navigation implementation reads them when computing the next path or
 * applying attribute values to the Bukkit mob.
 * <p>
 * Speed modifiers are additive and keyed by a unique string ID so that
 * multiple goals (sprint, slow, control) can independently layer bonuses
 * without overwriting each other. The effective speed used during
 * navigation is {@code base + sum(modifiers)}.
 *
 * @see AbstractNavigation#applyNavigationParameters()
 */
public class NavigationParameters {

    private final Map<String, Double> speedModifier = new HashMap<>();
    /**
     * Whether pathfinding should penalize water blocks. When enabled,
     * the navigation implementation raises the path cost of water nodes, so
     * the pet prefers dry routes.
     *
     * @param avoidWater {@code true} to penalize water paths
     * @return {@code true} if the pet is configured to avoid water paths
     */
    @Getter
    @Setter
    @Accessors(fluent = true)
    private boolean avoidWater = false;
    /**
     * Base movement speed (excluding modifiers). Setting this does
     * <em>not</em> clear existing speed modifiers — the effective speed
     * will be {@code base + sum(modifiers)}.
     *
     * @param speed new base speed
     * @return current base speed
     */
    @Getter
    @Setter
    private double speed;
    /**
     * Optional callback fired whenever a speed modifier is added or removed.
     * PaperNavigation uses this to re-apply parameters to MOVEMENT_SPEED
     * immediately, so that goals which only call removeSpeedModifier() (e.g.,
     * PetSprintGoal) actually see their speed drop without waiting for the
     * next navigateTo() or nav.stop().
     *
     * @param onSpeedChange callback to invoke on modifier mutation, or
     * {@code null} to clear
     */
    @Setter
    private Runnable onSpeedChange;

    /**
     * @param baseSpeed the pet type's default walk speed, typically sourced
     *                  from {@code MyPetInfoImpl} attribute defaults
     */
    public NavigationParameters(double baseSpeed) {
        this.speed = baseSpeed;
    }

    /**
     * Adds or replaces a named speed modifier. If a modifier with the same
     * {@code id} already exists, its value is overwritten.
     * <p>
     * Fires the {@link #setOnSpeedChange(Runnable) onSpeedChange} callback
     * so the navigation layer can immediately sync the attribute.
     *
     * @param id            stable identifier for this modifier (e.g.
     *                      {@code "sprint"}, {@code "slow"})
     * @param speedModifier additive speed delta (positive = faster,
     *                      negative = slower)
     */
    public void addSpeedModifier(String id, double speedModifier) {
        this.speedModifier.put(id, speedModifier);
        notifySpeedChange();
    }

    /**
     * Removes a previously registered speed modifier by ID. No-op if the
     * ID is not present. Fires the speed-change callback only if a value
     * was actually removed.
     */
    public void removeSpeedModifier(String id) {
        if (this.speedModifier.remove(id) != null) {
            notifySpeedChange();
        }
    }

    /**
     * Returns the sum of all active speed modifiers. Added to the base
     * speed to produce the effective movement speed.
     */
    public double speedModifier() {
        return this.speedModifier.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private void notifySpeedChange() {
        if (onSpeedChange != null) {
            onSpeedChange.run();
        }
    }
}