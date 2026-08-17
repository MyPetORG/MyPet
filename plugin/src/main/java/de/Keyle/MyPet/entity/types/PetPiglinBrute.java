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
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetZombifiable;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;

import java.util.List;

@ShopInfo(displayName = "Piglin Brute")
@DefaultInfo(food = {Material.GOLD_NUGGET}, flySpeed = 0.7709D)
public class PetPiglinBrute extends PetImpl implements PetEquipment, PetZombifiable {

    public static final ConfigKey<Boolean> ALLOW_ZOMBIFICATION = ConfigKey.bool("PiglinBrute", "AllowZombification", false);


    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     *
     * <p>Both activities are required. {@code fight} holds the melee and chase
     * behaviors; {@code idle} holds the {@code StartAttacking} that writes
     * {@code ATTACK_TARGET}. Because {@code Mob#getTarget} falls through to
     * {@code getTargetFromBrain}, leaving {@code idle} intact would leak the
     * owner into MyPet's own goal stack as a target even with {@code fight}
     * emptied. {@code core} is kept — {@code LookAtTargetSink} and
     * {@code MoveToTargetSink} go inert once nothing writes their memories.
     *
     * <p>Activities, not behavior class names: vanilla builds these behaviors
     * with {@code BehaviorBuilder}, so they reach the brain as anonymous
     * instances with an empty simple name, and {@code StartAttacking} /
     * {@code MeleeAttack} are static-factory holders that never appear as an
     * instance class. A name-based strip removes nothing here — measured.
     *
     * <p>Emptying this list in pet-config.yml restores vanilla AI, which means
     * the pet will attack its owner. That is intended.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED = ConfigKey.stringList(
            "PiglinBrute", "Brain.Disabled",
            "activity:idle",
            "activity:fight");


    public PetPiglinBrute(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
