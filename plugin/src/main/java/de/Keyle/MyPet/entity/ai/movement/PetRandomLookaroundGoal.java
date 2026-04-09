package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} replacement for vanilla's
 * {@code RandomLookAroundGoal} — makes an idle pet glance around in a
 * random horizontal direction for natural head motion when no nearby
 * player has claimed its attention.
 *
 * <p>Activates on a ~2% per-tick roll when the pet has no combat target
 * and is carrying no passengers. Once triggered, a random horizontal unit
 * vector is picked and the pet's head holds that heading for 20–39 ticks.
 *
 * <p>This is the default fallback for the {@link GoalType#LOOK} bucket;
 * {@link PetLookAtPlayerGoal} preempts it whenever a real player is
 * available to look at.
 */
public class PetRandomLookaroundGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private double directionX;
    private double directionZ;
    private int ticksUntilStopLooking;

    /**
     * @param petEntity the pet whose head will glance around
     */
    public PetRandomLookaroundGoal(MyPetBukkitEntity petEntity) {
        this.petEntity = petEntity;
    }

    @Override
    public boolean shouldActivate() {
        if (petEntity.hasTarget() && !petEntity.getMyPetTarget().isDead()) {
            return false;
        }
        if (!petEntity.getPassengers().isEmpty()) {
            return false;
        }
        return ThreadLocalRandom.current().nextFloat() < 0.02F;
    }

    @Override
    public boolean shouldStayActive() {
        return this.ticksUntilStopLooking > 0 && petEntity.getPassengers().isEmpty();
    }

    @Override
    public void start() {
        double angle = Math.PI * 2.0 * ThreadLocalRandom.current().nextDouble();
        this.directionX = Math.cos(angle);
        this.directionZ = Math.sin(angle);
        this.ticksUntilStopLooking = 20 + ThreadLocalRandom.current().nextInt(20);
    }

    @Override
    public void tick() {
        Location loc = petEntity.getLocation();
        Mob mob = (Mob) petEntity;
        mob.lookAt(
                loc.getX() + this.directionX,
                loc.getY() + petEntity.getEyeHeight(),
                loc.getZ() + this.directionZ,
                mob.getHeadRotationSpeed(),
                mob.getMaxHeadPitch()
        );
        this.ticksUntilStopLooking--;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.RANDOM_LOOKAROUND;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
    }
}
