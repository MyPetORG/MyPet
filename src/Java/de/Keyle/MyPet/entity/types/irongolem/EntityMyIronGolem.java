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

package de.Keyle.MyPet.entity.types.irongolem;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.EntityAIControl;
import de.Keyle.MyPet.entity.ai.movement.EntityAIFollowOwner;
import de.Keyle.MyPet.entity.ai.movement.EntityAIMeleeAttack;
import de.Keyle.MyPet.entity.ai.movement.EntityAIRide;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_4_R1.*;

@EntitySize(width = 1.4F, height = 2.9F)
public class EntityMyIronGolem extends EntityMyPet
{
    public static boolean CAN_THROW_UP = true;
    public EntityMyIronGolem(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/villager_golem.png";
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed + 0.15F));
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, this.walkSpeed, 5, 20));
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

    protected void setPlayerCreated(boolean flag)
    {
        byte b0 = this.datawatcher.getByte(16);

        if (flag)
        {
            this.datawatcher.watch(16, (byte) (b0 | 0x1));
        }
        else
        {
            this.datawatcher.watch(16, (byte) (b0 & 0xFFFFFFFE));
        }
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a()
    {
        super.a();
        this.datawatcher.a(16, new Byte((byte) 0)); // flower???
    }

    @Override
    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.irongolem.walk", 1.0F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
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
        return "mob.irongolem.hit";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String ba()
    {
        return "mob.irongolem.death";
    }

    public boolean m(Entity entity)
    {
        this.world.broadcastEntityEffect(this, (byte) 4);
        boolean flag = super.m(entity);
        if (CAN_THROW_UP && flag)
        {
            entity.motY += 0.4000000059604645D;
            this.world.makeSound(this, "mob.irongolem.throw", 1.0F, 1.0F);
        }
        return flag;
    }
}