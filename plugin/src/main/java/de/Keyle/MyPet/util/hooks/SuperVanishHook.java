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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;

@PluginHookName("SuperVanish")
public class SuperVanishHook implements PlayerVersusPlayerHook {

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
    public boolean canHurt(Player attacker, Player defender) {
        try {
            return !VanishAPI.isInvisible(defender);
        } catch (Throwable ignored) {
        }
        return true;
    }

    @EventHandler
    public void on(MyPetCallEvent e) {
        try {
            if (VanishAPI.isInvisible(e.getOwner().getPlayer())) {
                e.setCancelled(true);
            }
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(PlayerHideEvent e) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(e.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(e.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Here) {
                player.getMyPet().removePet(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(PlayerShowEvent e) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(e.getPlayer())) {
            MyPetPlayer player = MyPetApi.getPlayerManager().getMyPetPlayer(e.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == MyPet.PetState.Despawned && player.getMyPet().wantsToRespawn()) {
                player.getMyPet().createEntity();
            }
        }
    }
}