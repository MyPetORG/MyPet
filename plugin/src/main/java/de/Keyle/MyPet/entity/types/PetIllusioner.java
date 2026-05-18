/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import java.util.Set;
import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.APPLE}, fallbackIconMaterial = "SQUID_SPAWN_EGG", fallbackIconGlow = true)
public class PetIllusioner extends PetImpl implements PetEquipment {

    public PetIllusioner(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND && slot != EquipmentSlot.HEAD) {
            return;
        }
        super.setEquipment(slot, item);
    }


    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("HAND", "OFF_HAND");
    }
}
