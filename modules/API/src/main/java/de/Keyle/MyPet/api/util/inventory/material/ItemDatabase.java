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

package de.Keyle.MyPet.api.util.inventory.material;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Material;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                for (Object o : (JSONArray) obj) {
                    JSONObject entryObject = (JSONObject) o;
                    loadEntry(entryObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void loadEntry(JSONObject entryObject) {
        String introduced = entryObject.get("introduced").toString();
        if (MyPetApi.getCompatUtil().isCompatible(introduced)) {
            String id = entryObject.get("id").toString();

            MaterialHolder materialHolder;

            short legacyData = 0;
            String legacyName = null;
            int legacyId = -1;
            if (entryObject.containsKey("legacy-data")) {
                legacyData = Short.parseShort(entryObject.get("legacy-data").toString());
            }
            if (entryObject.containsKey("legacy-name")) {
                legacyName = entryObject.get("legacy-name").toString();
            }
            if (entryObject.containsKey("legacy-id")) {
                legacyId = Integer.parseInt(entryObject.get("legacy-id").toString());
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
