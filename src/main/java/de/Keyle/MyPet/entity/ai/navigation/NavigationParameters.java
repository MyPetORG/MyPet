/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.ai.navigation;

import java.util.HashMap;
import java.util.Map;

public class NavigationParameters
{
    private boolean avoidWater = false;
    private float speed;
    private Map<String, Float> speedModifier = new HashMap<String, Float>();

    public NavigationParameters(float baseSpeed)
    {
        speed = baseSpeed;
    }

    public void avoidWater(boolean avoidWater)
    {
        this.avoidWater = avoidWater;
    }

    public boolean avoidWater()
    {
        return avoidWater;
    }

    public void speed(float speed)
    {
        this.speed = speed;
    }

    public float speed()
    {
        return speed;
    }

    public void addSpeedModifier(String id, float speedModifier)
    {
        this.speedModifier.put(id, speedModifier);
    }

    public void removeSpeedModifier(String id)
    {
        this.speedModifier.remove(id);
    }

    public float speedModifier()
    {
        float speedModifier = 0F;
        for (Float sm : this.speedModifier.values())
        {
            speedModifier += sm;
        }
        return speedModifier;
    }
}