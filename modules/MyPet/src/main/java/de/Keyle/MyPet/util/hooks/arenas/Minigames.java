/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.hooks.arenas;

import com.pauldavdesign.mineauz.minigames.events.JoinMinigameEvent;
import com.pauldavdesign.mineauz.minigames.events.SpectateMinigameEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Minigames implements Listener {

    private static com.pauldavdesign.mineauz.minigames.Minigames plugin;
    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("Minigames", "com.pauldavdesign.mineauz.minigames.Minigames")) {
            plugin = PluginHookManager.getPluginInstance(com.pauldavdesign.mineauz.minigames.Minigames.class);
            Bukkit.getPluginManager().registerEvents(new Minigames(), MyPetApi.getPlugin());
            MyPetApi.getLogger().info("Minigames hook activated.");
            active = true;
        }
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
        if (active && Configuration.Hooks.DISABLE_PETS_IN_MINIGAMES && MyPetApi.getPlayerList().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerList().getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onSpectateMinigame(SpectateMinigameEvent event) {
        if (active && Configuration.Hooks.DISABLE_PETS_IN_MINIGAMES && MyPetApi.getPlayerList().isMyPetPlayer(event.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerList().getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(Translation.getString("Message.No.AllowedHere", player.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onMyPetCall(MyPetCallEvent event) {
        if (active && Configuration.Hooks.DISABLE_PETS_IN_MINIGAMES) {
            if (isInMinigame(event.getOwner())) {
                event.setCancelled(true);
            }
        }
    }
}
