/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.api.entity.types;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ShopInfo;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@ShopInfo(displayName = "Copper Golem")
@DefaultInfo(food = {Material.COPPER_INGOT}, leashFlags = {"UserCreated"})
public interface MyCopperGolem extends MyPet {

    OxidationState getOxidationState();

    void setOxidationState(OxidationState state);

    boolean isWaxed();

    void setWaxed(boolean waxed);

    ItemStack getPoppy();

    void setPoppy(ItemStack item);

    /**
     * Game ticks remaining in the current oxidation stage. Captured from
     * vanilla on despawn, so the timer resumes from the same point on respawn,
     * rather than vanilla rolling a fresh schedule each spawn cycle.
     * {@code 0} means "no in-flight schedule — let vanilla pick a fresh one".
     */
    long getOxidationRemainingTicks();

    void setOxidationRemainingTicks(long ticks);

    boolean hasPoppy();

    enum OxidationState {
        UNAFFECTED, EXPOSED, WEATHERED, OXIDIZED
    }
}
