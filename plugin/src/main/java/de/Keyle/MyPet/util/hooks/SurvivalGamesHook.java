/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.locale.Locale;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

@ServiceName("SurvivalGames")
@RequiresPlugin(value = "SurvivalGames", classPath = "com.thundergemios10.survivalgames.SurvivalGames")
@Load(Load.State.Hooks)
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
            if (player.hasPet() && player.getPet().getStatus() == Pet.PetState.Here) {
                player.getPet().removePet();
                player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }
}