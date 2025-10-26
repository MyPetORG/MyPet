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

package de.Keyle.MyPet.api.util.animation.particle;

import de.Keyle.MyPet.api.util.animation.ParticleAnimation;
import de.Keyle.MyPet.api.util.location.LocationHolder;
import org.bukkit.Location;

public abstract class CircleAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double anglePerTick;
    protected final double height;

    public CircleAnimation(double radius, double height, LocationHolder location) {
        super(20, location);
        this.radius = radius;
        this.height = height / 2;
        this.anglePerTick = (2 * Math.PI) / this.length;
    }

    @Override
    public void tick(int frame, Location location) {
        double y = location.getY() + this.height;
        double x = location.getX() + (radius * Math.cos(frame * anglePerTick));
        double z = location.getZ() + (radius * Math.sin(frame * anglePerTick));
        playParticleEffect(new Location(location.getWorld(), x, y, z));
    }
}
