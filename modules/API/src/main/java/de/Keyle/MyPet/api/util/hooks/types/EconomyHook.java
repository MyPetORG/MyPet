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

package de.Keyle.MyPet.api.util.hooks.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * This interface defines that the hook handles economy interactions
 */
public interface EconomyHook extends PluginHook {
    boolean canPay(MyPetPlayer petOwner, double costs);

    boolean canPay(UUID playerUUID, double costs);

    boolean canPay(OfflinePlayer player, double costs);

    boolean transfer(MyPetPlayer from, MyPetPlayer to, double costs);

    boolean transfer(UUID from, UUID to, double costs);

    boolean transfer(OfflinePlayer from, OfflinePlayer to, double costs);

    boolean pay(MyPetPlayer petOwner, double costs);

    boolean pay(UUID playerUUID, double costs);

    boolean pay(OfflinePlayer player, double costs);

    double getBalance(OfflinePlayer player);

    String currencyNameSingular();

    String format(double amount);

    boolean checkEconomy();
}