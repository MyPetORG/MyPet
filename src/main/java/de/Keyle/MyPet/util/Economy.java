/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class Economy {
    public static boolean USE_ECONOMY = true;
    private static boolean searchedVaultEconomy = false;
    private static net.milkbowl.vault.economy.Economy economy = null;

    public static boolean canUseEconomy() {
        if (!USE_ECONOMY) {
            return false;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        return economy != null;
    }

    public static boolean canPay(MyPetPlayer petOwner, double costs) {
        if (!USE_ECONOMY) {
            return true;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled()) {
            return economy.has(petOwner.getName(), costs);
        }
        return true;
    }

    public static boolean pay(MyPetPlayer petOwner, double costs) {
        if (!USE_ECONOMY) {
            return true;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled()) {
            if (economy.has(petOwner.getName(), costs)) {
                return economy.withdrawPlayer(petOwner.getName(), costs).transactionSuccess();
            } else {
                return false;
            }
        }
        return true;
    }

    public static void reset() {
        USE_ECONOMY = false;
        searchedVaultEconomy = false;
        economy = null;
    }

    public static net.milkbowl.vault.economy.Economy getEconomy() {
        return economy;
    }

    public static void setupEconomy() {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("Vault")) {
            searchedVaultEconomy = true;
            DebugLogger.info("Vault not found. Economy support not enabled.");
            return;
        }
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            searchedVaultEconomy = true;
            DebugLogger.info("Economy support enabled.");
            return;
        }
        DebugLogger.info("No Economy plugin found. Economy support not enabled.");
        searchedVaultEconomy = true;
    }
}