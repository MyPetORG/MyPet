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

import java.util.HashMap;
import java.util.Map;

public class NavigationParameters {
    private boolean avoidWater = false;
    private double speed;
    private Map<String, Double> speedModifier = new HashMap<>();
    /**
     * Optional callback fired whenever a speed modifier is added or removed.
     * PaperNavigation uses this to re-apply parameters to MOVEMENT_SPEED
     * immediately, so that goals which only call removeSpeedModifier() (e.g.
     * PetSprintGoal) actually see their speed drop without waiting for the
     * next navigateTo() or nav.stop().
     */
    private Runnable onSpeedChange;

    public NavigationParameters(double baseSpeed) {
        speed = baseSpeed;
    }

    public void avoidWater(boolean avoidWater) {
        this.avoidWater = avoidWater;
    }

    public boolean avoidWater() {
        return avoidWater;
    }

    public void speed(double speed) {
        this.speed = speed;
    }

    public double speed() {
        return speed;
    }

    public void addSpeedModifier(String id, double speedModifier) {
        this.speedModifier.put(id, speedModifier);
        notifySpeedChange();
    }

    public void removeSpeedModifier(String id) {
        if (this.speedModifier.remove(id) != null) {
            notifySpeedChange();
        }
    }

    public double speedModifier() {
        return this.speedModifier.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public void setOnSpeedChange(Runnable onSpeedChange) {
        this.onSpeedChange = onSpeedChange;
    }

    private void notifySpeedChange() {
        if (onSpeedChange != null) {
            onSpeedChange.run();
        }
    }
}