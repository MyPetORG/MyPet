/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.webeditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.types.PetModelHook;
import de.Keyle.MyPet.api.util.hooks.types.PetModelSourceHook;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Reads MyPet's on-disk config files into the JSON {@code configs} envelope the
 * web editor consumes — the server-side counterpart of the web workspace shape.
 *
 * <ul>
 *   <li>{@code config.yml} / {@code pet-config.yml} / {@code exp-config.yml} /
 *       {@code pet-shops.yml} → {@code {format:"yaml", content:"<raw>"}}</li>
 *   <li>{@code skilltrees/*.st.json} → {@code {format:"skilltree-bundle",
 *       files:{name: <parsed JSON>}}} (the .st.json files already ARE the format
 *       the web editor expects, so we pass them through parsed, not re-encoded)</li>
 *   <li>{@code locale/*.properties} → {@code {format:"properties-bundle",
 *       files:{name:"<raw>"}}} (only on-disk overrides; JAR defaults come from
 *       Crowdin on the web side)</li>
 * </ul>
 *
 * <p>The session layer wraps this {@code configs} object with
 * {@code metadata} and {@code socket}.
 */
public final class ConfigSerializer {

    private final File dataFolder;

    public ConfigSerializer(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public ConfigSerializer() {
        this(MyPetApi.getPlugin().getDataFolder());
    }

    /** Build the {@code configs} object of the session envelope. */
    public JsonObject serializeConfigs() {
        JsonObject configs = new JsonObject();
        configs.add("config", yamlEntry("config.yml"));
        configs.add("pet-config", yamlEntry("pet-config.yml"));
        configs.add("exp-config", yamlEntry("exp-config.yml"));
        configs.add("pet-shops", yamlEntry("pet-shops.yml"));
        configs.add("hooks-config", yamlEntry("hooks-config.yml"));
        configs.add("skilltrees", skilltreeBundle());
        configs.add("locale", localeBundle());
        JsonObject renderers = new JsonObject();
        for (PetModelHook hook : MyPetApi.getServiceManager().getServices(PetModelHook.class)) {
            JsonArray ids = new JsonArray();
            hook.availableModels().stream().sorted().forEach(ids::add);
            renderers.add(hook.getServiceName(), ids);
        }
        JsonObject sources = new JsonObject();
        for (PetModelSourceHook hook : MyPetApi.getServiceManager().getServices(PetModelSourceHook.class)) {
            JsonArray ids = new JsonArray();
            hook.availableSources().stream().sorted().forEach(ids::add);
            sources.add(hook.getServiceName(), ids);
        }
        JsonObject providers = new JsonObject();
        providers.add("renderers", renderers);
        providers.add("sources", sources);
        configs.add("model-providers", providers);
        JsonArray leashFlags = new JsonArray();
        MyPetApi.getLeashFlagManager().flagNames().forEach(leashFlags::add);
        configs.add("leash-flags", leashFlags);
        return configs;
    }

    private JsonObject yamlEntry(String fileName) {
        JsonObject entry = new JsonObject();
        entry.addProperty("format", "yaml");
        entry.addProperty("content", readOrEmpty(new File(dataFolder, fileName)));
        return entry;
    }

    private JsonObject skilltreeBundle() {
        JsonObject bundle = new JsonObject();
        bundle.addProperty("format", "skilltree-bundle");
        JsonObject files = new JsonObject();
        File[] skilltrees = listFiles("skilltrees", f -> f.getName().endsWith(".st.json"));
        for (File file : skilltrees) {
            String content = readOrEmpty(file);
            if (content.isEmpty()) {
                continue;
            }
            try {
                files.add(file.getName(), JsonParser.parseString(content));
            } catch (RuntimeException e) {
                MyPetApi.getLogger().warning("WebEditor: skipping unparseable skilltree " + file.getName());
            }
        }
        bundle.add("files", files);
        return bundle;
    }

    private JsonObject localeBundle() {
        JsonObject bundle = new JsonObject();
        bundle.addProperty("format", "properties-bundle");
        JsonObject files = new JsonObject();
        for (File file : listFiles("locale", f -> f.getName().endsWith(".properties"))) {
            files.addProperty(file.getName(), readOrEmpty(file));
        }
        bundle.add("files", files);
        return bundle;
    }

    private File[] listFiles(String subDir, FileFilter filter) {
        File dir = new File(dataFolder, subDir);
        File[] files = dir.listFiles(filter);
        return files != null ? files : new File[0];
    }

    private static String readOrEmpty(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MyPetApi.getLogger().warning("WebEditor: could not read " + file.getName() + ": " + e.getMessage());
            return "";
        }
    }

    // Retained for symmetry/testing: build a configs object from provided contents.
    static JsonObject configEntry(String format, String content) {
        JsonObject entry = new JsonObject();
        entry.addProperty("format", format);
        entry.addProperty("content", content);
        return entry;
    }

    static JsonElement parseSkilltree(String content) {
        return JsonParser.parseString(content);
    }
}
