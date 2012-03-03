/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util;

import de.Keyle.MyWolf.MyWolfPlugin;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class MyWolfPermissions
{
    private static Object Permissions;

    public enum PermissionsType
    {
        NONE, Vault, bPermissions, PermissionsEX, BukkitPermissions//, Permissions, GroupManager
    }

    private static PermissionsType PermissionsMode = PermissionsType.NONE;


    public static boolean has(Player player, String node)
    {
        if (player.isOp())
        {
            return true;
        }
        else if (PermissionsMode == PermissionsType.NONE || Permissions == null)
        {
            return true;
        }
        else if(PermissionsMode == PermissionsType.Vault)
        {
            ((Permission)Permissions).has(player,node);
        }
        else if (PermissionsMode == PermissionsType.PermissionsEX && Permissions instanceof PermissionManager)
        {
            return ((PermissionManager) Permissions).has(player, node);
        }
        else if (PermissionsMode == PermissionsType.BukkitPermissions || PermissionsMode == PermissionsType.bPermissions)
        {
            player.hasPermission(node);
        }
        return false;

    }

    public static void setup(PermissionsType pt)
    {
        PermissionsMode = pt;
    }

    public static void setup()
    {
        Plugin p;

        p = MyWolfPlugin.getPlugin().getServer().getPluginManager().getPlugin("Vault");
        if (p != null && PermissionsMode == PermissionsType.NONE)
        {
            PermissionsMode = PermissionsType.Vault;
            Permissions = null;

            RegisteredServiceProvider<Permission> permissionProvider = MyWolfPlugin.getPlugin().getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null)
            {
                Permissions = permissionProvider.getProvider();
            }
            if(Permissions != null)
            {
                MyWolfUtil.getLogger().info("[MyWolf] bPermissions integration enabled!");
                return;
            }
            PermissionsMode = PermissionsType.NONE;
        }

        p = MyWolfPlugin.getPlugin().getServer().getPluginManager().getPlugin("bPermissions");
        if (p != null && PermissionsMode == PermissionsType.NONE)
        {
            PermissionsMode = PermissionsType.bPermissions;
            Permissions = null;
            MyWolfUtil.getLogger().info("[MyWolf] bPermissions integration enabled!");
            return;
        }

        p = MyWolfPlugin.getPlugin().getServer().getPluginManager().getPlugin("PermissionsEx");
        if (p != null && PermissionsMode == PermissionsType.NONE)
        {
            PermissionsMode = PermissionsType.PermissionsEX;
            Permissions = PermissionsEx.getPermissionManager();
            MyWolfUtil.getLogger().info("[MyWolf] PermissionsEX integration enabled!");
            return;
        }

        MyWolfUtil.getLogger().info("[MyWolf] No permissions system fund!");
    }
}