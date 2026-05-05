/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.entity;

/**
 * Marker for pet types whose underlying vanilla mob has a baby/adult
 * lifecycle (animals, zombies, piglins, etc.). Exposes the baby state
 * so it can be persisted across despawns and restored by
 * {@code PetVisualSyncer}.
 * <p>
 * Also gates UI features: pets implementing this marker appear in the
 * "babies" section of {@link ShopInfo}-driven shops, and the
 * {@link DefaultInfo#growUpItem()} interaction is only offered to types
 * that implement this interface.
 */
public interface MyPetBaby {

    /** Returns {@code true} if this pet is currently in its baby form. */
    boolean isBaby();

    /**
     * Sets the baby/adult state. The visual syncer pushes this to the
     * Bukkit entity's {@code setAdult()}/{@code setBaby()} on the next
     * tick.
     */
    void setBaby(boolean flag);
}