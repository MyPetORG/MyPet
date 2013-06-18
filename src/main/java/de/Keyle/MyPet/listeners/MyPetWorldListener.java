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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.MyPetWorldGroup;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class MyPetWorldListener implements Listener
{
    @EventHandler
    public void onWorldInit(final WorldInitEvent event)
    {
        MyPetWorldGroup defaultGroup = MyPetWorldGroup.getGroup("default");
        if (defaultGroup != null)
        {
            if (defaultGroup.addWorld(event.getWorld().getName()))
            {
                MyPetPlugin.getPlugin().getConfig().set("Groups.default", defaultGroup.getWorlds());
                MyPetPlugin.getPlugin().saveConfig();
                MyPetLogger.write("added " + ChatColor.GOLD + event.getWorld().getName() + ChatColor.RESET + " to 'default' group.");
            }
        }
    }
}