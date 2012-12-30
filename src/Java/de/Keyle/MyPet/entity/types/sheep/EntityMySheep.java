/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.sheep;

import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Ride;
import net.minecraft.server.v1_4_5.*;

public class EntityMySheep extends EntityMyPet
{
    public EntityMySheep(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/sheep.png";
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new PathfinderGoalFloat(this));
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed + 0.15F, Ride.speedPerLevel));
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.addGoal("LeapAtTarget", new PathfinderGoalLeapAtTarget(this, this.walkSpeed + 0.1F));
            petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, this.walkSpeed, 3, 20));
            petTargetSelector.addGoal("OwnerHurtByTarget", new EntityAIOwnerHurtByTarget(this));
            petTargetSelector.addGoal("OwnerHurtTarget", new EntityAIOwnerHurtTarget(myPet));
            petTargetSelector.addGoal("HurtByTarget", new EntityAIHurtByTarget(this, true));
            petTargetSelector.addGoal("ControlTarget", new EntityAIControlTarget(myPet, 1));
            petTargetSelector.addGoal("AggressiveTarget", new EntityAIAggressiveTarget(myPet, 15));
            petTargetSelector.addGoal("FarmTarget", new EntityAIFarmTarget(myPet, 15));
        }
        petPathfinderSelector.addGoal("Control", new EntityAIControl(myPet, this.walkSpeed + 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new EntityAIFollowOwner(this, this.walkSpeed, 10.0F, 5.0F, 20F));
        petPathfinderSelector.addGoal("EatGrass", new EntityAIEatGrass(this, 0.02));
        petPathfinderSelector.addGoal("LookAtPlayer", false, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new PathfinderGoalRandomLookaround(this));
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setColor(((MySheep) myPet).getColor());
            this.setSheared(((MySheep) myPet).isSheared());
            this.setBaby(((MySheep) myPet).isBaby());
        }
    }

    public int getColor()
    {
        return this.datawatcher.getByte(16) & 15;
    }

    public void setColor(int value)
    {
        byte b0 = this.datawatcher.getByte(16);
        this.datawatcher.watch(16, (byte) (b0 & 240 | value & 15));
        ((MySheep) myPet).color = value;
    }

    public boolean isSheared()
    {
        return (this.datawatcher.getByte(16) & 16) != 0;
    }

    public void setSheared(boolean flag)
    {

        byte b0 = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (b0 | 16));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & -17));
        }
        ((MySheep) myPet).isSheared = flag;
    }

    public boolean isBaby()
    {
        return this.datawatcher.getInt(12) < 0;
    }

    @SuppressWarnings("boxing")
    public void setBaby(boolean flag)
    {
        if (flag)
        {
            this.datawatcher.watch(12, new Integer(-24000));
        }
        else
        {
            this.datawatcher.watch(12, new Integer(0));
        }
        ((MySheep) myPet).isBaby = flag;
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftMySheep(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // color/sheared
        this.datawatcher.a(12, new Integer(0));     // age
    }

    /**
     * Is called when player rightclicks this MyPet
     * return:
     * true: there was a reaction on rightclick
     * false: no reaction on rightclick
     */
    public boolean a(EntityHuman entityhuman)
    {
        if (super.a(entityhuman))
        {
            return true;
        }

        ItemStack itemStack = entityhuman.inventory.getItemInHand();

        if (entityhuman == getOwner() && itemStack != null)
        {
            if (itemStack.id == 351)
            {
                if (itemStack.getData() <= 15)
                {
                    ((MySheep) myPet).setColor(15 - itemStack.getData());
                    return true;

                }
            }
            else if (itemStack.id == Item.SHEARS.id && !((MySheep) myPet).isSheared())
            {
                if (!this.world.isStatic)
                {
                    ((MySheep) myPet).setSheared(true);
                    int i = 1 + this.random.nextInt(3);

                    for (int j = 0 ; j < i ; ++j)
                    {
                        EntityItem entityitem = this.a(new ItemStack(Block.WOOL.id, 1, ((MySheep) myPet).getColor()), 1.0F);

                        entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                        entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                        entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    }
                    makeSound("mob.sheep.shear", 1.0F, 1.0F);
                }
                itemStack.damage(1, entityhuman);
            }
        }
        return false;
    }

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.sheep.step", 0.15F, 1.0F);
    }

    /**
     * Called when the sheeps eat grass
     */
    public void aH()
    {
        ((MySheep) myPet).setSheared(false);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.sheep.say";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.sheep.say";
    }
}