/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks.arenas;

import com.garbagemule.MobArena.MobArenaHandler;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobArena implements Listener {

    private static MobArenaHandler arenaHandler;
    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("MobArena", "com.garbagemule.MobArena.MobArena")) {
            Bukkit.getPluginManager().registerEvents(new MobArena(), MyPetApi.getPlugin());
            arenaHandler = new MobArenaHandler();
            MyPetApi.getLogger().info("MobArena hook activated.");
            active = true;
        }
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
        if (active && Configuration.Hooks.DISABLE_PETS_IN_MOB_ARENA && MyPetApi.getPlayerList().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerList().getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMyPetDamageInArena(EntityDamageByEntityEvent event) {
        if (!active) {
            return;
        }
        MyPetBukkitEntity damager;

        if (event.getDamager() instanceof MyPetBukkitEntity) {
            damager = (MyPetBukkitEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof MyPetBukkitEntity) {
            damager = (MyPetBukkitEntity) ((Projectile) event.getDamager()).getShooter();
        } else {
            return;
        }
        if (isInMobArena(damager.getOwner())) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onMyPetCall(MyPetCallEvent event) {
        if (active && Configuration.Hooks.DISABLE_PETS_IN_MOB_ARENA) {
            if (isInMobArena(event.getOwner())) {
                event.setCancelled(true);
            }
        }
    }
}