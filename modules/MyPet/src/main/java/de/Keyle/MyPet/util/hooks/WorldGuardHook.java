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
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.bukkit.Location;

import java.util.Map;
import java.util.Set;

public class WorldGuardHook {
    private static boolean active = false;
    private static WorldGuardPlugin wgp = null;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("WorldGuard")) {
            active = true;
            wgp = PluginHookManager.getPluginInstance(WorldGuardPlugin.class).get();
            MyPetApi.getLogger().info("WorldGuard hook activated.");
        }
    }

    public static boolean canFly(Location location) {
        try {
            Map<String, Boolean> flyZones = Configuration.Skilltree.Skill.Ride.FLY_ZONES;
            boolean allowed = true;

            if (flyZones.size() > 0) {
                RegionManager mgr = wgp.getRegionManager(location.getWorld());
                ApplicableRegionSet set = mgr.getApplicableRegions(location);
                int priority = Integer.MIN_VALUE;
                Set<ProtectedRegion> regions = set.getRegions();
                regions.add(mgr.getRegion("__global__"));

                for (ProtectedRegion region : regions) {
                    String zone = location.getWorld().getName() + "::" + region.getId();
                    if (flyZones.containsKey(zone)) {
                        if (flyZones.get(zone)) {
                            if (region.getPriority() > priority) {
                                priority = region.getPriority();
                                allowed = true;
                            }
                        } else {
                            if (region.getPriority() >= priority) {
                                priority = region.getPriority();
                                allowed = false;
                            }
                        }
                    }
                }
            }

            return allowed;
        } catch (Throwable e) {
            active = false;
        }
        return true;
    }

    public static boolean isActive() {
        return active;
    }

    public static void disable() {
        active = false;
        wgp = null;
    }
}
