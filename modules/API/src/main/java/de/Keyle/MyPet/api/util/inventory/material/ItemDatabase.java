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

    Map<String, MaterialHolder> byID = new HashMap<>();
    Map<LegacyIdData, MaterialHolder> byLegacyId = new HashMap<>();
    Map<LegacyNamedData, MaterialHolder> byLegacyName = new HashMap<>();

    @Override
    public boolean onEnable() {
        try {
            loadFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onDisable() {
        byID.clear();
        byLegacyId.clear();
        byLegacyName.clear();
    }

    protected void loadFile() {
        // source: https://minecraftitemids.com/

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(MyPetApi.getPlugin().getResource("items.json"), StandardCharsets.UTF_8))) {
            Gson gson = new Gson();
            JsonArray obj = gson.fromJson(reader, JsonArray.class);
            obj.forEach(jsonElement -> loadEntry(jsonElement.getAsJsonObject()));
        } catch (Exception e) {
            e.printStackTrace();
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

            MaterialHolder materialHolder;

            short legacyData = 0;
            String legacyName = null;
            int legacyId = -1;
            if (entryObject.has("legacy-data")) {
                legacyData = Short.parseShort(entryObject.get("legacy-data").getAsString());
            }
            if (entryObject.has("legacy-name")) {
                legacyName = entryObject.get("legacy-name").getAsString();
            }
            if (entryObject.has("legacy-id")) {
                legacyId = Integer.parseInt(entryObject.get("legacy-id").getAsString());
            }
            if (legacyId >= 0) {
                if (legacyName != null) {
                    materialHolder = new MaterialHolder(introduced, id, legacyName, legacyId, legacyData);
                    byLegacyName.put(materialHolder.getLegacyName(), materialHolder);
                } else {
                    materialHolder = new MaterialHolder(introduced, id, legacyId, legacyData);
                }
                byLegacyId.put(materialHolder.getLegacyId(), materialHolder);
            } else {
                materialHolder = new MaterialHolder(introduced, id);
            }

            byID.put(id, materialHolder);
        }
    }

    public MaterialHolder getByID(String id) {
        id = id.toLowerCase();
        if (id.startsWith("minecraft:")) {
            id = id.substring(10);
        }
        return byID.get(id);
    }

    public MaterialHolder getByLegacyId(LegacyIdData legacyIdData) {
        return byLegacyId.get(legacyIdData);
    }

    public MaterialHolder getByLegacyId(int id, short data) {
        return getByLegacyId(new LegacyIdData(id, data));
    }

    public MaterialHolder getByLegacyId(int id) {
        return getByLegacyId(id, (short) 0);
    }

    public MaterialHolder getByLegacyName(LegacyNamedData legacyNamedData) {
        return byLegacyName.get(legacyNamedData);
    }

    public MaterialHolder getByLegacyName(String name, short data) {
        name = name.toLowerCase();
        if (name.startsWith("minecraft:")) {
            name = name.substring(10);
        }
        return getByLegacyName(new LegacyNamedData(name, data));
    }

    public MaterialHolder getByLegacyName(String name) {
        return getByLegacyName(name, (short) 0);
    }

    public Material getMaterialById(String id) {
        MaterialHolder materialHolder = getByID(id);
        if (materialHolder != null) {
            return MyPetApi.getPlatformHelper().getMaterial(materialHolder);
        }
        return null;
    }
}
