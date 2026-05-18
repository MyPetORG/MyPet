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
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetInteractionGate;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import org.bukkit.Material;
import org.bukkit.entity.MushroomCow;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTransformEvent;

import java.util.List;
import java.util.Set;

@ShopInfo
@DefaultInfo(food = {Material.WHEAT})
public class PetMooshroom extends PetImpl implements PetBaby, PetInteractionGate {

    public static final ConfigKey<Boolean> CAN_GIVE_STEW = ConfigKey.bool("Mooshroom", "CanGiveStew", false);
    public static final ConfigKey<Boolean> ALLOW_LIGHTNING_VARIANT_FLIP = ConfigKey.bool("Mooshroom", "AllowLightningVariantFlip", false);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Mooshroom", "experience_bottle");

    /**
     * Lightning strike on a Mooshroom pet — flips its variant (RED ↔ BROWN)
     * directly via Bukkit's {@code setVariant} API when
     * {@link #ALLOW_LIGHTNING_VARIANT_FLIP} is on.
     *
     * <p>Runs at {@link EventPriority#MONITOR} with {@code ignoreCancelled = false}
     * because {@code PetLightningStrikeListener}'s catch-all already cancels
     * the vanilla transform at HIGH (vanilla would otherwise discard + respawn
     * the entity, breaking the pet binding). This behavior just optionally
     * mutates the existing cow's variant after that cancellation.
     */
    public static final PetBehavior<EntityTransformEvent> LIGHTNING_VARIANT_FLIP =
            PetBehaviorHelpers.onPetLightningTransform("Mooshroom",
                    EventPriority.MONITOR, false, (event, pet, mob) -> {
                        if (event.getTransformReason() != EntityTransformEvent.TransformReason.LIGHTNING) return;
                        if (!ALLOW_LIGHTNING_VARIANT_FLIP.get()) return;
                        if (!(mob instanceof MushroomCow cow)) return;
                        cow.setVariant(cow.getVariant() == MushroomCow.Variant.RED
                                ? MushroomCow.Variant.BROWN
                                : MushroomCow.Variant.RED);
                    });

    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("type", MushroomCow.class, MushroomCow.Variant.class, MushroomCow::setVariant)
    );

    public PetMooshroom(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Set<Material> gatedInteractionItems() {
        return Set.of(Material.BOWL);
    }

    @Override
    public boolean isInteractionSuppressed() {
        return !CAN_GIVE_STEW.get();
    }
}
