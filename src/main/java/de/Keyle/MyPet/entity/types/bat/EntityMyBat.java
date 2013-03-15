/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.bat;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_R1.EntityHuman;
import net.minecraft.server.v1_4_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_4_R1.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_4_R1.World;


@EntitySize(width = 0.5F, height = 0.9F)
public class EntityMyBat extends EntityMyPet
{
    public EntityMyBat(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/bat.png";
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new EntityAIFloat(this));
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed));
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, this.walkSpeed, 3, 20));
            petTargetSelector.addGoal("OwnerHurtByTarget", new EntityAIOwnerHurtByTarget(this));
            petTargetSelector.addGoal("OwnerHurtTarget", new EntityAIOwnerHurtTarget(myPet));
            petTargetSelector.addGoal("HurtByTarget", new EntityAIHurtByTarget(this));
            petTargetSelector.addGoal("ControlTarget", new EntityAIControlTarget(myPet, 1));
            petTargetSelector.addGoal("AggressiveTarget", new EntityAIAggressiveTarget(myPet, 15));
            petTargetSelector.addGoal("FarmTarget", new EntityAIFarmTarget(myPet, 15));
            petTargetSelector.addGoal("DuelTarget", new EntityAIDuelTarget(myPet,5));
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

            this.setHanging(((MyBat) myPet).ishanging());
        }
    }

    public void setHanging(boolean flags)
    {
        int i = this.datawatcher.getByte(16);
        if (flags)
        {
            this.datawatcher.watch(16, (byte) (i | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (i & 0xFFFFFFFE));
        }
        ((MyBat) myPet).hanging = flags;
    }

    public boolean isHanging()
    {
        return (this.datawatcher.getByte(16) & 0x1) != 0;
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // hanging
    }

    /**
     * Returns the speed of played sounds
     */
    protected float aV()
    {
        return super.aV() * 0.95F;
    }

    @Override
    protected String aY()
    {
        return !playIdleSound() ? "" : "mob.bat.idle";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String aZ()
    {
        return "mob.bat.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.bat.death";
    }

    public void c()
    {
        super.c();

        if (!this.onGround && this.motY < 0.0D)
        {
            this.motY *= 0.6D;
        }
    }

    public void j_()
    {
        super.j_();
        if (!world.getMaterial((int) locX, (int) locY, (int) locZ).isLiquid() && !world.getMaterial((int) locX, (int) (locY+1.), (int) locZ).isSolid())
        {
            this.locY += 0.65;
        }
    }
}