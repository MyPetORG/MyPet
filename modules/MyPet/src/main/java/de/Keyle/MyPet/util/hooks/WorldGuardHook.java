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
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.FlyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

@PluginHookName("WorldGuard")
public class WorldGuardHook implements PlayerVersusPlayerHook, FlyHook {
    public static final StateFlag FLY_FLAG = new StateFlag("mypet-fly", false);

    protected WorldGuardPlugin wgp = null;
    protected WorldGuardCustomFlagsHook flagHook = null;
    protected boolean customFlags = false;

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.USE_WorldGuard) {
            wgp = MyPetApi.getPluginHookManager().getPluginInstance(WorldGuardPlugin.class).get();

            try {
                FlagRegistry flagRegistry = wgp.getFlagRegistry();
                flagRegistry.register(FLY_FLAG);
                customFlags = true;
            } catch (Exception ignored) {
            }

            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
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

    public boolean canFly(Location location) {
        if (customFlags) {
            RegionManager mgr = wgp.getRegionManager(location.getWorld());
            ApplicableRegionSet regions = mgr.getApplicableRegions(location);
            StateFlag.State s = regions.queryState(null, FLY_FLAG);
            return s == null || s != StateFlag.State.ALLOW;
        }

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
        } catch (Throwable ignored) {
        }
        return true;
    }

    public void enableFlagSupport(WorldGuardCustomFlagsHook hook) {
        flagHook = hook;
    }

    public void disableFlagSupport() {
        flagHook = null;
    }

    public WorldGuardPlugin getPlugin() {
        return wgp;
    }
}