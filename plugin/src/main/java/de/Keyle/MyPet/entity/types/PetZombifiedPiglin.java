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
import de.Keyle.MyPet.api.entity.leashing.WildAngerCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.PigZombie;

@ShopInfo(displayName = "Zombified Piglin")
@DefaultInfo(food = {Material.ROTTEN_FLESH})
public class PetZombifiedPiglin extends PetImpl implements PetEquipment, PetBaby, PetLavaEntity {

    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("ZombifiedPiglin", "experience_bottle");

    public static final WildAngerCheck<PigZombie> ANGER_CHECK =
            new WildAngerCheck<>(PigZombie.class, PigZombie::isAngry);


    public PetZombifiedPiglin(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
