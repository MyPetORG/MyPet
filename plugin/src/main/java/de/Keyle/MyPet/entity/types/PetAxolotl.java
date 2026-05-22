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

import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorHelpers;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetAmphibiousEntity;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.Axolotl;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

@ShopInfo
@DefaultInfo(food = {Material.TROPICAL_FISH}, flySpeed = 2.2026D)
public class PetAxolotl extends PetImpl implements PetBaby, PetAmphibiousEntity {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Axolotl", "CanSwim", true);
    public static final ConfigKey<Boolean> PREVENT_DRY_OUT = ConfigKey.bool("Axolotl", "PreventDryOut", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Axolotl", "experience_bottle");

    /**
     * Suppresses {@code DRYOUT} damage on Axolotl pets when
     * {@link #PREVENT_DRY_OUT} is on. Vanilla Axolotls take DRYOUT damage
     * while on land; this lets admins keep pet Axolotls alive when their
     * owner takes them out of water.
     */
    public static final PetBehavior<EntityDamageEvent> DRYOUT_SUPPRESS =
            PetBehaviorHelpers.onPetDamaged("Axolotl", (event, pet, mob) -> {
                if (event.getCause() != DamageCause.DRYOUT) return;
                if (!(mob instanceof Axolotl)) return;
                if (PREVENT_DRY_OUT.get()) {
                    event.setCancelled(true);
                }
            });

    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("variant", Axolotl.class, Axolotl.Variant.class, Axolotl::setVariant)
    );

    public PetAxolotl(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
