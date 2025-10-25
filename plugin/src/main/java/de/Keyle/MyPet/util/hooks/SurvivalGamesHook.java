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

import com.thundergemios10.survivalgames.GameManager;
import com.thundergemios10.survivalgames.api.PlayerJoinArenaEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

@PluginHookName(value = "SurvivalGames", classPath = "com.thundergemios10.survivalgames.SurvivalGames")
public class SurvivalGamesHook implements AllowedHook {

    @Override
    public boolean onEnable() {
            Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
            return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean isPetAllowed(MyPetPlayer owner) {
        try {
            return GameManager.getInstance().getPlayerGameId(owner.getPlayer()) == -1 || !GameManager.getInstance().isPlayerActive(owner.getPlayer());
        } catch (Throwable ignored) {
        }
        return true;
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
}