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

package de.Keyle.MyPet.skill.skills.implementation.ranged;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import net.minecraft.server.v1_6_R2.*;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftSnowball;

public class MyPetSnowball extends EntitySnowball implements MyPetProjectile
{
    protected int damage = 0;

    public MyPetSnowball(World world, EntityMyPet entityLiving)
    {
        super(world, entityLiving);
    }

    @Override
    public EntityMyPet getShooter()
    {
        return (EntityMyPet) this.shooter;
    }

    public void setDamage(int damage)
    {
        this.damage = damage;
    }

    @Override
    public CraftEntity getBukkitEntity()
    {
        if (this.bukkitEntity == null)
        {
            this.bukkitEntity = new CraftSnowball(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    @Override
    public void a(NBTTagCompound nbtTagCompound)
    {
    }

    @Override
    public void b(NBTTagCompound nbtTagCompound)
    {
    }

    @Override
    protected void a(MovingObjectPosition paramMovingObjectPosition)
    {
        if (paramMovingObjectPosition.entity != null)
        {
            paramMovingObjectPosition.entity.damageEntity(DamageSource.projectile(this, getShooter()), damage);
        }
        for (int i = 0 ; i < 8 ; i++)
        {
            this.world.addParticle("snowballpoof", this.locX, this.locY, this.locZ, 0.0D, 0.0D, 0.0D);
        }
        die();
    }
}