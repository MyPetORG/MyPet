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

package de.Keyle.MyPet.compat.v1_11_R1.entity;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetBukkitPart;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;

@Compat("v1_11_R1")
public class CraftMyPetPart extends CraftEntity implements MyPetBukkitPart {
    protected MyPetPlayer petOwner;
    protected EntityMyPetPart petEntity;

    public CraftMyPetPart(CraftServer server, EntityMyPetPart entityMyPet) {
        super(server, entityMyPet);
        petEntity = entityMyPet;
    }

    @Override
    public MyPetBukkitEntity getPetOwner() {
        return petEntity.getPetOwner().getBukkitEntity();
    }

    @Override
    public EntityMyPetPart getHandle() {
        return petEntity;
    }

    @Override
    public String toString() {
        return "CraftMyPetPart{Owner=" + getPetOwner() + "}";
    }

    @Override
    public EntityType getType() {
        return EntityType.UNKNOWN;
    }
}