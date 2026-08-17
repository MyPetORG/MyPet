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
import de.Keyle.MyPet.api.entity.PetNaturalDrop;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;

import java.util.List;
import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.TORCHFLOWER_SEEDS}, leashFlags = {"Tamed"}, flySpeed = 0.2203D)
public class PetSniffer extends PetImpl implements PetBaby, PetNaturalDrop {

    public static final ConfigKey<Boolean> CAN_DIG_SEEDS = ConfigKey.bool("Sniffer", "CanDigSeeds", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Sniffer", "experience_bottle");

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     * Empty by default — MyPet strips nothing from this species' brain. The key
     * exists so an admin can disable brain AI here without a plugin change;
     * entries are {@code activity:<name>} or {@code behavior:<SimpleClassName>}.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED =
            ConfigKey.stringList("Sniffer", "Brain.Disabled");


    public PetSniffer(MyPetPlayer petOwner) {
        super(petOwner);
    }


    @Override
    public Set<Material> naturalDropMaterials() {
        return Set.of(Material.TORCHFLOWER_SEEDS, Material.PITCHER_POD);
    }

    @Override
    public boolean isNaturalDropSuppressed() {
        return !PetSniffer.CAN_DIG_SEEDS.get();
    }
}
