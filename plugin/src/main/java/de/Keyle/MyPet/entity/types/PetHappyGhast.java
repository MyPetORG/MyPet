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
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.PetMultiPassenger;
import de.Keyle.MyPet.api.entity.PetNaturallyRideable;
import de.Keyle.MyPet.api.entity.PetSaddleable;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.options.PetCreationOptions;
import de.Keyle.MyPet.entity.options.PetCreationOptions.OptionSpec;
import de.Keyle.MyPet.util.PetSaddleHelper;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@ShopInfo
@DefaultInfo(food = {Material.GHAST_TEAR}, leashFlags = {"Tamed"}, flySpeed = 0.5556D)
public class PetHappyGhast extends PetImpl implements PetBaby, PetFlyingEntity, PetMultiPassenger, PetNaturallyRideable, PetSaddleable {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("HappyGhast", "CanFly", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("HappyGhast", "experience_bottle");

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     * Empty by default — MyPet strips nothing from this species' brain. The key
     * exists so an admin can disable brain AI here without a plugin change;
     * entries are {@code activity:<name>} or {@code behavior:<SimpleClassName>}.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED =
            ConfigKey.stringList("HappyGhast", "Brain.Disabled");

    /**
     * Per-pet creation option spec — admins use
     * {@code /petadmin create <owner> HappyGhast harness:<color>} to spawn a
     * HappyGhast with the matching {@code <COLOR>_HARNESS} material equipped.
     * Sixteen valid colors — any {@link DyeColor} enum value.
     *
     * <p>HappyGhast does not get a generic {@code saddle:} flag from the
     * {@code PetSaddleable} auto-gen because {@code PetSaddleHelper.getDefaultSaddleStack}
     * returns {@code null} for this class — the {@link Material#SADDLE} item
     * isn't accepted by HappyGhast (only {@code *_HARNESS} variants are).
     */
    public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
            () -> OptionSpec.ofEnum("harness", HappyGhast.class, DyeColor.class,
                    (mob, color) -> PetSaddleHelper.applySaddle(mob,
                            new ItemStack(Material.valueOf(color.name() + "_HARNESS"))))
    );

    public PetHappyGhast(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public double getYSpawnOffset() {
        return 4;
    }

    /**
     * Vanilla shift-right-click on a Happy Ghast attaches the player's
     * leashed mob to it. Defer to vanilla so the leash-transfer can happen
     * instead of consuming the gesture for sit-toggle.
     */
    @Override
    protected boolean defersSneakInteractToVanilla(Player player) {
        return hasLeashedEntity(player);
    }
}
