/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types.enderman;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.EntityAIControl;
import de.Keyle.MyPet.entity.ai.movement.EntityAIFollowOwner;
import de.Keyle.MyPet.entity.ai.movement.EntityAIMeleeAttack;
import de.Keyle.MyPet.entity.ai.movement.EntityAIRide;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_R1.*;


@EntitySize(width = 0.6F, height = 2.9F)
public class EntityMyEnderman extends EntityMyPet
{
    public EntityMyEnderman(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/enderman.png";
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new PathfinderGoalFloat(this));
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed + 0.15F));
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, this.walkSpeed, 3, 20));
            petTargetSelector.addGoal("OwnerHurtByTarget", new EntityAIOwnerHurtByTarget(this));
            petTargetSelector.addGoal("OwnerHurtTarget", new EntityAIOwnerHurtTarget(myPet));
            petTargetSelector.addGoal("HurtByTarget", new EntityAIHurtByTarget(this));
            petTargetSelector.addGoal("ControlTarget", new EntityAIControlTarget(myPet, 1));
            petTargetSelector.addGoal("AggressiveTarget", new EntityAIAggressiveTarget(myPet, 15));
            petTargetSelector.addGoal("FarmTarget", new EntityAIFarmTarget(myPet, 15));
        }
        petPathfinderSelector.addGoal("Control", new EntityAIControl(myPet, this.walkSpeed + 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new EntityAIFollowOwner(this, this.walkSpeed, 10.0F, 5.0F, 20F));
        petPathfinderSelector.addGoal("LookAtPlayer", false, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new PathfinderGoalRandomLookaround(this));
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.setScreaming(((MyEnderman) myPet).isScreaming());
            this.setBlock(((MyEnderman) myPet).getBlockID(), ((MyEnderman) myPet).getBlockData());
        }
    }

    public int getBlockID()
    {
        return ((MyEnderman) myPet).BlockID;
    }

    public int getBlockData()
    {
        return ((MyEnderman) myPet).BlockData;
    }

    public void setBlock(int blockID, int blockData)
    {
        this.datawatcher.watch(16, (byte) (blockID & 0xFF));
        ((MyEnderman) myPet).BlockID = blockID;

        this.datawatcher.watch(17, (byte) (blockData & 0xFF));
        ((MyEnderman) myPet).BlockData = blockData;
    }

    public boolean isScreaming()
    {
        return this.datawatcher.getByte(18) == 1;
    }

    public void setScreaming(boolean screaming)
    {
        this.datawatcher.watch(18, (byte) (screaming ? 1 : 0));
        ((MyEnderman) myPet).isScreaming = screaming;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0));  // BlockID
        this.datawatcher.a(17, new Byte((byte) 0));  // BlockData
        this.datawatcher.a(18, new Byte((byte) 0));  // Face(angry)
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
            if (itemStack.id == Item.SHEARS.id)
            {
                if (getBlockID() != 0)
                {
                    EntityItem entityitem = this.a(new ItemStack(getBlockID(), 1, getBlockData()), 1.0F);
                    entityitem.motY += (double) (this.random.nextFloat() * 0.05F);
                    entityitem.motX += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);
                    entityitem.motZ += (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F);

                    setBlock(0, 0);

                    return true;
                }
            }
            else if (getBlockID() <= 0 && itemStack.id > 0 && itemStack.id < 256)
            {
                setBlock(itemStack.id, itemStack.getData());
                if (!entityhuman.abilities.canInstantlyBuild)
                {
                    --itemStack.count;
                }
                if (itemStack.count <= 0)
                {
                    entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, null);
                }
            }
        }
        return false;
    }

    @Override
    protected String aY()
    {
        return !playIdleSound() ? "" : isScreaming() ? "mob.endermen.scream" : "mob.endermen.idle";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.endermen.hit";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.endermen.death";
    }
}