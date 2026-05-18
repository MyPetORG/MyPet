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
import de.Keyle.MyPet.api.entity.PetAmphibiousEntity;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.Nautilus;
import org.bukkit.inventory.ArmoredSaddledMountInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.NAUTILUS_SHELL}, leashFlags = {"Tamed"})
public class PetNautilus extends PetImpl implements PetEquipment, PetAmphibiousEntity {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Nautilus", "CanSwim", true);


    public PetNautilus(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof Nautilus nautilus)) return new ItemStack[]{null, null};
        ArmoredSaddledMountInventory inv = nautilus.getInventory();
        return new ItemStack[]{inv.getArmor(), inv.getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof Nautilus nautilus)) return null;
        return switch (slot.name()) {
            case "BODY" -> nautilus.getInventory().getArmor();
            case "SADDLE" -> nautilus.getInventory().getSaddle();
            default -> null;
        };
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof Nautilus nautilus)) {
            super.setEquipmentBySlotName(slotName, item);
            return;
        }
        switch (slotName) {
            case "BODY" -> nautilus.getInventory().setArmor(item);
            case "SADDLE" -> nautilus.getInventory().setSaddle(item);
            default -> super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof Nautilus nautilus)) return;
        ArmoredSaddledMountInventory inv = nautilus.getInventory();
        ItemStack saddle = inv.getSaddle();
        ItemStack armor = inv.getArmor();
        if (saddle != null && saddle.getType() != Material.AIR) {
            nautilus.getWorld().dropItem(nautilus.getLocation(), saddle);
            inv.setSaddle(null);
        }
        if (armor != null && armor.getType() != Material.AIR) {
            nautilus.getWorld().dropItem(nautilus.getLocation(), armor);
            inv.setArmor(null);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE", "BODY");
    }
}
