/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation.beacon;

import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftInventory;
import org.bukkit.inventory.BeaconInventory;
import org.bukkit.inventory.ItemStack;

public class CraftMyPetInventoryBeacon extends CraftInventory implements BeaconInventory
{
    public CraftMyPetInventoryBeacon(MyPetCustomBeaconInventory beaconInv)
    {
        super(beaconInv);
    }

    public void setItem(ItemStack item)
    {
        setItem(0, item);
    }

    public ItemStack getItem()
    {
        return getItem(0);
    }
}