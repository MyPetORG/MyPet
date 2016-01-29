/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.entity.types.enderdragon;

import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import org.bukkit.ChatColor;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.Impossible;
import static org.bukkit.Material.ENDER_STONE;

@MyPetInfo(food = {ENDER_STONE}, leashFlags = {Impossible})
public class MyEnderDragon extends MyPet {
    public MyEnderDragon(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.EnderDragon;
    }

    @Override
    public String toString() {
        return "MyEnderDragon{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + "}";
    }
}