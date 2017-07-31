/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.ArenaHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.mcsg.survivalgames.GameManager;
import org.mcsg.survivalgames.api.PlayerJoinArenaEvent;

@PluginHookName(value = "SurvivalGames", classPath = "org.mcsg.survivalgames.SurvivalGames")
public class SurvivalGamesHook implements ArenaHook {

    @Override
    public boolean onEnable() {
        if (Configuration.Hooks.DISABLE_PETS_IN_SURVIVAL_GAMES) {
            Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean isInArena(MyPetPlayer owner) {
        try {
            return GameManager.getInstance().getPlayerGameId(owner.getPlayer()) != -1 && GameManager.getInstance().isPlayerActive(owner.getPlayer());
        } catch (Throwable ignored) {
        }
        return false;
    }

    @EventHandler
    public void onJoinPvPArena(PlayerJoinArenaEvent event) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                player.getMyPet().removePet();
                player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onMyPetCall(MyPetCallEvent event) {
        if (isInArena(event.getOwner())) {
            event.setCancelled(true);
        }
    }
}