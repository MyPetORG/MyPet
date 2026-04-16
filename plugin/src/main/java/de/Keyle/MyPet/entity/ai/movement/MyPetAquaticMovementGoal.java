package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper Goal that supplements the default ground MoveControl with Y-axis velocity
 * and body rotation for aquatic pets in water.
 *
 * <p>The default MoveControl handles X/Z movement via pathfinder waypoints but cannot
 * produce Y-axis velocity underwater. This goal fills that gap by:
 * <ul>
 *   <li>Applying direct Y velocity toward the navigation target when swimming</li>
 *   <li>Syncing body rotation to face the swimming direction</li>
 *   <li>Applying gentle sinking when idle in water</li>
 * </ul>
 *
 * <p>On land, this goal does nothing — the default MoveControl handles everything.
 * Uses {@code GoalType.UNKNOWN_BEHAVIOR} so it doesn't conflict with MOVE goals.
 */
public class MyPetAquaticMovementGoal implements Goal<Mob> {

    private static final double IDLE_SINK_VELOCITY = -0.005D;
    private static final double Y_FORCE_MULTIPLIER = 0.15D;
    private static final double MIN_Y_FORCE = 0.03D;
    private static final double RAD_TO_DEG = 57.2957763671875D;

    private final MyPet pet;
    private final Mob mob;

    public MyPetAquaticMovementGoal(MyPet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return true;
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        if (!isInWaterOrBubble()) {
            return; // On land: default MoveControl handles everything
        }

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return;

        Location petLoc = mob.getLocation();
        Location ownerLoc = owner.getLocation();
        float speed = (float) mob.getAttribute(Attribute.MOVEMENT_SPEED).getValue();

        boolean hasPath = mob.getPathfinder().hasPath();
        boolean hasTarget = pet.hasTarget() && pet.getMyPetTarget() != null;

        if (hasPath || hasTarget) {
            // Active swimming: apply Y velocity toward target
            Location targetLoc = hasTarget ? pet.getMyPetTarget().getLocation() : ownerLoc;
            double dy = targetLoc.getY() - petLoc.getY();
            double dx = targetLoc.getX() - petLoc.getX();
            double dz = targetLoc.getZ() - petLoc.getZ();
            double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (totalDist > 0.5) {
                // Y-axis velocity proportional to the Y component of direction
                double yRatio = dy / totalDist;
                double yVelocity = speed * yRatio * Y_FORCE_MULTIPLIER;

                // Ensure minimum velocity magnitude when Y difference is significant
                if (Math.abs(dy) > 1.0 && Math.abs(yVelocity) < MIN_Y_FORCE) {
                    yVelocity = dy > 0 ? MIN_Y_FORCE : -MIN_Y_FORCE;
                }

                // Direct-set Y velocity (do NOT add to existing). Adding caused
                // unbounded Y-axis accumulation over multiple ticks — water
                // friction (~0.8) wasn't strong enough to dampen the accumulated
                // value at the intended magnitude, producing vertical overshoot
                // and oscillation around the target depth. The idle branch
                // below correctly uses the same direct-set pattern.
                Vector vel = mob.getVelocity();
                mob.setVelocity(new Vector(vel.getX(), yVelocity, vel.getZ()));
            }

            // Body rotation to face swimming direction
            if (dx != 0.0D || dz != 0.0D) {
                float targetYaw = (float) (Math.atan2(dz, dx) * RAD_TO_DEG) - 90.0F;
                float newYaw = rotlerp(petLoc.getYaw(), targetYaw, 90.0F);
                mob.setRotation(newYaw, petLoc.getPitch());
                mob.setBodyYaw(newYaw);
            }
        } else {
            // Idle in water: gentle sinking
            Vector vel = mob.getVelocity();
            mob.setVelocity(new Vector(vel.getX(), IDLE_SINK_VELOCITY, vel.getZ()));
        }
    }

    private boolean isInWaterOrBubble() {
        return mob.isInWater() || mob.isInBubbleColumn();
    }

    private static float rotlerp(float current, float target, float maxDelta) {
        float delta = wrapDegrees(target - current);
        if (delta > maxDelta) delta = maxDelta;
        if (delta < -maxDelta) delta = -maxDelta;
        return current + delta;
    }

    private static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.AQUATIC_MOVEMENT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }
}
