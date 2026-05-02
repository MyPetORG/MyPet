package de.Keyle.MyPet.util.animation;

import org.bukkit.Location;

import java.util.function.Supplier;

public abstract class ParticleAnimation extends Animation {

    public ParticleAnimation(int length, Supplier<Location> location) {
        super(length, location);
    }

    protected abstract void playParticleEffect(Location location);
}
