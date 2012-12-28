/*
 * Copyright (C) 2011-2012 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyPetPermissions
{
    private static Object permissions;

    public enum PermissionsType
    {
        NONE, Vault, Superperms
    }

    private static PermissionsType permissionsMode = PermissionsType.NONE;


    public static boolean has(Player player, String node)
    {
        if (player.isOp())
        {
            //MyPetUtil.getLogger().info("--- permissions:" + node + " -> OP -> true");
            return true;
        }
        else if (permissionsMode == PermissionsType.NONE)
        {
            //MyPetUtil.getLogger().info("--- permissions:" + node + " -> None -> true");
            return true;
        }
        else if (permissionsMode == PermissionsType.Vault)
        {
            //MyPetUtil.getLogger().info("--- permissions:" + node + " -> Vault -> " + ((Permission) Permissions).has(player, node));
            return ((Permission) permissions).has(player, node);
        }
        else if (permissionsMode == PermissionsType.Superperms)
        {
            //MyPetUtil.getLogger().info("--- permissions:" + node + " -> Bukkit -> " + player.hasPermission(node));
            return player.hasPermission(node);
        }
        return false;

    }

    public static void setup(PermissionsType pt)
    {
        permissionsMode = pt;
    }

    public static void setup()
    {
        Plugin p;

        p = MyPetPlugin.getPlugin().getServer().getPluginManager().getPlugin("Vault");
        if (p != null && permissionsMode == PermissionsType.NONE)
        {
            permissionsMode = PermissionsType.Vault;
            permissions = null;

            RegisteredServiceProvider<Permission> permissionProvider = MyPetPlugin.getPlugin().getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null)
            {
                permissions = permissionProvider.getProvider();
            }
            if (permissions != null)
            {
                MyPetLogger.write(ChatColor.GREEN + "\"Vault\"" + ChatColor.RESET + " integration enabled!");
                MyPetUtil.getDebugLogger().info("Permissions: Vault");
                return;
            }
            permissionsMode = PermissionsType.NONE;
        }

        if (permissionsMode == PermissionsType.NONE && MyPetConfig.superperms)
        {
            permissionsMode = PermissionsType.Superperms;
            MyPetLogger.write(ChatColor.YELLOW + "\"Superperms\"" + ChatColor.RESET + " integration enabled!");
            MyPetUtil.getDebugLogger().info("Permissions: Superperms");
            return;
        }

        MyPetLogger.write(ChatColor.RED + "No" + ChatColor.RESET + " permissions system found!");
        MyPetUtil.getDebugLogger().info("Permissions: -");
    }

    public static PermissionsType getPermissionsMode()
    {
        return permissionsMode;
    }
}