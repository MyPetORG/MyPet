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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.entity.ShopInfo;
import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.SUGAR, Material.WHEAT, Material.APPLE}, leashFlags = {"Tamed"}, growUpItem = Material.BREAD)
public class PetHorse extends MyPet implements MyPetBaby, MyPetEquipment {

    public PetHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    // ─── MyPetEquipment ─── routes BODY/SADDLE to the horse's inventory ───

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof Horse horse)) return new ItemStack[]{null, null};
        HorseInventory inv = horse.getInventory();
        return new ItemStack[]{inv.getArmor(), inv.getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof Horse horse)) return null;
        return switch (slot.name()) {
            case "BODY" -> horse.getInventory().getArmor();
            case "SADDLE" -> horse.getInventory().getSaddle();
            default -> null;
        };
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof Horse horse)) return;
        switch (slotName) {
            case "BODY" -> horse.getInventory().setArmor(item);
            case "SADDLE" -> horse.getInventory().setSaddle(item);
            default -> super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof Horse horse)) return;
        HorseInventory inv = horse.getInventory();
        ItemStack saddle = inv.getSaddle();
        ItemStack armor = inv.getArmor();
        if (saddle != null && saddle.getType() != Material.AIR) {
            horse.getWorld().dropItem(horse.getLocation(), saddle);
            inv.setSaddle(null);
        }
        if (armor != null && armor.getType() != Material.AIR) {
            horse.getWorld().dropItem(horse.getLocation(), armor);
            inv.setArmor(null);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE", "BODY");
    }
}
