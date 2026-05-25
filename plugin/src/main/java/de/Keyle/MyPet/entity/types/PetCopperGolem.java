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

import de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval;
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
@DefaultInfo(food = {Material.COPPER_INGOT}, leashFlags = {"UserCreated"}, flySpeed = 0.4405D)
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

    /**
     * Strip the vanilla brain behaviors that drive autonomous item logistics
     * — the {@code TransportItemsBetweenContainers} behavior in
     * {@code CopperGolemAi}'s {@code IDLE} activity. Without removal, a pet
     * copper golem will walk to copper chests to pick items out of them and
     * to standard chests to deposit them. The transfer itself happens via direct
     * {@code container.setItem(...)} — no Bukkit event fires for third-party
     * plugins to cancel — so strip-at-spawn is the only clean intervention.
     *
     * <p>Other behaviors registered on the brain ({@code MoveToTargetSink},
     * {@code LookAtTargetSink}, {@code AnimalPanic}, cooldown countdowns,
     * etc.) are left in place — they're either consumers of memories MyPet
     * goals don't write (harmless) or cosmetic glances, and removing them
     * piecemeal isn't needed to fix the reported bug.
     */
    public static final PetBrainBehaviorRemoval BRAIN_BEHAVIOR_REMOVAL = new PetBrainBehaviorRemoval(
            "CopperGolem",
            "TransportItemsBetweenContainers"
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
