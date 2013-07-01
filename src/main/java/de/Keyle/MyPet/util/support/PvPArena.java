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
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import net.slipcor.pvparena.api.PVPArenaAPI;
import net.slipcor.pvparena.events.PAJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PvPArena implements Listener
{
    public static boolean DISABLE_PETS_IN_ARENA = true;

    private static boolean active = false;

    public static void findPlugin()
    {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("pvparena"))
        {
            Bukkit.getPluginManager().registerEvents(new PvPArena(), MyPetPlugin.getPlugin());
            active = true;
        }
        DebugLogger.info("PvPArena support " + (active ? "" : "not ") + "activated.");
    }

    public static boolean isInPvPArena(MyPetPlayer owner)
    {
        if (active)
        {
            Player p = owner.getPlayer();
            return !PVPArenaAPI.getArenaName(p).equals("");
        }
        return false;
    }

    @EventHandler
    public void onJoinPvPArena(PAJoinEvent event)
    {
        if (DISABLE_PETS_IN_ARENA && MyPetPlayer.isMyPetPlayer(event.getPlayer()))
        {
            MyPetPlayer player = MyPetPlayer.getMyPetPlayer(event.getPlayer());
            if (player.hasMyPet() && player.getMyPet().getStatus() == PetState.Here)
            {
                player.getMyPet().removePet(true);
                player.getPlayer().sendMessage(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.NotAllowedHere", player.getPlayer())));
            }
        }
    }
}
