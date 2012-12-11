package de.Keyle.MyPet.entity.types.creeper;

import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalControl;
import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalFollowOwner;
import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalMeleeAttack;
import de.Keyle.MyPet.entity.pathfinder.movement.PathfinderGoalRide;
import de.Keyle.MyPet.entity.pathfinder.target.*;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalOwnerHurtByTarget;
import de.Keyle.MyPet.entity.pathfinder.target.PathfinderGoalOwnerHurtTarget;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Ride;
import net.minecraft.server.*;


public class EntityMyCreeper extends EntityMyPet
{
    public EntityMyCreeper(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/creeper.png";

        petPathfinderSelector.addGoal("Float", new PathfinderGoalFloat(this));
        petPathfinderSelector.addGoal("Ride", new PathfinderGoalRide(this, this.walkSpeed + 0.15F, Ride.speedPerLevel));
        if (MyPet.getStartDamage(MyCreeper.class) > 0)
        {
            petPathfinderSelector.addGoal("LeapAtTarget", new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            petPathfinderSelector.addGoal("MeleeAttack", new PathfinderGoalMeleeAttack(this, this.walkSpeed, 3, 20));
            petTargetSelector.addGoal("OwnerHurtByTarget", new PathfinderGoalOwnerHurtByTarget(this));
            petTargetSelector.addGoal("OwnerHurtTarget", new PathfinderGoalOwnerHurtTarget(myPet));
            petTargetSelector.addGoal("HurtByTarget", new PathfinderGoalHurtByTarget(this, true));
            petTargetSelector.addGoal("ControlTarget", new PathfinderGoalControlTarget(myPet, 1));
            petTargetSelector.addGoal("AggressiveTarget", new PathfinderGoalAggressiveTarget(myPet, 15));
            petTargetSelector.addGoal("FarmTarget", new PathfinderGoalFarmTarget(myPet, 15));
        }
        petPathfinderSelector.addGoal("Control", new PathfinderGoalControl(myPet, this.walkSpeed + 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new PathfinderGoalFollowOwner(this, this.walkSpeed, 10.0F, 5.0F, 20F));
        petPathfinderSelector.addGoal("LookAtPlayer", false, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new PathfinderGoalRandomLookaround(this));
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMyCreeper(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setPowered(((MyCreeper) myPet).isPowered());
        }
    }

    public void setPowered(boolean powered)
    {
        if (!powered)
        {
            this.datawatcher.watch(17, (byte) 0);
        }
        else
        {
            this.datawatcher.watch(17, (byte) 1);
        }
        ((MyCreeper) myPet).isPowered = powered;
    }

    public boolean isPowered()
    {
        return this.datawatcher.getByte(17) == 1;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) -1)); // N/A
        this.datawatcher.a(17, new Byte((byte) 0));  // powered
    }

    @Override
    protected String aY()
    {
        return "";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.creeper.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.creeper.death";
    }
}
