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

import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.Mule;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.SUGAR, Material.WHEAT, Material.APPLE}, leashFlags = {"Tamed"})
public class PetMule extends PetImpl implements PetBaby, PetEquipment {

    public PetMule(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof Mule mule)) return new ItemStack[]{null};
        return new ItemStack[]{mule.getInventory().getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof Mule mule)) return null;
        if ("SADDLE".equals(slot.name())) return mule.getInventory().getSaddle();
        return null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof Mule mule)) {
            super.setEquipmentBySlotName(slotName, item);
            return;
        }
        if ("SADDLE".equals(slotName)) {
            mule.getInventory().setSaddle(item);
        } else {
            super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof Mule mule)) return;
        AbstractHorseInventory inv = mule.getInventory();
        ItemStack saddle = inv.getSaddle();
        if (saddle != null && saddle.getType() != Material.AIR) {
            mule.getWorld().dropItem(mule.getLocation(), saddle);
            inv.setSaddle(null);
        }
        if (mule.isCarryingChest()) {
            mule.getWorld().dropItem(mule.getLocation(), new ItemStack(Material.CHEST));
            mule.setCarryingChest(false);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE");
    }
}
