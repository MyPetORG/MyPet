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

package de.Keyle.MyPet.util.animation.particle;

import de.Keyle.MyPet.util.animation.ParticleAnimation;
import org.bukkit.Location;

import java.util.function.Supplier;

public abstract class OrbitAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double stepAngle;
    protected int rotationAngle = 0;

    public OrbitAnimation(double radius, Supplier<Location> location) {
        super(9000, location);
        this.setFramesPerTick(1);
        this.radius = radius;
        this.stepAngle = 0.08;
        this.tickRate = 3;
    }

    @Override
    public void reset() {
        super.reset();
        rotationAngle = 0;
    }

    @Override
    public void tick(int frame, Location location) {
        Location loc = location.clone();

        double zangle = Math.toRadians(rotationAngle++);
        double zAxisCos = Math.cos(zangle);
        double zAxisSin = Math.sin(zangle);

        Location rotLoc = new Location(loc.getWorld(), radius * Math.cos(frame * stepAngle), 0, radius * Math.sin(frame * stepAngle));

        rotateAroundAxisZ(rotLoc, zAxisCos, zAxisSin);

        loc.add(rotLoc);

        playParticleEffect(loc);
    }

    private void rotateAroundAxisZ(Location v, double cos, double sin) {
        double x = v.getX() * cos - v.getY() * sin;
        double y = v.getX() * sin + v.getY() * cos;
        v.setX(x);
        v.setY(y);
    }
}
