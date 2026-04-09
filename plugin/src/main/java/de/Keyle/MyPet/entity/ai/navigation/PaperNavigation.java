package de.Keyle.MyPet.entity.ai.navigation;

import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.navigation.NavigationParameters;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Version-independent {@link AbstractNavigation} that drives a pet's
 * movement through Paper's {@link com.destroystokyo.paper.entity.Pathfinder}
 * API.
 *
 * <p>This class only manages the navigation <em>parameters</em> (speed,
 * float flag, speed modifiers). The actual path-progression tick is
 * driven from {@code EntityMyPet.doMyPetTick()} because {@code EntityMyPet}
 * overrides {@code aiStep()} and never calls {@code Mob.serverAiStep()};
 * see {@link #tick()} below.
 */
public class PaperNavigation extends AbstractNavigation {

    private final Mob mob;

    /**
     * Creates a navigation wrapper using the default {@link NavigationParameters}.
     *
     * @param entityMyPet the entity whose Bukkit handle is driven through Paper's Pathfinder
     */
    public PaperNavigation(MyPetMinecraftEntity entityMyPet) {
        super(entityMyPet);
        this.mob = entityMyPet.getBukkitEntity();
        // Re-apply MOVEMENT_SPEED whenever a speed modifier is added/removed,
        // so PetSprintGoal.stop() (and any other goal that touches modifiers
        // without calling nav.stop()/navigateTo()) actually takes effect.
        parameters.setOnSpeedChange(this::applyNavigationParameters);
    }

    /**
     * Creates a navigation wrapper using a caller-supplied parameter set.
     *
     * @param entityMyPet the entity whose Bukkit handle is driven through Paper's Pathfinder
     * @param parameters  pre-configured navigation parameters (speed, float, modifiers)
     */
    public PaperNavigation(MyPetMinecraftEntity entityMyPet, NavigationParameters parameters) {
        super(entityMyPet, parameters);
        this.mob = entityMyPet.getBukkitEntity();
        parameters.setOnSpeedChange(this::applyNavigationParameters);
    }

    /**
     * Halts any in-progress pathfinding and re-applies the current navigation
     * parameters so speed-modifier changes take effect immediately.
     */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
        // applyNavigationParameters() collapses all speed modifiers into the
        // MOVEMENT_SPEED attribute's base value. When a goal's stop() removes
        // a modifier (Sprint, Control, MeleeAttack, RangedAttack, ...) the
        // map change alone has no effect on the entity until the parameters
        // are re-applied. Do that here so goals that call nav.stop() in their
        // stop() path get the correct speed on the next tick; goals that
        // don't call nav.stop() will re-apply via their next navigateTo().
        applyNavigationParameters();
    }

    /**
     * Asks Paper's pathfinder to move the pet toward the given world coordinates.
     * Re-applies navigation parameters on success so the pathfinder sees any
     * freshly-added speed modifiers for this movement.
     *
     * @return {@code true} if a path was accepted, {@code false} otherwise
     */
    @Override
    public boolean navigateTo(double x, double y, double z) {
        if (mob.getPathfinder().moveTo(new Location(mob.getWorld(), x, y, z))) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    /**
     * Asks Paper's pathfinder to move the pet toward a target living entity.
     * The pathfinder continuously updates its destination as the entity moves.
     *
     * @return {@code true} if a path was accepted, {@code false} otherwise
     */
    @Override
    public boolean navigateTo(LivingEntity entity) {
        if (mob.getPathfinder().moveTo(entity)) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    /**
     * No-op: the real path step is driven from {@code EntityMyPet.doMyPetTick()}
     * because {@code EntityMyPet} overrides {@code aiStep()} and never calls
     * {@code Mob.serverAiStep()}. See the class-level Javadoc.
     */
    @Override
    public void tick() {
        // No-op. See class Javadoc.
    }

    /**
     * Writes the current navigation parameter state onto the underlying Bukkit
     * {@link Mob}: collapses all speed modifiers into the {@code MOVEMENT_SPEED}
     * attribute and toggles the pathfinder's {@code canFloat} flag.
     *
     * <p>Invoked automatically on every {@link #stop()}, every successful
     * {@link #navigateTo(double, double, double)}/{@link #navigateTo(LivingEntity)},
     * and every speed-modifier change — so callers never need to invoke it
     * directly.
     */
    @Override
    public void applyNavigationParameters() {
        // Paper's Pathfinder.setCanFloat(true) means "allowed to swim/float on
        // water surfaces while pathfinding". A pet that avoids water should
        // NOT float (it shouldn't be in water to begin with), and a pet that
        // tolerates water SHOULD float. Hence the negation of avoidWater().
        mob.getPathfinder().setCanFloat(!parameters.avoidWater());
        mob.getAttribute(Attribute.MOVEMENT_SPEED)
                .setBaseValue(parameters.speed() + parameters.speedModifier());
    }
}
