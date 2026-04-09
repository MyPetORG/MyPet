package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.types.MySheep;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} that makes a sheared sheep pet stop, eat grass, and
 * regrow its wool — the Paper-goal equivalent of vanilla's
 * {@code EatBlockGoal}.
 *
 * <p>Activates only when <em>all</em> of the following are true:
 * <ul>
 *   <li>{@link de.Keyle.MyPet.api.Configuration.MyPet.Sheep#CAN_REGROW_WOOL}
 *       is enabled in config.yml.</li>
 *   <li>The underlying {@link MySheep} is currently sheared.</li>
 *   <li>A random 1-in-1000 roll succeeds (matches vanilla frequency).</li>
 *   <li>The pet has no active target.</li>
 *   <li>The block at the pet's feet is {@link Material#SHORT_GRASS} or the
 *       block below is {@link Material#GRASS_BLOCK}.</li>
 * </ul>
 *
 * <p>While active the goal halts navigation, broadcasts entity event byte
 * {@code 10} to play the sheep head-bob eat animation on clients, and after
 * a 30-tick wind-up consumes the grass (subject to the {@link GameRule#MOB_GRIEFING}
 * game rule) and resets the sheared flag so the pet re-wears its wool.
 *
 * <p>Declares {@link GoalType#MOVE} and {@link GoalType#LOOK} because during
 * the animation the pet must neither wander nor glance around.
 */
public class PetEatGrassGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private int eatTicks = 0;

    /**
     * @param petEntity the sheep pet that should eat grass when sheared
     */
    public PetEatGrassGoal(MyPetBukkitEntity petEntity) {
        this.petEntity = petEntity;
    }

    @Override
    public boolean shouldActivate() {
        if (!Configuration.MyPet.Sheep.CAN_REGROW_WOOL) {
            return false;
        }
        MySheep mySheep = (MySheep) petEntity.getMyPet();
        if (!mySheep.isSheared()) {
            return false;
        }
        if (ThreadLocalRandom.current().nextInt(1000) != 0) {
            return false;
        }
        if (petEntity.hasTarget() && !petEntity.getMyPetTarget().isDead()) {
            return false;
        }
        Location loc = petEntity.getLocation();
        Block blockAt = loc.getBlock();
        Block blockBelow = blockAt.getRelative(BlockFace.DOWN);
        return blockAt.getType() == Material.SHORT_GRASS || blockBelow.getType() == Material.GRASS_BLOCK;
    }

    @Override
    public boolean shouldStayActive() {
        return this.eatTicks > 0;
    }

    @Override
    public void start() {
        this.eatTicks = 30;
        petEntity.getHandle().getPetNavigation().stop();
        // Entity event 10 triggers the sheep head-bob eat animation on the client
        petEntity.getHandle().broadcastEntityEvent((byte) 10);
    }

    @Override
    public void stop() {
        this.eatTicks = 0;
    }

    @Override
    public void tick() {
        if (--this.eatTicks == 0) {
            Location loc = petEntity.getLocation();
            World world = loc.getWorld();
            Block blockAt = loc.getBlock();
            Block blockBelow = blockAt.getRelative(BlockFace.DOWN);

            Boolean mobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
            if (mobGriefing != null && !mobGriefing) {
                return;
            }

            if (blockAt.getType() == Material.SHORT_GRASS) {
                blockAt.setType(Material.AIR);
                ((MySheep) petEntity.getMyPet()).setSheared(false);
            } else if (blockBelow.getType() == Material.GRASS_BLOCK) {
                blockBelow.setType(Material.DIRT);
                ((MySheep) petEntity.getMyPet()).setSheared(false);
            }
        }
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.EAT_GRASS;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
