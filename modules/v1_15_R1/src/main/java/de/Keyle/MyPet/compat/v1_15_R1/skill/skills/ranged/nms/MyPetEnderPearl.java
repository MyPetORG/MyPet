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

package de.Keyle.MyPet.compat.v1_15_R1.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_15_R1.skill.skills.ranged.bukkit.CraftMyPetEnderPearl;
import net.minecraft.server.v1_15_R1.*;

@Compat("v1_15_R1")
public class MyPetEnderPearl extends EntityEnderPearl implements EntityMyPetProjectile {

    protected float damage = 0;
    protected CraftMyPetEnderPearl bukkitEntity = null;

    public MyPetEnderPearl(World world, EntityMyPet entityLiving) {
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
    public CraftMyPetEnderPearl getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetEnderPearl(this.world.getServer(), this);
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
        if (movingObjectPosition.getType() == MovingObjectPosition.EnumMovingObjectType.ENTITY) {
            Entity entity = ((MovingObjectPositionEntity) movingObjectPosition).getEntity();
            if (entity instanceof EntityLiving) {
                entity.damageEntity(DamageSource.projectile(this, getShooter()), damage);
            }
        }
        for (int i = 0; i < 32; ++i) {
            this.world.addParticle(Particles.PORTAL, this.locX(), this.locY() + this.random.nextDouble() * 2.0D, this.locZ(), this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
        }
        die();
    }

    public boolean damageEntity(DamageSource damagesource, float f) {
        return false;
    }
}