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
