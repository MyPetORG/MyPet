/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util.support.arenas;

import com.garbagemule.MobArena.MobArenaHandler;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.MyPetEntity;
import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.support.PluginSupportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobArena implements Listener {
    public static boolean DISABLE_PETS_IN_ARENA = true;

    private static MobArenaHandler arenaHandler;
    private static boolean active = false;

    public static void findPlugin() {
        if (PluginSupportManager.isPluginUsable("MobArena")) {
            Bukkit.getPluginManager().registerEvents(new MobArena(), MyPetPlugin.getPlugin());
            arenaHandler = new MobArenaHandler();
            active = true;
        }
        DebugLogger.info("MobArena support " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInMobArena(MyPetPlayer owner) {
        if (active && arenaHandler != null) {
            try {
                return arenaHandler.isPlayerInArena(owner.getPlayer());
            } catch (Exception e) {
                active = false;
            }
        }
        return false;
    }

    @EventHandler
    public void onJoinPvPArena(ArenaPlayerJoinEvent event) {
        if (active && DISABLE_PETS_IN_ARENA && MyPetPlayer.isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMyPetDamageInArena(EntityDamageByEntityEvent event) {
        if (!active) {
            return;
        }
        MyPetEntity damager;

        if (event.getDamager() instanceof MyPetEntity) {
            damager = (CraftMyPet) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof MyPetEntity) {
            damager = (CraftMyPet) ((Projectile) event.getDamager()).getShooter();
        } else {
            return;
        }
        if (isInMobArena(damager.getOwner())) {
            event.setCancelled(false);
        }
    }
}