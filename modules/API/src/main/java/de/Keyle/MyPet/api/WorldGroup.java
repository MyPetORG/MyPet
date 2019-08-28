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

package de.Keyle.MyPet.api;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.configuration.ConfigurationYAML;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class WorldGroup {

    private static Map<String, WorldGroup> allGroups = new HashMap<>();
    private static Map<String, WorldGroup> groupWorlds = new HashMap<>();

    public static final WorldGroup DEFAULT_GROUP = new WorldGroup("default", false);
    public static final WorldGroup DISABLED_GROUP = new WorldGroup("---DISABLED---", true);

    private String name;
    @Getter private boolean disabled;
    private List<String> worlds = new ArrayList<>();


    public WorldGroup(String groupName, boolean disabled) {
        this.name = groupName.toLowerCase();
        this.disabled = disabled;
        if (allGroups.containsKey(this.getName())) {
            return;
        }
        allGroups.put(this.getName(), this);
    }

    public boolean addWorld(String world) {
        if (groupWorlds.containsKey(world)) {
            return false;
        }
        if (!this.worlds.contains(world)) {
            this.worlds.add(world);
            groupWorlds.put(world, this);
            return true;
        }
        return false;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getWorlds() {
        return this.worlds;
    }

    @Override
    public String toString() {
        return "WorldGroup{name=" + name + ", worlds=" + worlds + "}";
    }

    /**
     * Checks whether a world group contains the world
     *
     * @param worldName The name of the checked world
     * @return boolean
     */
    public boolean containsWorld(String worldName) {
        return this.worlds.contains(worldName);
    }

    /**
     * Returns all available world groups
     *
     * @return Collection<WorldGroup>
     */
    public static Collection<WorldGroup> getGroups() {
        return Collections.unmodifiableCollection(allGroups.values());
    }

    /**
     * Returns the group the world is in
     *
     * @param name World
     * @return WorldGroup
     */
    public static WorldGroup getGroupByWorld(String name) {
        WorldGroup group = groupWorlds.get(name);
        if (group == null) {
            return DISABLED_GROUP;
        }
        return group;
    }

    /**
     * Returns the group the world is in
     *
     * @param world World
     * @return WorldGroup
     */
    public static WorldGroup getGroupByWorld(World world) {
        WorldGroup group = groupWorlds.get(world.getName());
        if (group == null) {
            return DISABLED_GROUP;
        }
        return group;
    }

    /**
     * Returns the group by name
     *
     * @param name World
     * @return WorldGroup
     */
    public static WorldGroup getGroupByName(String name) {
        WorldGroup group = allGroups.get(name);
        if (group == null) {
            return DISABLED_GROUP;
        }
        return group;
    }

    /**
     * Removes all worlds from the groups and then deletes the groups
     */
    public static void clearGroups() {
        allGroups.clear();
        groupWorlds.clear();
        allGroups.put(DEFAULT_GROUP.getName(), DEFAULT_GROUP);
        allGroups.put(DISABLED_GROUP.getName(), DISABLED_GROUP);
    }

    public static void loadGroups(File f) {
        MyPetApi.getLogger().info("Loading WorldGroups...");

        ConfigurationYAML yamlConfiguration = new ConfigurationYAML(f);
        FileConfiguration config = yamlConfiguration.getConfig();

        WorldGroup.clearGroups();

        if (config == null) {
            return;
        }

        config.options().header("" +
                "######################################################################\n" +
                "          This is the world group configuration of MyPet             #\n" +
                "                You can find more info on the wiki:                  #\n" +
                "  https://wiki.mypet-plugin.de/setup/configurations/worldgroups.yml  #\n" +
                "######################################################################\n");
        config.options().copyHeader(true);

        Set<String> groups;
        Set<String> disabledWorlds = new HashSet<>();
        try {
            groups = config.getConfigurationSection("Groups").getKeys(false);
        } catch (NullPointerException e) {
            groups = new HashSet<>();
        }
        if (config.contains("Disabled")) {
            disabledWorlds.addAll(config.getStringList("Disabled"));
        } else {
            config.set("Disabled", new String[]{"example_world"});
            yamlConfiguration.saveConfig();
        }

        for (String world : disabledWorlds) {
            if (Bukkit.getServer().getWorld(world) != null) {
                if (WorldGroup.DISABLED_GROUP.addWorld(world)) {
                    MyPetApi.getLogger().info("   disabled MyPet in '" + world + "'");
                }
            }
        }
        groups = groups.stream().filter(s -> !s.equals("default")).collect(Collectors.toSet());
        for (String node : groups) {
            List<String> worlds = config.getStringList("Groups." + node);
            if (worlds.size() > 0) {
                WorldGroup newGroup = new WorldGroup(node, false);
                for (String world : worlds) {
                    if (Bukkit.getServer().getWorld(world) != null) {
                        if (newGroup.addWorld(world)) {
                            MyPetApi.getLogger().info("   added '" + ChatColor.GOLD + world + ChatColor.RESET + "' to '" + newGroup.getName() + "'");
                        }
                    }
                }
            }
        }

        List<String> worldNames = new ArrayList<>();
        boolean newWorldFound = false;
        List<String> worlds = config.getStringList("Groups." + DEFAULT_GROUP.name);
        if (worlds.size() > 0) {
            for (String world : worlds) {
                if (Bukkit.getServer().getWorld(world) != null) {
                    if (DEFAULT_GROUP.addWorld(world)) {
                        MyPetApi.getLogger().info("   added '" + ChatColor.GOLD + world + ChatColor.RESET + "' to '" + DEFAULT_GROUP.getName() + "'");
                        worldNames.add(world);
                    }
                }
            }
        }
        for (World world : Bukkit.getServer().getWorlds()) {
            if (WorldGroup.DEFAULT_GROUP.addWorld(world.getName())) {
                MyPetApi.getLogger().info("   added " + ChatColor.GOLD + world.getName() + ChatColor.RESET + " to 'default' group.");
                worldNames.add(world.getName());
                newWorldFound = true;
            }
        }
        if (newWorldFound && worldNames.size() > 0) {
            config.set("Groups.default", worldNames);
            yamlConfiguration.saveConfig();
        }
    }
}