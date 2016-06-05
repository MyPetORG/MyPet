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

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.bukkit.Location;

public class WorldGuardCustomFlagsHook {
    public static final StateFlag FLY_FLAG = new StateFlag("mypet-fly", false);

    private static boolean active = false;
    private static WorldGuardPlugin wgPlugin = null;
    private static WGCustomFlagsPlugin wgcfPlugin;

    public static void findPlugin(WorldGuardPlugin wgp) {
        if (PluginHookManager.isPluginUsable("WGCustomFlags")) {
            active = true;
            wgPlugin = wgp;
            wgcfPlugin = PluginHookManager.getPluginInstance(WGCustomFlagsPlugin.class).get();
            wgcfPlugin.addCustomFlag(FLY_FLAG);
            MyPetApi.getLogger().info("WorldGuard Custom Flags hook activated.");
        }
    }

    public static boolean canFly(Location location) {
        try {
            RegionManager mgr = wgPlugin.getRegionManager(location.getWorld());
            ApplicableRegionSet regions = mgr.getApplicableRegions(location);
            StateFlag.State s = regions.queryState(null, FLY_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable e) {
            disable();
        }
        return true;
    }

    public static boolean isActive() {
        return active;
    }

    public static void disable() {
        active = false;
        wgPlugin = null;
        wgcfPlugin = null;
    }
}
