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

package de.Keyle.MyPet.compat.v1_16_R3.entity;

import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetMinecraftPart;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import net.minecraft.server.v1_16_R3.*;

public class EntityMyPetPart extends Entity implements MyPetMinecraftPart {

    public final EntityMyPet owner;
    private final EntitySize size;
    private final String part;
    protected CraftMyPetPart bukkitEntity = null;

    public EntityMyPetPart(EntityMyPet owner, String part, float width, float height) {
        super(EntityTypes.ENDER_DRAGON, owner.world);
        ReflectionUtil.setFieldValue("bukkitEntity", this, new CraftMyPetPart(this.world.getServer(), this));
        this.owner = owner;
        this.part = part;
        this.size = EntitySize.b(width, height);
    }

    @Override
    protected void initDatawatcher() {
    }


    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    protected void loadData(NBTTagCompound nbtTagCompound) {

    }

    @Override
    protected void saveData(NBTTagCompound nbtTagCompound) {

    }

    @Override
    public boolean damageEntity(DamageSource var1, float var2) {
        return false;
    }

    @Override
    public boolean s(Entity var1) {
        return this == var1 || this.owner == var1;
    }

    @Override
    public Packet<?> P() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntitySize a(EntityPose entitypose) {
        return this.size;
    }

    @Override
    public MyPetMinecraftEntity getPetOwner() {
        return owner;
    }

    @Override
    public CraftMyPetPart getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetPart(this.world.getServer(), this);
        }
        return this.bukkitEntity;
    }
}
