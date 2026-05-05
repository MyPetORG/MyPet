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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Disk-backed cache for pre-computed experience-per-level values.
 *
 * <p>Computing the experience required for every level on every pet type can be expensive
 * when using custom (e.g. JavaScript) calculators. This service persists the computed
 * mapping to a GZIP-compressed JSON file ({@code exp.cache}) in the plugin data folder,
 * avoiding redundant recalculation across server restarts.
 *
 * <p>The cache is keyed by world-group, pet type, and level. It is automatically
 * invalidated when the active {@link ExperienceCalculator} changes its identifier or
 * version (see {@link #checkVersion(ExperienceCalculator)}).
 *
 * <p>Loaded during {@link Load.State#OnLoad} so that the cache is available before
 * the experience calculator manager activates.
 */
@ServiceName("ExperienceCache")
@Load(Load.State.OnLoad)
public class ExperienceCache implements ServiceContainer {

    String calculator = null;
    long version = 0;
    JsonObject expMap = new JsonObject();

    final File cacheFile = new File(MyPetApi.getPlugin().getDataFolder(), "exp.cache");

    /**
     * Retrieves the cached cumulative experience for the given world-group, pet type, and level.
     *
     * @param worldGroup the world-group name
     * @param type       the pet type
     * @param level      the target level
     * @return the cached experience value
     * @throws LevelNotCalculatedException if no cached value exists for the given combination
     */
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

    /**
     * Determines the highest level whose cached experience threshold is at or below the given
     * experience amount, effectively performing a reverse lookup from experience to level.
     *
     * @param worldGroup the world-group name
     * @param type       the pet type
     * @param exp        the current experience total
     * @return the highest level the pet qualifies for, or {@code 0} if no cache entries exist
     */
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

    /**
     * Inserts a computed experience value into the in-memory cache.
     *
     * <p>Values with {@code level < 1} or an empty world-group are silently ignored.
     *
     * @param worldGroup the world-group name
     * @param type       the pet type
     * @param level      the level (must be >= 1)
     * @param exp        the cumulative experience required to reach this level
     */
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

    /**
     * Validates the cache against the current calculator's identifier and version.
     *
     * <p>If either has changed since the cache was last built, all cached entries are
     * cleared, a new version/identifier is recorded, and the cache file is saved. The
     * next time experience values are needed they will be recalculated on demand.
     *
     * @param calculator the currently active experience calculator
     */
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

    /** Persists the in-memory cache to the GZIP-compressed {@code exp.cache} file. */
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

    /**
     * Loads the cache from the {@code exp.cache} file.
     *
     * <p>If the file is corrupt or unreadable, the cache is silently reset to empty and the
     * file is deleted so it will be rebuilt on the next save.
     */
    protected void load() {
        try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(cacheFile.toPath())), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject cacheObject = gson.fromJson(reader, JsonObject.class);
            this.expMap = cacheObject.get("expMap").getAsJsonObject();
            this.version = cacheObject.get("version").getAsLong();
            this.calculator = cacheObject.get("calculator").getAsString();
        } catch (Throwable e) {
            try {
                Files.deleteIfExists(cacheFile.toPath());
            } catch (IOException ignored) {
            }
            version = 0;
            calculator = null;
            expMap.entrySet().clear();
        }
    }

    /**
     * Thrown when a requested level has not yet been computed and cached.
     *
     * <p>Callers should catch this and compute the value on demand via the active
     * {@link ExperienceCalculator}, then insert the result into the cache.
     */
    public static class LevelNotCalculatedException extends Exception {

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
