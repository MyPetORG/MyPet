/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

package de.Keyle.MyPet.api.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Material;

@ServiceName("ItemDatabase")
public class ItemDatabase implements ServiceContainer {
    BiMap<String, Integer> name2id = HashBiMap.create();
    BiMap<Integer, String> id2name = name2id.inverse();

    @Override
    public boolean onEnable() {
        loadFile();
        return true;
    }

    @Override
    public void onDisable() {
        name2id.clear();
    }

    protected void loadFile() {
        // source: http://minecraft-ids.grahamedgecombe.com/items.tsv
        // remove diplicate lines from CSV: ^(.*?)$\s+?^(?=.*^\1$)
        String items = Util.convertStreamToString(MyPetApi.getPlugin().getResource("items.csv"));
        for (String line : items.split("\n")) {
            if (line.startsWith("#")) {
                continue;
            }
            String data[] = line.split(",");
            String name = data[1];
            int id = Integer.parseInt(data[0]);
            name2id.put(name, id);
        }
    }

    public Material getMaterial(String name) {
        if (name.startsWith("minecraft:")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        if (name2id.containsKey(name)) {
            int id = name2id.get(name);
            return Material.getMaterial(id);
        }
        return null;
    }

    public int getID(String name) {
        if (name.startsWith("minecraft:")) {
            name = name.substring(name.indexOf(':'));
        }
        if (name2id.containsKey(name)) {
            return name2id.get(name);
        }
        return 0;
    }

    public String getName(int id) {
        if (id2name.containsKey(id)) {
            return id2name.get(id);
        }
        return null;
    }
}
