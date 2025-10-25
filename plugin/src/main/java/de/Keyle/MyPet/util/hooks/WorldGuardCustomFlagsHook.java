/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import com.sk89q.worldguard.protection.flags.StateFlag;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.FlyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@PluginHookName("WGCustomFlags")
public class WorldGuardCustomFlagsHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook, FlyHook {

    protected WorldGuardHook wgHook = null;

    @Override
    public boolean onEnable() {
        try {
            if (MyPetApi.getPluginHookManager().isPluginUsable("WorldGuard")) {
                wgHook = MyPetApi.getPluginHookManager().getHook(WorldGuardHook.class);

                WGCustomFlagsPlugin wgcfPlugin = MyPetApi.getPluginHookManager().getPluginInstance(WGCustomFlagsPlugin.class).get();
                wgcfPlugin.addCustomFlag(WorldGuardHook.FLY_FLAG);
                wgcfPlugin.addCustomFlag(WorldGuardHook.DAMAGE_FLAG);
                wgcfPlugin.addCustomFlag(WorldGuardHook.DENY_FLAG);
                wgcfPlugin.addCustomFlag(WorldGuardHook.LEASH_FLAG);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            Location location = defender.getLocation();
            StateFlag.State s = wgHook.getState(location, null, WorldGuardHook.DAMAGE_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Location location = defender.getLocation();
            StateFlag.State s = wgHook.getState(location, defender, WorldGuardHook.DAMAGE_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable ignored) {
        }
        return true;
    }

    public boolean canFly(Location location) {
        try {
            StateFlag.State s = wgHook.getState(location, null, WorldGuardHook.FLY_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable ignored) {
        }
        return true;
    }
}