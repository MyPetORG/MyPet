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

package de.Keyle.MyPet.util.shop;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.commands.CommandTrade;
import org.bukkit.entity.Player;

/** Reusable trade-business logic. Used by /pettrade and the trade hub flow. */
public final class PetTradeService {

    private PetTradeService() {}

    /**
     * Initiates a free trade offer from {@code from} to {@code target} for the given pet.
     * Sends accept/reject prompts and handles ownership transfer via the {@code /pettrade}
     * accept/reject/cancel sub-commands. Emits chat messages on failure (existing open offer,
     * self-trade, missing offer permission, etc).
     */
    public static void offerTrade(Player from, Player target, Pet pet) {
        CommandTrade.beginTrade(from, target, pet, 0);
    }
}
