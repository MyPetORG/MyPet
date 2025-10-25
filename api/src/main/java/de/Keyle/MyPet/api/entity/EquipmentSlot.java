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

package de.Keyle.MyPet.api.entity;

public enum EquipmentSlot {
    MainHand(0, 0),
    OffHand(5, 1),
    Boots(1, 2),
    Leggins(2, 3),
    Chestplate(3, 4),
    Helmet(4, 5);

    int slot;
    int slot_19;

    EquipmentSlot(int slot, int slot_19) {
        this.slot = slot;
        this.slot_19 = slot_19;
    }

    public static EquipmentSlot getSlotById(int id) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.slot == id) {
                return slot;
            }
        }
        return EquipmentSlot.MainHand;
    }

    public int get19Slot() {
        return slot_19;
    }

    public int getSlotId() {
        return this.slot;
    }
}