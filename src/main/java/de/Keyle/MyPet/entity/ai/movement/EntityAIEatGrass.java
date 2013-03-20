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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import net.minecraft.server.v1_5_R2.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R2.event.CraftEventFactory;

public class EntityAIEatGrass extends PathfinderGoal
{
    private EntityLiving entityMyPet;
    private World world;
    private double chanceToEat;
    int eatTicks = 0;

    public EntityAIEatGrass(EntityMyPet entityliving, double chanceToEat)
    {
        this.entityMyPet = entityliving;
        this.chanceToEat = chanceToEat;
        this.world = entityliving.world;
    }

    public boolean a()
    {
        if (this.entityMyPet.getGoalTarget() != null && this.entityMyPet.getGoalTarget().isAlive())
        {
            return false;
        }
        else if (entityMyPet.aE().nextDouble() > chanceToEat / 100.)
        {
            return false;
        }
        int blockLocX = MathHelper.floor(this.entityMyPet.locX);
        int blockLocY = MathHelper.floor(this.entityMyPet.locY);
        int blockLocZ = MathHelper.floor(this.entityMyPet.locZ);

        return this.world.getTypeId(blockLocX, blockLocY, blockLocZ) == Block.LONG_GRASS.id && this.world.getData(blockLocX, blockLocY, blockLocZ) == 1;
    }

    public boolean b()
    {
        return this.eatTicks > 0;
    }

    public void c()
    {
        this.eatTicks = 40;
        this.world.broadcastEntityEffect(this.entityMyPet, (byte) 10);
        this.entityMyPet.getNavigation().g();
    }

    public void d()
    {
        this.eatTicks = 0;
    }

    public void e()
    {
        this.eatTicks--;
        if (this.eatTicks == 4)
        {
            int blockLocX = MathHelper.floor(this.entityMyPet.locX);
            int blockLocY = MathHelper.floor(this.entityMyPet.locY);
            int blockLocZ = MathHelper.floor(this.entityMyPet.locZ);

            if (this.world.getTypeId(blockLocX, blockLocY, blockLocZ) == Block.LONG_GRASS.id)
            {
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.entityMyPet.getBukkitEntity(), this.entityMyPet.world.getWorld().getBlockAt(blockLocX, blockLocY, blockLocZ), Material.AIR).isCancelled())
                {
                    this.world.triggerEffect(2001, blockLocX, blockLocY, blockLocZ, Block.LONG_GRASS.id + 4096);
                    this.world.setAir(blockLocX, blockLocY, blockLocZ);
                    this.entityMyPet.aK();
                }
            }
            else if (this.world.getTypeId(blockLocX, blockLocY - 1, blockLocZ) == Block.GRASS.id)
            {
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.entityMyPet.getBukkitEntity(), this.entityMyPet.world.getWorld().getBlockAt(blockLocX, blockLocY - 1, blockLocZ), Material.DIRT).isCancelled())
                {
                    this.world.triggerEffect(2001, blockLocX, blockLocY - 1, blockLocZ, Block.GRASS.id);
                    this.world.setTypeIdAndData(blockLocX, blockLocY - 1, blockLocZ, Block.DIRT.id, 0, 2);
                    this.entityMyPet.aK();
                }
            }
        }
    }
}
