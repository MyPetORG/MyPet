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

package de.Keyle.MyPet.compat.v1_20_R4.entity;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetBukkitPart;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftEntity;
import org.bukkit.entity.SpawnCategory;
import org.jetbrains.annotations.NotNull;

@Compat("v1_20_R4")
public class CraftMyPetPart extends CraftEntity implements MyPetBukkitPart {

    protected de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPetPart petEntity;

    public CraftMyPetPart(CraftServer server, de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPetPart entityMyPet) {
        super(server, entityMyPet);
        petEntity = entityMyPet;
    }

    @Override
    public MyPetBukkitEntity getPetOwner() {
        return petEntity.getPetOwner().getBukkitEntity();
    }

    @Override
    public de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPetPart getHandle() {
        return petEntity;
    }

    @Override
    public String toString() {
        return "CraftMyPetPart{Owner=" + getPetOwner() + "}";
    }

    @Override
    public boolean isInWater() {
        return getHandle().isInWater();
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void setPersistent(boolean b) {
    }

    /* This doesn't work rn as getType was made final...
    @NotNull
    @Override
    public EntityType getType() {
        return EntityType.UNKNOWN;
    }*/

    @NotNull
    @Override
    public SpawnCategory getSpawnCategory() {
        return SpawnCategory.MISC;
    }

    /* I have no clue why I need to override these other deprecated methods also don't need to be implemented so yea...*/
    @Override
    public void setVisibleByDefault(boolean b) {
    }

    @Override
    public boolean isVisibleByDefault() {
        return true;
    }
}
