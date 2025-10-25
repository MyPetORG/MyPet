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
    }

    public void removeSpeedModifier(String id) {
        this.speedModifier.remove(id);
    }

    public double speedModifier() {
        double speedModifier = 0D;
        for (Double sm : this.speedModifier.values()) {
            speedModifier += sm;
        }
        return speedModifier;
    }
}