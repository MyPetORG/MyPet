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

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Material;
import org.bukkit.entity.Villager;

import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.APPLE})
public interface MyVillager extends MyPet, MyPetBaby, MyPetEquipment {
    @Override
    default Set<String> getAllowedSlotNames() {
        return Set.of("HAND");
    }

    int getProfession();

    void setProfession(int value);

    Villager.Type getType();

    void setType(Villager.Type value);

    int getLevel();

    void setLevel(int level);

    CompoundBinaryTag getOriginalData();

    void setOriginalData(CompoundBinaryTag compound);

    boolean hasOriginalData();

}