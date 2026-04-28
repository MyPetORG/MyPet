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

package de.Keyle.MyPet.api.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetNaturalDrop;
import de.Keyle.MyPet.api.entity.ShopInfo;
import org.bukkit.Material;

import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.WHEAT})
public interface MyGoat extends MyPet, MyPetBaby, MyPetNaturalDrop {
    boolean isScreaming();

    void setScreaming(boolean flag);

    boolean hasLeftHorn();

    boolean hasRightHorn();

    void setLeftHorn(boolean flag);

    void setRightHorn(boolean flag);

    @Override
    default Set<Material> naturalDropMaterials() {
        return Set.of(Material.GOAT_HORN);
    }

    @Override
    default boolean isNaturalDropSuppressed() {
        return !Configuration.MyPet.Goat.CAN_DROP_HORN;
    }
}