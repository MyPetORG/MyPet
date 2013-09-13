/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.util.support;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import me.kitskub.hungergames.HungerGames;
import me.kitskub.hungergames.api.GameManager;
import me.kitskub.hungergames.api.event.PlayerJoinGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyHungerGames implements Listener {
    public static boolean DISABLE_PETS_IN_HUNGER_GAMES = true;

    private static boolean active = false;
    private static GameManager gameManager;

    public static void findPlugin() {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("MyHungerGames")) {
            Bukkit.getPluginManager().registerEvents(new MyHungerGames(), MyPetPlugin.getPlugin());
            gameManager = HungerGames.getInstance().getGameManager();
            active = true;
        }
        DebugLogger.info("MyHungerGames support " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInHungerGames(MyPetPlayer owner) {
        if (active) {
            try {
                return gameManager.getSpectating(owner.getPlayer()) != null || HungerGames.getInstance().getGameManager().getRawPlayingSession(owner.getPlayer()) != null;
            } catch (Exception e) {
                active = false;
            }
        }
        return false;
    }

    @EventHandler
    public void onJoinPvPArena(PlayerJoinGameEvent event) {
        if (active && DISABLE_PETS_IN_HUNGER_GAMES && MyPetPlayer.isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }
}