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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorHelpers;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.PolarBear;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@ShopInfo(displayName = "Polar Bear")
@DefaultInfo(food = {Material.COD}, flySpeed = 0.5507D)
public class PetPolarBear extends PetImpl implements PetBaby {

    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("PolarBear", "experience_bottle");

    /**
     * Plays the bear's stand-up-and-maul animation on each successful pet hit.
     * Vanilla drives this via a {@code standingAnimationTimer} that the (stripped)
     * attack goal sets; without that, a manual {@code setStanding(true)} sticks
     * forever — schedule a {@code setStanding(false)} 20 ticks out to match the
     * vanilla animation length.
     */
    public static final PetBehavior<EntityDamageByEntityEvent> ATTACK_ANIMATION =
            PetBehaviorHelpers.onPetDamages("PolarBear", (event, pet, mob) -> {
                if (mob instanceof PolarBear bear) {
                    bear.setStanding(true);
                    bear.getScheduler().runDelayed(MyPetApi.getPlugin(),
                            task -> bear.setStanding(false), null, 20L);
                }
            });

    public PetPolarBear(MyPetPlayer petOwner) {
        super(petOwner);
    }
}
