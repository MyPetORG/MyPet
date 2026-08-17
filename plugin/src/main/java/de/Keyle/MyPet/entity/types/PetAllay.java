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

import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.APPLE}, flySpeed = 1.1111D)
public class PetAllay extends PetImpl implements PetEquipment, PetFlyingEntity {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Allay", "CanFly", true);

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     * Empty by default — MyPet strips nothing from this species' brain. The key
     * exists so an admin can disable brain AI here without a plugin change;
     * entries are {@code activity:<name>} or {@code behavior:<SimpleClassName>}.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED =
            ConfigKey.stringList("Allay", "Brain.Disabled");


    public PetAllay(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND) {
            return;
        }
        super.setEquipment(slot, item);
    }


    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("HAND");
    }
}
