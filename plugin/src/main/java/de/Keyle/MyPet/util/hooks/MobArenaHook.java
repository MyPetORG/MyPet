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

import com.garbagemule.MobArena.MobArenaHandler;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@PluginHookName(value = "MobArena", classPath = "com.garbagemule.MobArena.MobArena")
public class MobArenaHook implements PlayerVersusPlayerHook, AllowedHook {

    public static boolean ENABLED = true;
    public static boolean ALLOW_PETS = true;
    public static boolean RESPECT_PVP_RULE = true;
    
    protected MobArenaHandler mobArenaHandler;

    @Override
    public boolean onEnable() {
        if (ENABLED) {
            Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
            mobArenaHandler = new MobArenaHandler();
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void loadConfig(ConfigurationSection config) {
        config.addDefault("Enabled", ENABLED);
        config.addDefault("AllowPets", ALLOW_PETS);
        config.addDefault("RespectPvPRule", RESPECT_PVP_RULE);

        ENABLED = config.getBoolean("Enabled", true);
        ALLOW_PETS = config.getBoolean("AllowPets", true);
        RESPECT_PVP_RULE = config.getBoolean("RespectPvPRule", true);
    }

    @Override
    public boolean isPetAllowed(MyPetPlayer owner) {
        if (!ALLOW_PETS) {
            try {
                return !mobArenaHandler.isPlayerInArena(owner.getPlayer());
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        if (RESPECT_PVP_RULE) {
            try {
                if (mobArenaHandler.isPlayerInArena(defender)) {
                    return mobArenaHandler.getArenaWithPlayer(defender).getSettings().getBoolean("pvp-enabled", true);
                }
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    @EventHandler
    public void onJoinPvPArena(ArenaPlayerJoinEvent event) {
        if (!ALLOW_PETS) {
            if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
                MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
                if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                    player.getMyPet().removePet();
                    player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMyPetDamageInArena(EntityDamageByEntityEvent event) {
        MyPetBukkitEntity damager;

        if (event.getDamager() instanceof MyPetBukkitEntity) {
            damager = (MyPetBukkitEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof MyPetBukkitEntity) {
            damager = (MyPetBukkitEntity) ((Projectile) event.getDamager()).getShooter();
        } else {
            return;
        }
        if (!isPetAllowed(damager.getOwner())) {
            event.setCancelled(false);
        }
    }
}