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

package de.Keyle.MyPet.util.hooks;

import com.nisovin.magicspells.events.SpellTargetEvent;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.util.hooks.PluginHookManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MagicSpellsHook implements Listener {
    private static boolean active = false;

    public static void findPlugin() {
        if (PluginHookManager.isPluginUsable("MagicSpells")) {
            active = true;
            Bukkit.getPluginManager().registerEvents(new MagicSpellsHook(), MyPetApi.getPlugin());
        }
    }

    @EventHandler
    public void onPlayerExpGain(SpellTargetEvent event) {
        if (event.getTarget() instanceof MyPetBukkitEntity) {
            if (((MyPetBukkitEntity) event.getTarget()).getOwner().equals(event.getCaster())) {
                event.setCancelled(true);
            } else if (!PvPChecker.canHurt(event.getCaster(), ((MyPetBukkitEntity) event.getTarget()).getOwner().getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

    public static boolean isActive() {
        return active;
    }
}