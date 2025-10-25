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

package de.Keyle.MyPet.compat.v1_12_R1.entity;

import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetMinecraftPart;
import net.minecraft.server.v1_12_R1.DamageSource;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;

public class EntityMyPetPart extends Entity implements MyPetMinecraftPart {
    public final EntityMyPet owner;

    public EntityMyPetPart(EntityMyPet owner) {
        super(owner.world);
        this.owner = owner;
    }

    protected void i() {
    }

    protected void a(NBTTagCompound var1) {
    }

    protected void b(NBTTagCompound var1) {
    }

    public boolean isInteractable() {
        return true;
    }

    public boolean damageEntity(DamageSource var1, float var2) {
        return false;
    }

    public boolean s(Entity var1) {
        return this == var1 || this.owner == var1;
    }

    public MyPetMinecraftEntity getPetOwner() {
        return owner;
    }

    public CraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetPart(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }
}
