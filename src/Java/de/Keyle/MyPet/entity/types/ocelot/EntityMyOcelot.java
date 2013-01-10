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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_6.*;
import org.bukkit.entity.Ocelot.Type;

@EntitySize(width = 0.6F, height = 0.8F)
public class EntityMyOcelot extends EntityMyPet
{
    private EntityAISit sitPathfinder;

    public EntityMyOcelot(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/ozelot.png";
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new PathfinderGoalFloat(this));
        petPathfinderSelector.addGoal("Sit", sitPathfinder);
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed + 0.15F));
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
        petPathfinderSelector.addGoal("LookAtPlayer", false, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new PathfinderGoalRandomLookaround(this));
    }

    public void setMyPet(MyPet myPet)
    {
        if (myPet != null)
        {
            this.sitPathfinder = new EntityAISit(this);

            super.setMyPet(myPet);

            this.setSitting(((MyOcelot) myPet).isSitting());
            this.setBaby(((MyOcelot) myPet).isBaby());
            this.setCatType(((MyOcelot) myPet).getCatType().getId());
        }
    }

    public boolean canMove()
    {
        return !isSitting();
    }

    public void setSitting(boolean flag)
    {
        this.sitPathfinder.setSitting(flag);
    }

    public boolean isSitting()
    {
        return this.sitPathfinder.isSitting();
    }

    public void applySitting(boolean flag)
    {
        int i = this.datawatcher.getByte(16);
        if (flag)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyOcelot) myPet).isSitting = flag;
    }

    public int getCatType()
    {
        return this.datawatcher.getByte(18);
    }

    public void setCatType(int value)
    {
        this.datawatcher.watch(18, (byte) value);
        ((MyOcelot) myPet).catType = Type.getType(value);
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
        ((MyOcelot) myPet).isBaby = flag;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(12, new Integer(0));     // age
        this.datawatcher.a(16, new Byte((byte) 0)); // tamed/sitting
        this.datawatcher.a(18, new Byte((byte) 0)); // cat type

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

        if (entityhuman == getOwner())
        {
            if (itemStack != null)
            {
                if (itemStack.id == 351)
                {
                    if (itemStack.getData() == 11)
                    {
                        ((MyOcelot) myPet).setCatType(Type.WILD_OCELOT);
                        return true;
                    }
                    else if (itemStack.getData() == 0)
                    {
                        ((MyOcelot) myPet).setCatType(Type.BLACK_CAT);
                        return true;
                    }
                    else if (itemStack.getData() == 14)
                    {
                        ((MyOcelot) myPet).setCatType(Type.RED_CAT);
                        return true;
                    }
                    else if (itemStack.getData() == 7)
                    {
                        ((MyOcelot) myPet).setCatType(Type.SIAMESE_CAT);
                        return true;
                    }
                }
            }
            this.sitPathfinder.toogleSitting();
            this.bF = false;
            return true;
        }
        return false;
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String aY()
    {
        return !playIdleSound() ? "" : this.random.nextInt(4) == 0 ? "mob.cat.purreow" : "mob.cat.meow";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    protected String aZ()
    {
        return "mob.cat.hitt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    protected String ba()
    {
        return "mob.cat.hitt";
    }
}