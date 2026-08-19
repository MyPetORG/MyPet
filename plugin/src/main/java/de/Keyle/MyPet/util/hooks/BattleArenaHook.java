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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.locale.Locale;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.event.player.ArenaJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

@ServiceName("BattleArena")
@RequiresPlugin("BattleArena")
@Load(Load.State.Hooks)
public class BattleArenaHook implements AllowedHook {

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
            Player p = owner.getPlayer();
            return ArenaPlayer.arenaPlayer(p).isEmpty();
        } catch (Throwable ignored) {
        }
        return true;
    }

    @EventHandler
    public void onJoinBattleArena(ArenaJoinEvent event) {
        Player joined = event.getArenaPlayer().getPlayer();
        if (MyPetApi.getPlayerManager().isMyPetPlayer(joined.getName())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(joined);
            boolean despawnedAny = false;
            for (Pet pet : player.getPets()) {
                if (pet.getStatus() == Pet.PetState.Here) {
                    pet.removePet();
                    despawnedAny = true;
                }
            }
            if (despawnedAny) {
                player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }
}