package de.Keyle.MyPet.util.animation.particle;

import de.Keyle.MyPet.util.animation.ParticleAnimation;
import org.bukkit.Location;

import java.util.function.Supplier;

public abstract class SpiralAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double stepY;
    protected final double stepRadius;
    protected final double height;
    protected double factor = 15;

    public SpiralAnimation(double radius, double height, Supplier<Location> location) {
        super(90, location);
        this.setFramesPerTick(3);
        this.radius = radius;
        this.height = height;
        this.stepY = height / this.length;
        this.stepRadius = 1. / this.length;
    }

    @Override
    public void tick(int frame, Location location) {
        double y = location.getY() + (frame * stepY);
        double x = location.getX() + (radius * Math.cos(frame * stepRadius * factor));
        double z = location.getZ() + (radius * Math.sin(frame * stepRadius * factor));
        playParticleEffect(new Location(location.getWorld(), x, y, z));
    }
}
