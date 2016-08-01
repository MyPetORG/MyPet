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

package de.Keyle.MyPet.util.hooks;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.util.PluginHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@PluginHookName("WorldGuard")
public class WorldGuardHook extends PluginHook implements PlayerVersusPlayerHook {

    protected WorldGuardPlugin wgp = null;

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_WorldGuard) {
            wgp = MyPetApi.getPluginHookManager().getPluginInstance(WorldGuardPlugin.class).get();
            return true;
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Location location = defender.getLocation();
            WorldGuardPlugin wgp = MyPetApi.getPluginHookManager().getPluginInstance(WorldGuardPlugin.class).get();
            RegionManager mgr = wgp.getRegionManager(location.getWorld());
            ApplicableRegionSet set = mgr.getApplicableRegions(location);
            StateFlag.State s = set.queryState(null, DefaultFlag.PVP);
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable ignored) {
        }
        return true;
    }

    public WorldGuardPlugin getPlugin() {
        return wgp;
    }
}