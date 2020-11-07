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

package de.Keyle.MyPet.compat.v1_16_R2.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_16_R2.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_16_R2.skill.skills.ranged.bukkit.CraftMyPetLlamaSpit;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_16_R2.*;
import org.bukkit.entity.LivingEntity;

@Compat("v1_16_R2")
public class MyPetLlamaSpit extends EntityLlamaSpit implements EntityMyPetProjectile {

    @Setter @Getter protected float damage = 0;
    protected CraftMyPetLlamaSpit bukkitEntity = null;

    public MyPetLlamaSpit(World world, EntityMyPet entityMyPet) {
        super(EntityTypes.LLAMA_SPIT, world);
        this.setShooter(entityMyPet);
        this.setPosition(entityMyPet.locX() - (double) (entityMyPet.getWidth() + 1.0F) * 0.5D * (double) MathHelper.sin(entityMyPet.aE * 0.017453292F),
                entityMyPet.locY() + (double) entityMyPet.getHeadHeight() - 0.10000000149011612D,
                entityMyPet.locZ() + (double) (entityMyPet.getWidth() + 1.0F) * 0.5D * (double) MathHelper.cos(entityMyPet.aE * 0.017453292F));
    }

    @Override
    public EntityMyPet getShooter() {
                return (EntityMyPet) super.getShooter();
    }

    @Override
    public CraftMyPetLlamaSpit getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetLlamaSpit(this.world.getServer(), this);
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
    public boolean damageEntity(DamageSource damagesource, float f) {
        return false;
    }

    @Override
    public void a(MovingObjectPosition movingObjectPosition) {
        if (movingObjectPosition.getType() == MovingObjectPosition.EnumMovingObjectType.ENTITY) {
            Entity entity = ((MovingObjectPositionEntity) movingObjectPosition).getEntity();
            if (entity instanceof EntityLiving) {
                entity.damageEntity(DamageSource.a(this, getShooter()), damage);
            }
        }
        this.die();
    }
}