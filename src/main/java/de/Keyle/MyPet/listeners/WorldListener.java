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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.configuration.ConfigurationYAML;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import java.io.File;

public class WorldListener implements Listener {
    @EventHandler
    public void onWorldInit(final WorldInitEvent event) {
        if (WorldGroup.getGroupByWorld(event.getWorld().getName()) == null) {
            WorldGroup defaultGroup = WorldGroup.getGroupByName("default");
            if (defaultGroup == null) {
                defaultGroup = new WorldGroup("default");
                defaultGroup.registerGroup();
            }
            if (defaultGroup.addWorld(event.getWorld().getName())) {
                File groupsFile = new File(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "worldgroups.yml");
                ConfigurationYAML yamlConfiguration = new ConfigurationYAML(groupsFile);
                FileConfiguration config = yamlConfiguration.getConfig();
                config.set("Groups.default", defaultGroup.getWorlds());
                yamlConfiguration.saveConfig();
                MyPetLogger.write("added " + ChatColor.YELLOW + event.getWorld().getName() + ChatColor.RESET + " to '" + ChatColor.YELLOW + "default" + ChatColor.RESET + "' group.");
            } else {
                MyPetLogger.write("An error occured while adding " + ChatColor.YELLOW + event.getWorld().getName() + ChatColor.RESET + " to '" + ChatColor.YELLOW + "default" + ChatColor.RESET + "' group. Please restart the server.");
            }
        }
    }
}