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
import de.Keyle.MyPet.api.entity.PetMetamorphic;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;

import java.util.List;

@ShopInfo
@DefaultInfo(food = {Material.SLIME_BALL}, flySpeed = 2.2026D)
public class PetTadpole extends PetImpl implements PetAquaticEntity, PetMetamorphic {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Tadpole", "CanSwim", true);
    public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Tadpole", "PreventSuffocation", true);

    /**
     * Whether vanilla's 20-minute tadpole timer is allowed to turn this pet into a
     * Frog pet. Default {@code false}: the age timer is locked at spawn so the pet
     * stays the Tadpole its owner tamed (and keeps Tadpole-only skilltrees such as
     * Metamorphosis). Set {@code true} to let it mature — the pet is re-typed to
     * Frog, keeping its UUID, name, XP, level and skill state.
     */
    public static final ConfigKey<Boolean> ALLOW_METAMORPHOSIS = ConfigKey.bool("Tadpole", "AllowMetamorphosis", false);

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     * Empty by default — MyPet strips nothing from this species' brain. The key
     * exists so an admin can disable brain AI here without a plugin change;
     * entries are {@code activity:<name>} or {@code behavior:<SimpleClassName>}.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED =
            ConfigKey.stringList("Tadpole", "Brain.Disabled");


    public PetTadpole(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
