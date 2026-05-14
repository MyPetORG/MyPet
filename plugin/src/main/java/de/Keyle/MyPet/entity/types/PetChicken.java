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

import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.PetNaturalDrop;

import java.util.Set;

import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.WHEAT_SEEDS})
public class PetChicken extends PetImpl implements PetBaby, PetNaturalDrop {

    public PetChicken(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Set<Material> naturalDropMaterials() {
        return Set.of(Material.EGG);
    }

    @Override
    public boolean isNaturalDropSuppressed() {
        return !Configuration.MyPet.Chicken.CAN_LAY_EGGS;
    }
}
