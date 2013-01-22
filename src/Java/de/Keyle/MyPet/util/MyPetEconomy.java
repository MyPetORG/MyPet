/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyPetEconomy
{
    public static boolean USE_ECONOMY = true;
    private static boolean searchedVaultEconomy = false;
    private static Economy economy = null;

    public static boolean canUseEconomy()
    {
        if (!USE_ECONOMY)
        {
            return false;
        }
        if (!searchedVaultEconomy)
        {
            setupEconomy();
        }
        return economy != null;
    }

    public static boolean canPay(MyPetPlayer petOwner, double costs)
    {
        if (!USE_ECONOMY)
        {
            return true;
        }
        if (!searchedVaultEconomy)
        {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled())
        {
            return economy.has(petOwner.getName(), costs);
        }
        return true;
    }

    public static boolean pay(MyPetPlayer petOwner, double costs)
    {
        if (!USE_ECONOMY)
        {
            return true;
        }
        if (!searchedVaultEconomy)
        {
            setupEconomy();
        }
        if (economy != null && economy.isEnabled())
        {
            if (economy.has(petOwner.getName(), costs))
            {
                economy.withdrawPlayer(petOwner.getName(), costs);
                return true;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    public static void reset()
    {
        USE_ECONOMY = false;
        searchedVaultEconomy = false;
        economy = null;
    }

    public static Economy getEconomy()
    {
        return economy;
    }

    public static void setupEconomy()
    {
        if (!MyPetUtil.getServer().getPluginManager().isPluginEnabled("Vault"))
        {
            searchedVaultEconomy = true;
            return;
        }
        RegisteredServiceProvider<Economy> economyProvider = MyPetUtil.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
        {
            economy = economyProvider.getProvider();
        }
        searchedVaultEconomy = true;
    }
}
