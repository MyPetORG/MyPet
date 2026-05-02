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

package de.Keyle.MyPet.api.skill.experience;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import lombok.Getter;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@ServiceName("ExperienceCache")
@Load(Load.State.OnLoad)
public class ExperienceCache implements ServiceContainer {

    String calculator = null;
    long version = 0;
    JsonObject expMap = new JsonObject();

    File cacheFile = new File(MyPetApi.getPlugin().getDataFolder(), "exp.cache");

    public double getExp(String worldGroup, MyPetType type, int level) throws LevelNotCalculatedException {
        if (this.expMap.has(worldGroup)) {
            JsonObject typeMap = this.expMap.getAsJsonObject(worldGroup);
            if (typeMap.has(type.name())) {
                JsonObject expMap = typeMap.getAsJsonObject(type.name());
                if (expMap.has("" + level)) {
                    return expMap.get("" + level).getAsDouble();
                }
            }
        }
        throw new LevelNotCalculatedException(type, level);
    }

    public int getLevel(String worldGroup, MyPetType type, double exp) {
        if (!this.expMap.has(worldGroup)) return 0;
        JsonObject typeMap = this.expMap.getAsJsonObject(worldGroup);
        if (!typeMap.has(type.name())) return 0;
        JsonObject levelMap = typeMap.getAsJsonObject(type.name());
        int found = 0;
        for (String levelKey : levelMap.keySet()) {
            try {
                int level = Integer.parseInt(levelKey);
                double levelExp = levelMap.get(levelKey).getAsDouble();
                if (levelExp <= exp && level > found) {
                    found = level;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return found;
    }

    public void insertExp(String worldGroup, MyPetType type, int level, double exp) {
        if (level < 1) {
            return;
        }
        if (worldGroup.isEmpty()) {
            return;
        }
        if (!expMap.has(worldGroup)) {
            expMap.add(worldGroup, new JsonObject());
        }
        JsonObject typeMap = this.expMap.get(worldGroup).getAsJsonObject();
        if (!typeMap.has(type.name())) {
            typeMap.add(type.name(), new JsonObject());
        }
        JsonObject expMap = typeMap.get(type.name()).getAsJsonObject();
        expMap.addProperty("" + level, exp);
    }

    @Override
    public boolean onEnable() {
        if (cacheFile.exists()) {
            load();
        }
        return true;
    }

    @Override
    public void onDisable() {
        save();
        version = 0;
        calculator = null;
        expMap.entrySet().clear();
    }

    public void checkVersion(ExperienceCalculator calculator) {
        long version = calculator.getVersion();
        String identifier = calculator.getIdentifier();
        if (version != this.version || !identifier.equals(this.calculator)) {
            expMap.entrySet().clear();
            this.version = version;
            this.calculator = identifier;
            MyPetApi.getLogger().info("Current Exp-Cache is invalid, it will be recalculated.");
            save();
        }
    }

    protected void save() {
        try (OutputStreamWriter oos = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(cacheFile.toPath())))) {
            JsonObject cacheObject = new JsonObject();
            cacheObject.add("expMap", expMap);
            cacheObject.addProperty("version", version);
            cacheObject.addProperty("calculator", calculator);
            Gson gson = new Gson();
            oos.write(gson.toJson(cacheObject));
        } catch (Exception e) {
            ErrorUtil.report(e);
        }
    }

    protected void load() {
        try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(cacheFile.toPath())), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject cacheObject = gson.fromJson(reader, JsonObject.class);
            this.expMap = cacheObject.get("expMap").getAsJsonObject();
            this.version = cacheObject.get("version").getAsLong();
            this.calculator = cacheObject.get("calculator").getAsString();
        } catch (Throwable e) {
            cacheFile.delete();
            version = 0;
            calculator = null;
            expMap.entrySet().clear();
        }
    }

    public class LevelNotCalculatedException extends Exception {

        @Getter
        private final MyPetType type;
        @Getter
        private final int level;

        public LevelNotCalculatedException(MyPetType type, int level) {
            super("Exp for " + type + " at level " + level + " not yet calculated!");
            this.type = type;
            this.level = level;
        }
    }
}
