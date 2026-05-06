/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 */

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetAquaticEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import org.bukkit.Material;

@ShopInfo
@DefaultInfo(food = {Material.SEAGRASS})
public class PetSalmon extends MyPet implements MyPetAquaticEntity {

    public PetSalmon(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
