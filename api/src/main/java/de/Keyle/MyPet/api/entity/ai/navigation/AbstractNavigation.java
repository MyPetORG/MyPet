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

package de.Keyle.MyPet.api.entity.ai.navigation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Abstraction over the pathfinding/movement system for a pet entity.
 * <p>
 * Goals (movement, attack, follow) interact with the pet's locomotion
 * exclusively through this class rather than calling Bukkit's
 * {@code Pathfinder} API directly. This allows the navigation layer to
 * apply speed modifiers, water-avoidance, and parameter syncing in one
 * place.
 * <p>
 * The concrete implementation ({@code PaperNavigation} in the plugin
 * module) delegates to the Paper {@code Pathfinder} and manages the
 * {@code MOVEMENT_SPEED} attribute on the underlying Bukkit {@code Mob}.
 *
 * @see NavigationParameters
 */
@RequiredArgsConstructor
public abstract class AbstractNavigation {

    /**
     * Mutable parameter set controlling speed and modifiers.
     * Goals may cache this reference and adjust modifiers (e.g. sprint
     * boost) without re-fetching.
     */
    @Getter
    protected final NavigationParameters parameters;

    /**
     * Creates a navigation instance with default parameters at the given
     * base walk speed.
     *
     * @param walkSpeed base movement speed (blocks/tick multiplier applied
     *                  to the mob's {@code MOVEMENT_SPEED} attribute)
     */
    public AbstractNavigation(double walkSpeed) {
        this.parameters = new NavigationParameters(walkSpeed);
    }

    /**
     * Cancels any active pathfinding and halts the pet. Goals call this
     * when their target becomes invalid or the goal is interrupted.
     */
    public abstract void stop();

    /**
     * Starts pathfinding toward the given world coordinates.
     *
     * @return {@code true} if a path was successfully computed and
     * navigation begun; {@code false} if the target is unreachable
     */
    public abstract boolean navigateTo(double x, double y, double z);

    /**
     * Pushes the current {@link NavigationParameters} (speed, modifiers,
     * water-avoidance) onto the underlying Bukkit mob. Called after
     * parameter changes to ensure the entity's attribute values reflect
     * the latest state.
     */
    public abstract void applyNavigationParameters();

    /**
     * Convenience overload — navigates to the given location's block
     * coordinates.
     */
    public boolean navigateTo(Location loc) {
        return navigateTo(loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Convenience overload — navigates toward a living entity's current
     * position. Note: this is a one-shot calculation; re-call on
     * subsequent ticks to track a moving target.
     */
    public boolean navigateTo(LivingEntity entity) {
        return navigateTo(entity.getLocation());
    }

    /**
     * Called once per pet tick to perform any per-frame navigation
     * bookkeeping (e.g., re-applying parameters, checking path
     * completion). The pet's scheduler invokes this — goals do not need
     * to call it manually.
     */
    public abstract void tick();
}
