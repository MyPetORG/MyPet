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
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.Snowman;
import org.bukkit.event.block.EntityBlockFormEvent;

import java.util.List;

@ShopInfo
@DefaultInfo(food = {Material.CARROT, Material.SNOWBALL}, fallbackIconMaterial = "PUMPKIN")
public class PetSnowGolem extends PetImpl {

    public static final ConfigKey<Boolean> DISABLE_SNOW_TRACK = ConfigKey.bool("SnowGolem", "DisableSnowTrack", true);

    /**
     * Cancels snow-block placement by SnowGolem pets when
     * {@link #DISABLE_SNOW_TRACK} is on. Vanilla {@code SnowGolem#aiStep}
     * places top-snow on grass/dirt in cold biomes; SnowGolem is the only
     * vanilla mob that uses {@link EntityBlockFormEvent}, so the marker
     * check (via the dispatcher) is sufficient. The block-type guard is
     * defensive in case Mojang ever extends the event to other entities.
     */
    public static final PetBehavior<EntityBlockFormEvent> SNOW_TRACK_SUPPRESS =
            PetBehaviorHelpers.onPetBlockForm("SnowGolem", (event, pet, mob) -> {
                if (event.getNewState().getType() == Material.SNOW
                        && DISABLE_SNOW_TRACK.get()) {
                    event.setCancelled(true);
                }
            });

    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofFlag("derp", Snowman.class, s -> s.setDerp(true))
    );

    public PetSnowGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

}
