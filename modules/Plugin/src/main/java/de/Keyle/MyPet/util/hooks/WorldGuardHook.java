/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagSettings;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

@PluginHookName("WorldGuard")
public class WorldGuardHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook, AllowedHook {
    public static final StateFlag DAMAGE_FLAG = new StateFlag("mypet-damage", false);
    public static final StateFlag DENY_FLAG = new StateFlag("mypet-deny", false);
    public static final StateFlag LEASH_FLAG = new StateFlag("mypet-leash", true);

    protected WorldGuardPlugin wgp = null;
    protected boolean customFlags = false;

    public WorldGuardHook() {
        if (Configuration.Hooks.USE_WorldGuard) {
            wgp = MyPetApi.getPluginHookManager().getPluginInstance(WorldGuardPlugin.class).get();

            try {
                FlagRegistry flagRegistry = wgp.getFlagRegistry();
                flagRegistry.register(DAMAGE_FLAG);
                flagRegistry.register(DENY_FLAG);
                flagRegistry.register(LEASH_FLAG);

                MyPetApi.getLeashFlagManager().registerLeashFlag(new RegionFlag());
                customFlags = true;
            } catch (NoSuchMethodError ignored) {
            }
        }
    }

    @Override
    public boolean onEnable() {
        if (customFlags && Configuration.Hooks.USE_WorldGuard) {
            Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
        }
        return Configuration.Hooks.USE_WorldGuard;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        MyPetApi.getLeashFlagManager().removeFlag("WorldGuard");
    }

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        if (customFlags) {
            try {
                Location location = defender.getLocation();
                RegionManager mgr = wgp.getRegionManager(location.getWorld());

                ApplicableRegionSet set = mgr.getApplicableRegions(location);
                StateFlag.State s;
                if (defender instanceof Animals) {
                    s = set.queryState(null, DefaultFlag.DAMAGE_ANIMALS, DAMAGE_FLAG);
                } else {
                    s = set.queryState(null, DAMAGE_FLAG);
                }
                return s == null || s == StateFlag.State.ALLOW;
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Location location = defender.getLocation();
            RegionManager mgr = wgp.getRegionManager(location.getWorld());
            ApplicableRegionSet set = mgr.getApplicableRegions(location);
            StateFlag.State s;
            if (customFlags) {
                s = set.queryState(wgp.wrapPlayer(defender), DefaultFlag.PVP, DAMAGE_FLAG);
            } else {
                s = set.queryState(wgp.wrapPlayer(defender), DefaultFlag.PVP);
            }
            return s == null || s == StateFlag.State.ALLOW;
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean isPetAllowed(MyPetPlayer player) {
        if (customFlags) {
            Player p = player.getPlayer();
            RegionManager mgr = wgp.getRegionManager(p.getWorld());
            ApplicableRegionSet regions = mgr.getApplicableRegions(p.getLocation());
            StateFlag.State s = regions.queryState(null, DENY_FLAG);
            return s == null || s == StateFlag.State.ALLOW;
        }
        return true;
    }

    @EventHandler
    public void on(MyPetCallEvent event) {
        if (!isPetAllowed(event.getOwner())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent event) {
        if (customFlags) {
            if (event.getFrom().getBlock() != event.getTo().getBlock()) {
                if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                    MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                    if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                        if (!isPetAllowed(player)) {
                            player.getMyPet().removePet(true);
                            player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
                        }
                    }
                }
            }
        }
    }

    @LeashFlagName("WorldGuard")
    class RegionFlag implements LeashFlag {
        @Override
        public boolean check(Player player, LivingEntity entity, double damage, LeashFlagSettings settings) {
            Location loc = entity.getLocation();
            RegionManager mgr = wgp.getRegionManager(loc.getWorld());
            ApplicableRegionSet regions = mgr.getApplicableRegions(loc);
            StateFlag.State s = regions.queryState(null, LEASH_FLAG);

            return s == null || s == StateFlag.State.ALLOW;
        }
    }
}