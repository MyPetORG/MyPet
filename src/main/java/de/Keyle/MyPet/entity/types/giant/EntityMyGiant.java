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

package de.Keyle.MyPet.entity.types.giant;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.movement.*;
import de.Keyle.MyPet.entity.ai.target.*;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.MyPetConfiguration;
import net.minecraft.server.v1_5_R2.EntityHuman;
import net.minecraft.server.v1_5_R2.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_5_R2.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_5_R2.World;

@EntitySize(width = 5.5f, height = 5.5F)
public class EntityMyGiant extends EntityMyPet
{
    public EntityMyGiant(World world, MyPet myPet)
    {
        super(world, myPet);
        this.texture = "/mob/zombie.png";
    }

    public void setMyPet(MyPet myPet)
    {

        if (myPet != null)
        {
            super.setMyPet(myPet);

            this.height *= 6.0F;
        }
    }

    public void setPathfinder()
    {
        petPathfinderSelector.addGoal("Float", new EntityAIFloat(this));
        petPathfinderSelector.addGoal("Ride", new EntityAIRide(this, this.walkSpeed));
        if (myPet.getRangedDamage() > 0)
        {
            petTargetSelector.addGoal("RangedTarget", new EntityAIRangedAttack(myPet, -0.1F, 35, 12.0F));
        }
        if (myPet.getDamage() > 0)
        {
            petPathfinderSelector.addGoal("MeleeAttack", new EntityAIMeleeAttack(this, 0.1F, 8, 20));
            petTargetSelector.addGoal("OwnerHurtByTarget", new EntityAIOwnerHurtByTarget(this));
            petTargetSelector.addGoal("OwnerHurtTarget", new EntityAIOwnerHurtTarget(myPet));
            petTargetSelector.addGoal("HurtByTarget", new EntityAIHurtByTarget(this));
            petTargetSelector.addGoal("ControlTarget", new EntityAIControlTarget(myPet, 1));
            petTargetSelector.addGoal("AggressiveTarget", new EntityAIAggressiveTarget(myPet, 15));
            petTargetSelector.addGoal("FarmTarget", new EntityAIFarmTarget(myPet, 15));
            petTargetSelector.addGoal("DuelTarget", new EntityAIDuelTarget(myPet, 5));
        }
        petPathfinderSelector.addGoal("Control", new EntityAIControl(myPet, 0.1F));
        petPathfinderSelector.addGoal("FollowOwner", new EntityAIFollowOwner(this, 0F, MyPetConfiguration.MYPET_FOLLOW_DISTANCE, 5.0F, 20F));
        petPathfinderSelector.addGoal("LookAtPlayer", false, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        petPathfinderSelector.addGoal("RandomLockaround", new PathfinderGoalRandomLookaround(this));
    }

    // Obfuscated Methods -------------------------------------------------------------------------------------------

    protected void a(int i, int j, int k, int l)
    {
        makeSound("mob.zombie.step", 0.15F, 1.0F);
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String bb()
    {
        return !playIdleSound() ? "" : "mob.zombie.say";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String bc()
    {
        return "mob.zombie.hurt";
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String bd()
    {
        return "mob.zombie.death";
    }
}