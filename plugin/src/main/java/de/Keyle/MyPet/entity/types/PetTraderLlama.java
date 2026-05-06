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
import org.bukkit.entity.TraderLlama;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LlamaInventory;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.ShopInfo;
import org.bukkit.Material;

@ShopInfo(displayName = "Trader Llama")
@DefaultInfo(food = {Material.WHEAT}, leashFlags = {"Tamed"})
public class PetTraderLlama extends PetImpl implements PetBaby {

    public PetTraderLlama(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public void dropEquipment() {
        super.dropEquipment();
        if (status != PetState.Here || !(getBukkitEntity() instanceof TraderLlama llama)) return;
        LlamaInventory inv = llama.getInventory();
        ItemStack decor = inv.getDecor();
        if (decor != null) {
            llama.getWorld().dropItem(llama.getLocation(), decor);
            inv.setDecor(null);
        }
    }
}
