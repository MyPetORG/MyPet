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
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.ROTTEN_FLESH}, flySpeed = 0.5066D)
public class PetDrowned extends PetImpl implements PetEquipment, PetBaby, PetAmphibiousEntity, PetSunSensitive {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Drowned", "CanSwim", true);
    public static final ConfigKey<Boolean> PREVENT_DAYLIGHT_BURN = ConfigKey.bool("Drowned", "PreventDaylightBurn", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Drowned", "experience_bottle");


    public PetDrowned(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
