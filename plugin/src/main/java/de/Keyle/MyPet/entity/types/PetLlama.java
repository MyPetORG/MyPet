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
import de.Keyle.MyPet.api.entity.PetChested;
import de.Keyle.MyPet.api.entity.PetNaturallyRideable;
import de.Keyle.MyPet.api.entity.PetTameable;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.Llama;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LlamaInventory;

import java.util.List;

@ShopInfo
@DefaultInfo(food = {Material.WHEAT}, leashFlags = {"Tamed"}, flySpeed = 0.3855D)
public class PetLlama extends PetImpl implements PetBaby, PetChested, PetNaturallyRideable, PetTameable {

    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Llama", "experience_bottle");


    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("variant", Llama.class, Llama.Color.class, Llama::setColor)
    );

    public PetLlama(MyPetPlayer petOwner) {
        super(petOwner);
    }

    // Llama dropEquipment overrides the base behaviour to also drop the
    // (mostly cosmetic) decor carpet and chest. Chest content drop on
    // unsaddle/release is vanilla-handled, so we only drop our custom
    // overrides here.
    @Override
    public void dropEquipment() {
        super.dropEquipment();
        if (status != PetState.Here || !(getBukkitEntity() instanceof Llama llama)) return;
        LlamaInventory inv = llama.getInventory();
        ItemStack decor = inv.getDecor();
        if (decor != null) {
            llama.getWorld().dropItem(llama.getLocation(), decor);
            inv.setDecor(null);
        }
    }
}
