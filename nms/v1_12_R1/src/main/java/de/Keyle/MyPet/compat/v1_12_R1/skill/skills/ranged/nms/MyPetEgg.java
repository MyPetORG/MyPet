/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_12_R1.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_12_R1.skill.skills.ranged.bukkit.CraftMyPetEgg;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;

@Compat("v1_12_R1")
public class MyPetEgg extends EntityEgg implements EntityMyPetProjectile {
    protected float damage = 0;

    public MyPetEgg(World world, EntityMyPet entityLiving) {
        super(world, entityLiving);
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) this.shooter;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public CraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetEgg(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }

    @Override
    public void a(NBTTagCompound nbtTagCompound) {
    }

    @Override
    public void b(NBTTagCompound nbtTagCompound) {
    }

    @Override
    protected void a(MovingObjectPosition movingObjectPosition) {
        if (movingObjectPosition.entity != null) {
            if (movingObjectPosition.entity instanceof EntityLiving) {
                movingObjectPosition.entity.damageEntity(DamageSource.projectile(this, getShooter()), damage);
            }
        }
        for (int i = 0; i < 8; ++i) {
            this.world.addParticle(EnumParticle.ITEM_CRACK, this.locX, this.locY, this.locZ, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, Item.getId(Items.EGG));
        }
        die();
    }

    public boolean damageEntity(DamageSource damagesource, float f) {
        return false;
    }
}