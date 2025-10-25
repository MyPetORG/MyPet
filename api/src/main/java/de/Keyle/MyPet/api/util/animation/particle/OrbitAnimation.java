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

public abstract class OrbitAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double stepAngle;
    protected int rotationAngle = 0;

    public OrbitAnimation(double radius, LocationHolder location) {
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

        //double xangle = Math.toRadians(i++); // note that here we do have to convert to radians.
        //double xAxisCos = Math.cos(xangle); // getting the cos value for the pitch.
        //double xAxisSin = Math.sin(xangle); // getting the sin value for the pitch.

        //double yangle = Math.toRadians(i++); // note that here we do have to convert to radians.
        //double yAxisCos = Math.cos(-yangle); // getting the cos value for the yaw.
        //double yAxisSin = Math.sin(-yangle); // getting the sin value for the yaw.

        double zangle = Math.toRadians(rotationAngle++); // note that here we do have to convert to radians.
        double zAxisCos = Math.cos(zangle); // getting the cos value for the roll.
        double zAxisSin = Math.sin(zangle); // getting the sin value for the roll.

        Location rotLoc = new Location(loc.getWorld(), radius * Math.cos(frame * stepAngle), 0, radius * Math.sin(frame * stepAngle));

        //rotateAroundAxisX(rotLoc, xAxisCos, xAxisSin);
        //rotateAroundAxisY(rotLoc, yAxisCos, yAxisSin);
        rotateAroundAxisZ(rotLoc, zAxisCos, zAxisSin);

        loc.add(rotLoc);

        playParticleEffect(loc);
    }

    private void rotateAroundAxisX(Location v, double cos, double sin) {
        double y = v.getY() * cos - v.getZ() * sin;
        double z = v.getY() * sin + v.getZ() * cos;
        v.setY(y);
        v.setZ(z);
    }

    private void rotateAroundAxisY(Location v, double cos, double sin) {
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        v.setX(x);
        v.setZ(z);
    }

    private void rotateAroundAxisZ(Location v, double cos, double sin) {
        double x = v.getX() * cos - v.getY() * sin;
        double y = v.getX() * sin + v.getY() * cos;
        v.setX(x);
        v.setY(y);
    }
}