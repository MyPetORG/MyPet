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

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetLightningConvertible;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.APPLE})
public class PetVillager extends PetImpl implements PetBaby, PetEquipment, PetLightningConvertible {

    // Villager also gets its trade level reset alongside the profession change —
    // matches the legacy behavior (fresh-profession villagers have no trades).
    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofRegistry("profession", Villager.class, RegistryKey.VILLAGER_PROFESSION,
                    (Villager v, Villager.Profession p) -> { v.setProfession(p); v.setVillagerLevel(1); }),
            () -> OptionSpec.ofRegistry("type",       Villager.class, RegistryKey.VILLAGER_TYPE,       Villager::setVillagerType)
    );

    public PetVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND) return;
        super.setEquipment(slot, item);
    }

    @Override
    public Set<String> getAllowedSlotNames() {
        return Set.of("HAND");
    }
}
