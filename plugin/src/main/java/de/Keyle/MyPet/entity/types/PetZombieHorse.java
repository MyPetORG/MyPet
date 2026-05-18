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
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetSaddleable;
import de.Keyle.MyPet.api.entity.PetTameable;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@ShopInfo(displayName = "Zombie Horse")
@DefaultInfo(food = {Material.ROTTEN_FLESH})
public class PetZombieHorse extends PetImpl implements PetBaby, PetEquipment, PetSaddleable, PetTameable {

    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("ZombieHorse", "experience_bottle");


    public PetZombieHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public ItemStack[] getEquipment() {
        if (!(getBukkitEntity() instanceof ZombieHorse horse)) return new ItemStack[]{null};
        return new ItemStack[]{horse.getInventory().getSaddle()};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (!(getBukkitEntity() instanceof ZombieHorse horse)) return null;
        if ("SADDLE".equals(slot.name())) return horse.getInventory().getSaddle();
        return null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (!(getBukkitEntity() instanceof ZombieHorse horse)) {
            super.setEquipmentBySlotName(slotName, item);
            return;
        }
        if ("SADDLE".equals(slotName)) {
            horse.getInventory().setSaddle(item);
        } else {
            super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status != PetState.Here || !(getBukkitEntity() instanceof ZombieHorse horse)) return;
        AbstractHorseInventory inv = horse.getInventory();
        ItemStack saddle = inv.getSaddle();
        if (saddle != null && saddle.getType() != Material.AIR) {
            horse.getWorld().dropItem(horse.getLocation(), saddle);
            inv.setSaddle(null);
        }
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("SADDLE");
    }
}
