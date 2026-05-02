package de.Keyle.MyPet.util.animation.particle;

import de.Keyle.MyPet.util.animation.ParticleAnimation;
import org.bukkit.Location;

import java.util.function.Supplier;

public abstract class CircleAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double anglePerTick;
    protected final double height;

    public CircleAnimation(double radius, double height, Supplier<Location> location) {
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
