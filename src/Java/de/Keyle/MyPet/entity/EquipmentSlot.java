/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity;

public enum EquipmentSlot
{
    Weapon(0),
    Boots(1),
    Leggins(2),
    Chestplate(3),
    Helmet(4);

    int slot;

    EquipmentSlot(int slot)
    {
        this.slot = slot;
    }

    public int getSlotId()
    {
        return this.slot;
    }

    public static EquipmentSlot getSlotById(int id)
    {
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getSlotId() == id)
            {
                return slot;
            }
        }
        return EquipmentSlot.Weapon;
    }
}
