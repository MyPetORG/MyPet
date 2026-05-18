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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import io.papermc.paper.world.WeatheringCopperState;
import org.bukkit.Material;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Mob;

import java.util.List;

@ShopInfo(displayName = "Copper Golem")
@DefaultInfo(food = {Material.COPPER_INGOT}, leashFlags = {"UserCreated"})
public class PetCopperGolem extends PetImpl {

    public static final ConfigKey<Boolean> CAN_OXIDIZE = ConfigKey.bool("CopperGolem", "CanOxidize", true);


    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("oxidation", CopperGolem.class, WeatheringCopperState.class, CopperGolem::setWeatheringState),
            () -> OptionSpec.ofFlag("waxed",     CopperGolem.class,                              g -> g.setOxidizing(CopperGolem.Oxidizing.waxed()))
    );

    /**
     * Overrides vanilla's oxidation schedule with {@code Oxidizing.waxed()}
     * at spawn time when {@link #CAN_OXIDIZE} is disabled, freezing the
     * weathering state for the pet's lifetime. The snapshot envelope round-
     * trips the vanilla {@code Oxidizing} NBT verbatim, so without this
     * override an admin's {@code CanOxidize: false} would silently be ignored
     * after the legacy migration window. Toggling the flag takes effect on
     * the next spawn (despawn/recall or restart).
     */
    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "CopperGolem",
            PetCopperGolem::applyOxidationSuppress,
            pet -> {}
    );

    public PetCopperGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    private static void applyOxidationSuppress(Pet pet) {
        if (CAN_OXIDIZE.get()) return;
        Mob mob = pet.getBukkitEntity();
        if (mob instanceof CopperGolem golem) {
            try {
                golem.setOxidizing(CopperGolem.Oxidizing.waxed());
            } catch (Throwable ignored) {
            }
        }
    }

}
