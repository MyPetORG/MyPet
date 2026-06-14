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
import de.Keyle.MyPet.api.entity.PetAmphibiousEntity;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetNaturalDrop;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Set;

@Getter
@ShopInfo
@DefaultInfo(food = {Material.SEAGRASS}, flySpeed = 0.5507D)
public class PetTurtle extends PetImpl implements PetBaby, PetAmphibiousEntity, PetNaturalDrop {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Turtle", "CanSwim", true);
    public static final ConfigKey<Boolean> CAN_DROP_SCUTE = ConfigKey.bool("Turtle", "CanDropScute", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Turtle", "experience_bottle");


    public PetTurtle(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Set<Material> naturalDropMaterials() {
        return Set.of(Material.TURTLE_SCUTE);
    }

    @Override
    public boolean isNaturalDropSuppressed() {
        return !PetTurtle.CAN_DROP_SCUTE.get();
    }
}