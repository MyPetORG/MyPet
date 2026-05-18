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
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetAquaticEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.Salmon;

import java.util.List;

@ShopInfo
@DefaultInfo(food = {Material.SEAGRASS})
public class PetSalmon extends PetImpl implements PetAquaticEntity {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Salmon", "CanSwim", true);
    public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Salmon", "PreventSuffocation", true);


    // Salmon.Variant + setVariant landed in 1.21.2. On older Paper the
    // spec factory throws LinkageError and is silently dropped by specs().
    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("variant", Salmon.class, Salmon.Variant.class, Salmon::setVariant)
    );

    public PetSalmon(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
