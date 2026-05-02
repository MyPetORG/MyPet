package de.Keyle.MyPet.util.animation.particle;

import de.Keyle.MyPet.util.animation.ParticleAnimation;
import org.bukkit.Location;

import java.util.function.Supplier;

public abstract class FixedCircleAnimation extends ParticleAnimation {
    protected final double radius;
    protected final double height;
    private final int number;
    private final int anglePerSpot;


    public FixedCircleAnimation(double radius, double height, int number, Supplier<Location> location) {
        super(10, location);
        this.radius = radius;
        this.height = height / 2;
        this.number = number;
        this.anglePerSpot = 360 / number;
    }

    @Override
    public void tick(int frame, Location location) {
        double y = location.getY() + this.height;

        for (int i = 0; i < number; i++) {
            double x = location.getX() + (radius * Math.cos(i * anglePerSpot));
            double z = location.getZ() + (radius * Math.sin(i * anglePerSpot));
            playParticleEffect(new Location(location.getWorld(), x, y, z));
        }
    }
}
