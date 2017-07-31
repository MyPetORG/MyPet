/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.EconomyHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

@PluginHookName("Vault")
public class VaultHook implements EconomyHook {
    private Economy economy = null;

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_ECONOMY) {
            RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) {
                economy = economyProvider.getProvider();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canPay(MyPetPlayer petOwner, double costs) {
        return canPay(petOwner.getPlayer(), costs);
    }

    @Override
    public boolean canPay(UUID playerUUID, double costs) {
        return canPay(Bukkit.getOfflinePlayer(playerUUID), costs);
    }

    @Override
    public boolean canPay(OfflinePlayer player, double costs) {
        try {
            return economy.has(player, costs);
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean transfer(MyPetPlayer from, MyPetPlayer to, double costs) {
        return transfer(from.getPlayer(), to.getPlayer(), costs);
    }

    @Override
    public boolean transfer(UUID from, UUID to, double costs) {
        return transfer(Bukkit.getOfflinePlayer(from), Bukkit.getOfflinePlayer(to), costs);
    }

    @Override
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double costs) {
        if (economy.has(from, costs)) {
            try {
                return economy.withdrawPlayer(from, costs).transactionSuccess() && economy.depositPlayer(to, costs).transactionSuccess();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean pay(MyPetPlayer petOwner, double costs) {
        return pay(petOwner.getPlayer(), costs);
    }

    @Override
    public boolean pay(UUID playerUUID, double costs) {
        return pay(Bukkit.getOfflinePlayer(playerUUID), costs);
    }

    @Override
    public boolean pay(OfflinePlayer player, double costs) {

        if (economy.has(player, costs)) {
            try {
                return economy.withdrawPlayer(player, costs).transactionSuccess();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Override
    public String currencyNameSingular() {
        return economy.currencyNameSingular();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}