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

package de.Keyle.MyPet.api.util.inventory.material;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ServiceName("ItemDatabase")
public class ItemDatabase implements ServiceContainer {

    Map<String, Material> byID = new HashMap<>();

    @Override
    public boolean onEnable() {
        try {
            loadFile();
        } catch (Exception e) {
            ErrorUtil.report(e);
        }
        return true;
    }

    @Override
    public void onDisable() {
        byID.clear();
    }

    protected void loadFile() {
        // source: https://minecraftitemids.com/

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(MyPetApi.getPlugin().getResource("items.json"), StandardCharsets.UTF_8))) {
            Gson gson = new Gson();
            JsonArray obj = gson.fromJson(reader, JsonArray.class);
            obj.forEach(jsonElement -> loadEntry(jsonElement.getAsJsonObject()));
        } catch (Exception e) {
            ErrorUtil.report(e);
        }
    }

    protected void loadEntry(JsonObject entryObject) {
        String introduced = entryObject.get("introduced").getAsString();
        if (MyPetApi.getCompatUtil().isCompatible(introduced)) {
            if (entryObject.has("last-used")) {
                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion(entryObject.get("last-used").getAsString()) > 0) {
                    return;
                }
            }
            String id = entryObject.get("id").getAsString();
            Material material = Material.matchMaterial(id.toUpperCase());
            if (material != null) {
                byID.put(id, material);
            }
        }
    }

    public Material getByID(String id) {
        id = id.toLowerCase();
        if (id.startsWith("minecraft:")) {
            id = id.substring(10);
        }
        return byID.get(id);
    }
}
