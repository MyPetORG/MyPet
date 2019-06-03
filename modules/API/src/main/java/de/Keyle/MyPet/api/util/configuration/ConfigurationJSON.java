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

package de.Keyle.MyPet.api.util.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.Keyle.MyPet.MyPetApi;

import java.io.*;

public class ConfigurationJSON {
    public File jsonFile;
    private JsonObject config;

    public ConfigurationJSON(String path) {
        this(new File(path));
    }

    public ConfigurationJSON(File file) {
        jsonFile = file;
    }

    public JsonObject getJsonObject() {
        if (config == null) {
            config = new JsonObject();
        }
        return config;
    }

    public boolean load() {
        config = new JsonObject();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, JsonObject.class);
        } catch (JsonParseException e) {
            MyPetApi.getLogger().warning("Could not parse/load " + jsonFile.getName());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJsonString = gson.toJson(config);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
            writer.write(prettyJsonString);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearConfig() {
        config = new JsonObject();
    }
}