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

package de.Keyle.MyPet.util.hooks.arenas;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.entity.MyPet;
import me.maker56.survivalgames.events.UserLobbyJoinedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static me.maker56.survivalgames.SurvivalGames.getUserManager;

public class UltimateSurvivalGames implements Listener {
    public static boolean DISABLE_PETS_IN_SURVIVAL_GAMES = true;

    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("SurvivalGames", "me.maker56.survivalgames.SurvivalGames")) {
            Bukkit.getPluginManager().registerEvents(new UltimateSurvivalGames(), MyPetApi.getPlugin());
            MyPetApi.getLogger().info("Ultimate Survival Games hook activated.");
            active = true;
        }
    }

    public static boolean isInSurvivalGames(MyPetPlayer owner) {
        if (active) {
            try {
                return getUserManager().isPlaying(owner.getPlayer().getName()) || getUserManager().isSpectator(owner.getPlayer().getName());
            } catch (Throwable e) {
                active = false;
            }
        }
        return false;
    }

    @EventHandler
    public void onJoinPvPArena(UserLobbyJoinedEvent event) {
        if (active && DISABLE_PETS_IN_SURVIVAL_GAMES && MyPetApi.getPlayerManager().isMyPetPlayer(event.getUser().getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(event.getUser().getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onMyPetCall(MyPetCallEvent event) {
        if (active && Configuration.Hooks.DISABLE_PETS_IN_SURVIVAL_GAMES) {
            if (isInSurvivalGames(event.getOwner())) {
                event.setCancelled(true);
            }
        }
    }
}