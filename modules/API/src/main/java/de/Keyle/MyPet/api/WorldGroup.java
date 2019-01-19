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

import lombok.Getter;
import org.bukkit.World;

import java.util.*;

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
}