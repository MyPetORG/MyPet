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
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.util.hooks.PluginHookManager;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import net.slipcor.pvparena.api.PVPArenaAPI;
import net.slipcor.pvparena.events.PAJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PvPArena implements Listener {
    public static boolean DISABLE_PETS_IN_ARENA = true;

    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("pvparena")) {
            Bukkit.getPluginManager().registerEvents(new PvPArena(), MyPetPlugin.getPlugin());
            active = true;
        }
        DebugLogger.info("PvPArena hook " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInPvPArena(MyPetPlayer owner) {
        if (active) {
            try {
                return !PVPArenaAPI.getArenaName(owner.getPlayer()).equals("");
            } catch (Exception e) {
                active = false;
            }
        }
        return false;
    }

    @EventHandler
    public void onJoinPvPArena(PAJoinEvent event) {
        if (active && DISABLE_PETS_IN_ARENA && PlayerList.isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = PlayerList.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }
}
