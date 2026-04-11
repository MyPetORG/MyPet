package de.Keyle.MyPet.entity.ai.navigation;

import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Version-independent {@link AbstractNavigation} that drives a pet's
 * movement through Paper's {@link com.destroystokyo.paper.entity.Pathfinder}
 * API. Wraps a real vanilla Bukkit {@link Mob}.
 */
public class PaperNavigation extends AbstractNavigation {

    private final Mob mob;

    public PaperNavigation(Mob mob, double walkSpeed) {
        super(walkSpeed);
        this.mob = mob;
        parameters.setOnSpeedChange(this::applyNavigationParameters);
    }

    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
        applyNavigationParameters();
    }

    @Override
    public boolean navigateTo(double x, double y, double z) {
        if (mob.getPathfinder().moveTo(new Location(mob.getWorld(), x, y, z))) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    @Override
    public boolean navigateTo(LivingEntity entity) {
        if (mob.getPathfinder().moveTo(entity)) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        // No-op. Paper's Pathfinder advances automatically per tick.
    }

    @Override
    public void applyNavigationParameters() {
        mob.getPathfinder().setCanFloat(!parameters.avoidWater());
        var speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(parameters.speed() + parameters.speedModifier());
        }
    }
}
