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
import de.Keyle.MyPet.api.entity.PetAquaticEntity;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;

@ShopInfo(displayName = "Glow Squid")
@DefaultInfo(food = {Material.COD})
public class PetGlowSquid extends PetImpl implements PetBaby, PetAquaticEntity {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("GlowSquid", "CanSwim", true);
    public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("GlowSquid", "PreventSuffocation", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("GlowSquid", "experience_bottle");


    public PetGlowSquid(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
