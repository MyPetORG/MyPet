/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util.support.arenas;

import com.pauldavdesign.mineauz.minigames.events.JoinMinigameEvent;
import com.pauldavdesign.mineauz.minigames.events.SpectateMinigameEvent;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.Keyle.MyPet.util.support.PluginSupportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Minigames implements Listener {
    public static boolean DISABLE_PETS_IN_MINIGAMES = true;

    private static com.pauldavdesign.mineauz.minigames.Minigames plugin;
    private static boolean active = false;

    public static void findPlugin() {
        if (PluginSupportManager.isPluginUsable("Minigames")) {
            plugin = PluginSupportManager.getPluginInstance(com.pauldavdesign.mineauz.minigames.Minigames.class);
            Bukkit.getPluginManager().registerEvents(new Minigames(), MyPetPlugin.getPlugin());
            active = true;
        }
        DebugLogger.info("Minigames support " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInMinigame(MyPetPlayer owner) {
        if (active) {
            try {
                if (plugin != null) {
                    Player p = owner.getPlayer();
                    return plugin.pdata.playersInMinigame().contains(p);
                }
            } catch (Exception e) {
                active = false;
            }
        }
        return false;
    }

    @EventHandler
    public void onJoinMinigame(JoinMinigameEvent event) {
        if (active && DISABLE_PETS_IN_MINIGAMES && MyPetPlayer.isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onSpectateMinigame(SpectateMinigameEvent event) {
        if (active && DISABLE_PETS_IN_MINIGAMES && MyPetPlayer.isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Locales.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }
}
