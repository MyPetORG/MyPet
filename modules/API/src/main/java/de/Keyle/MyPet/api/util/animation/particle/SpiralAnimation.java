/*
 * This file is part of mypet-api_main
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api_main is licensed under the GNU Lesser General Public License.
 *
 * mypet-api_main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api_main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.animation.particle;

import de.Keyle.MyPet.api.util.animation.ParticleAnimation;
import de.Keyle.MyPet.api.util.location.LocationHolder;
import org.bukkit.Location;

public abstract class SpiralAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double stepY;
    protected final double stepRadius;
    protected final double height;
    protected double factor = 15;

    public SpiralAnimation(double radius, double height, LocationHolder location) {
        super(30, location);
        this.radius = radius;
        this.height = height;
        this.stepY = height / this.length;
        this.stepRadius = 1. / this.length;
    }

    @Override
    public void tick(int frame, Location location) {
        double y = location.getY() + (frame * stepY);
        double x;
        double z;
        for (double offset = 0; offset < 1; offset += 0.3) {
            x = location.getX() + (radius * Math.cos(((frame - offset) * stepRadius) * factor));
            z = location.getZ() + (radius * Math.sin(((frame - offset) * stepRadius) * factor));

            playParticleEffect(new Location(location.getWorld(), x, y, z));
        }
    }
}