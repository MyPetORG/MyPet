/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.util.hooks.PluginHookManager;
import de.Keyle.MyPet.util.locale.Translation;
import de.Keyle.MyPet.util.logger.DebugLogger;
import mc.alk.arena.events.players.ArenaPlayerEnterEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BattleArena implements Listener {
    public static boolean DISABLE_PETS_IN_ARENA = true;

    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("BattleArena")) {
            Bukkit.getPluginManager().registerEvents(new BattleArena(), MyPetPlugin.getPlugin());
            active = true;
        }
        DebugLogger.info("BattleArena hook " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInBattleArena(MyPetPlayer owner) {
        if (active) {
            try {
                Player p = owner.getPlayer();
                return mc.alk.arena.BattleArena.inArena(p) && mc.alk.arena.BattleArena.inCompetition(p);
            } catch (Exception e) {
                active = false;
            }
        }
        return false;
    }

    @EventHandler
    public void onJoinBattleArena(ArenaPlayerEnterEvent event) {
        if (active && DISABLE_PETS_IN_ARENA && PlayerList.isMyPetPlayer(event.getPlayer().getName())) {
            MyPetPlayer player = PlayerList.getMyPetPlayer(event.getPlayer().getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }
}
