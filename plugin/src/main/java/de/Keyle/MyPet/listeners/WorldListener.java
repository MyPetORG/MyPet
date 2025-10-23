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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.util.configuration.ConfigurationYAML;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import java.io.File;

public class WorldListener implements Listener {
    @EventHandler
    public void on(WorldInitEvent event) {
        if (WorldGroup.getGroupByWorld(event.getWorld().getName()) == WorldGroup.DISABLED_GROUP) {
            if (WorldGroup.DEFAULT_GROUP.addWorld(event.getWorld().getName())) {
                File groupsFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "worldgroups.yml");
                ConfigurationYAML yamlConfiguration = new ConfigurationYAML(groupsFile);
                FileConfiguration config = yamlConfiguration.getConfig();
                config.set("Groups.default", WorldGroup.DEFAULT_GROUP.getWorlds());
                yamlConfiguration.saveConfig();
                MyPetApi.getLogger().info("added " + ChatColor.YELLOW + event.getWorld().getName() + ChatColor.RESET + " to '" + ChatColor.YELLOW + "default" + ChatColor.RESET + "' group.");
            } else {
                MyPetApi.getLogger().warning("An error occured while adding " + ChatColor.YELLOW + event.getWorld().getName() + ChatColor.RESET + " to '" + ChatColor.YELLOW + "default" + ChatColor.RESET + "' group. Please restart the server.");
            }
        }
    }
}