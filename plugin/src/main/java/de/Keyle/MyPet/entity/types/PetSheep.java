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
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetInteractionGate;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.WHEAT}, flySpeed = 0.5066D)
public class PetSheep extends PetImpl implements PetBaby, PetInteractionGate {

    public static final ConfigKey<Boolean> CAN_BE_SHEARED = ConfigKey.bool("Sheep", "CanBeSheared", true);
    public static final ConfigKey<Boolean> CAN_REGROW_WOOL = ConfigKey.bool("Sheep", "CanRegrowWool", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("Sheep", "experience_bottle");


    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("color",   Sheep.class, DyeColor.class, Sheep::setColor),
            () -> OptionSpec.ofFlag("sheared", Sheep.class,                 s -> s.setSheared(true))
    );

    public static final Supplier<Listener> GRASS_EATING_SUPPRESSOR =
            PetListenerRegistry.register(GrassEatingSuppressor::new);

    public PetSheep(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public Set<Material> gatedInteractionItems() {
        return Set.of(Material.SHEARS);
    }

    @Override
    public boolean isInteractionSuppressed() {
        return !PetSheep.CAN_BE_SHEARED.get();
    }

    /**
     * Suppresses vanilla grass-eating for sheep pets when admins disable
     * {@link #CAN_REGROW_WOOL}. Since v4 pets are real vanilla mobs, the
     * vanilla {@code EatBlockGoal} runs on pet sheep and converts
     * SHORT_GRASS to AIR / GRASS_BLOCK to DIRT around the pet while
     * regrowing sheared wool.
     *
     * <p>Default {@code true}: vanilla behavior is preserved. Admins who
     * set {@code CanRegrowWool: false} get the suppressor — grass stays
     * intact and wool regrowth has to go through the player-driven
     * interaction path (gated by {@link #CAN_BE_SHEARED}).
     */
    public static final class GrassEatingSuppressor implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onPetSheepEatGrass(EntityChangeBlockEvent event) {
            if (CAN_REGROW_WOOL.get()) return;
            if (!(event.getEntity() instanceof Sheep)) return;
            if (!PetEntityMarker.isMarked(event.getEntity())) return;
            event.setCancelled(true);
        }
    }
}
