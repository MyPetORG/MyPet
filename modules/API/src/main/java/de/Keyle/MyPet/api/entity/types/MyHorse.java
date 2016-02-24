/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.entity.types;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import org.bukkit.inventory.ItemStack;

import static de.Keyle.MyPet.api.entity.LeashFlag.Tamed;
import static org.bukkit.Material.*;

@DefaultInfo(food = {SUGAR, WHEAT, APPLE}, leashFlags = {Tamed})
public interface MyHorse extends ActiveMyPet, MyPetBaby {
    int getAge();

    void setAge(int value);

    ItemStack getArmor();

    boolean hasArmor();

    void setArmor(ItemStack item);

    ItemStack getChest();

    boolean hasChest();

    void setChest(ItemStack item);

    ItemStack getSaddle();

    boolean hasSaddle();

    void setSaddle(ItemStack item);

    byte getHorseType();

    void setHorseType(byte horseType);

    int getVariant();

    void setVariant(int variant);
}