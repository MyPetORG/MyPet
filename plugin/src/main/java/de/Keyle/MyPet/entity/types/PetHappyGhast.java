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
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@ShopInfo
@DefaultInfo(food = {Material.GHAST_TEAR}, leashFlags = {"Tamed"})
public class PetHappyGhast extends PetImpl implements PetFlyingEntity, PetBaby {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("HappyGhast", "CanFly", true);
    public static final ConfigKey<ConfigItem> GROW_UP_ITEM = ConfigKey.growUpItem("HappyGhast", "experience_bottle");


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
