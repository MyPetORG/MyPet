/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.api.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyHook {
    private static boolean searchedVaultEconomy = false;
    private static Economy economy = null;

    public static boolean canUseEconomy() {
        if (!Configuration.Hooks.USE_ECONOMY) {
            return false;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        return economy != null;
    }

    public static boolean canPay(MyPetPlayer petOwner, double costs) {
        return canPay(petOwner.getPlayer(), costs);
    }

    public static boolean canPay(UUID playerUUID, double costs) {
        return canPay(Bukkit.getOfflinePlayer(playerUUID), costs);
    }

    public static boolean canPay(OfflinePlayer player, double costs) {
        if (!Configuration.Hooks.USE_ECONOMY) {
            return true;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled()) {
            try {
                return economy.has(player, costs);
            } catch (Exception e) {
                e.printStackTrace();
                MyPetApi.getLogger().warning("The economy plugin threw an exception, economy support disabled.");
                Configuration.Hooks.USE_ECONOMY = false;
            }
        }
        return true;
    }

    public static boolean transfer(MyPetPlayer from, MyPetPlayer to, double costs) {
        return transfer(from.getPlayer(), to.getPlayer(), costs);
    }

    public static boolean transfer(UUID from, UUID to, double costs) {
        return transfer(Bukkit.getOfflinePlayer(from), Bukkit.getOfflinePlayer(to), costs);
    }

    public static boolean transfer(OfflinePlayer from, OfflinePlayer to, double costs) {
        if (!Configuration.Hooks.USE_ECONOMY) {
            return true;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled()) {
            if (economy.has(from, costs)) {
                try {
                    return economy.withdrawPlayer(from, costs).transactionSuccess() && economy.depositPlayer(to, costs).transactionSuccess();
                } catch (Exception e) {
                    e.printStackTrace();
                    MyPetApi.getLogger().warning("The economy plugin threw an exception, economy support disabled.");
                    Configuration.Hooks.USE_ECONOMY = false;
                }
            }
            return false;
        }
        return true;
    }

    public static boolean pay(MyPetPlayer petOwner, double costs) {
        return pay(petOwner.getPlayer(), costs);
    }

    public static boolean pay(UUID playerUUID, double costs) {
        return pay(Bukkit.getOfflinePlayer(playerUUID), costs);
    }

    public static boolean pay(OfflinePlayer player, double costs) {
        if (!Configuration.Hooks.USE_ECONOMY) {
            return true;
        }
        if (!searchedVaultEconomy) {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled()) {
            if (economy.has(player, costs)) {
                try {
                    return economy.withdrawPlayer(player, costs).transactionSuccess();
                } catch (Exception e) {
                    e.printStackTrace();
                    MyPetApi.getLogger().warning("The economy plugin threw an exception, economy support disabled.");
                    Configuration.Hooks.USE_ECONOMY = false;
                }
            }
            return false;
        }
        return true;
    }

    public static void reset() {
        Configuration.Hooks.USE_ECONOMY = false;
        searchedVaultEconomy = false;
        economy = null;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static void setupEconomy() {
        if (PluginHookManager.isPluginUsable("Vault")) {
            RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) {
                economy = economyProvider.getProvider();
                searchedVaultEconomy = true;
                return;
            }
        }
        MyPetApi.getLogger().info("No Economy plugin found. Economy hook not enabled.");
        searchedVaultEconomy = true;
    }
}