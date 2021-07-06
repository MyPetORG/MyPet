/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_17_R1.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_17_R1.skill.skills.ranged.bukkit.CraftMyPetWitherSkull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.EntityWitherSkull;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.royawesome.jlibnoise.MathHelper;

@Compat("v1_17_R1")
public class MyPetWitherSkull extends EntityWitherSkull implements EntityMyPetProjectile {

    protected float damage = 0;
    protected int deathCounter = 100;
    protected CraftMyPetWitherSkull bukkitEntity = null;

    public MyPetWitherSkull(World world, EntityMyPet entityliving, double d0, double d1, double d2) {
        super(world, entityliving, d0, d1, d2);
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getShooter();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void setDirection(double d0, double d1, double d2) {
        d0 += this.Q.nextGaussian() * 0.2D;
        d1 += this.Q.nextGaussian() * 0.2D;
        d2 += this.Q.nextGaussian() * 0.2D;
        double d3 = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        this.b = (d0 / d3 * 0.1D);
        this.c = (d1 / d3 * 0.1D);
        this.d = (d2 / d3 * 0.1D);
    }

    @Override
    public CraftMyPetWitherSkull getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetWitherSkull(this.t.getCraftServer(), this);
        }
        return this.bukkitEntity;
    }

    @Override
    public void saveData(NBTTagCompound nbtTagCompound) {
    }

    @Override
    public void loadData(NBTTagCompound nbtTagCompound) {
    }

    @Override
    protected void a(MovingObjectPosition movingObjectPosition) {
        if (movingObjectPosition.getType() == MovingObjectPosition.EnumMovingObjectType.c) {
            Entity entity = ((MovingObjectPositionEntity) movingObjectPosition).getEntity();
            if (entity instanceof EntityLiving) {
                entity.damageEntity(DamageSource.projectile(this, getShooter()), damage);
            }
        }
        die();
    }

    @Override
    public void tick() {
        try {
            super.tick();
            if (deathCounter-- <= 0) {
                die();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
