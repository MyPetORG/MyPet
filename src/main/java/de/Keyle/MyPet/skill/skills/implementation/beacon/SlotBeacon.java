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

import net.minecraft.server.v1_6_R2.IInventory;
import net.minecraft.server.v1_6_R2.Item;
import net.minecraft.server.v1_6_R2.ItemStack;
import net.minecraft.server.v1_6_R2.Slot;

public class SlotBeacon extends Slot {

    public SlotBeacon(IInventory beaconInventory, int index, int posX, int posY) {
        super(beaconInventory, index, posX, posY);
    }

    public boolean isAllowed(ItemStack paramItemStack) {
        return paramItemStack != null && (paramItemStack.id == Item.EMERALD.id || paramItemStack.id == Item.DIAMOND.id || paramItemStack.id == Item.GOLD_INGOT.id || paramItemStack.id == Item.IRON_INGOT.id);
    }

    public int a() {
        return 1;
    }
}
