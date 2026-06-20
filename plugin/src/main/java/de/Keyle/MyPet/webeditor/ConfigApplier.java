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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.util.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Writes a change payload (the {@code configs} object, containing only the dirty
 * files) back to disk and hot-reloads the affected subsystems.
 *
 * <p>{@link #reload()} touches plugin state (config + skilltree manager + locale)
 * and MUST be called on the server main thread; the socket handler schedules it
 * there. Writing files is plain I/O and is safe off-thread.
 */
public final class ConfigApplier {

    private final File dataFolder;

    public ConfigApplier(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public ConfigApplier() {
        this(MyPetApi.getPlugin().getDataFolder());
    }

    /** Write all changed files from the payload to disk (off-thread safe). */
    public void writeChanges(JsonObject changedConfigs) throws IOException {
        writeYaml(changedConfigs, "config", "config.yml");
        writeYaml(changedConfigs, "pet-config", "pet-config.yml");
        writeYaml(changedConfigs, "exp-config", "exp-config.yml");
        writeYaml(changedConfigs, "pet-shops", "pet-shops.yml");

        if (changedConfigs.has("skilltrees")) {
            File dir = ensureDir("skilltrees");
            JsonObject files = changedConfigs.getAsJsonObject("skilltrees").getAsJsonObject("files");
            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                // .st.json files store the raw JSON object as written by the editor.
                File target = safeChildFile(dir, entry.getKey(), ".st.json");
                if (target != null) {
                    write(target, entry.getValue().toString());
                }
            }
        }

        if (changedConfigs.has("locale")) {
            File dir = ensureDir("locale");
            JsonObject files = changedConfigs.getAsJsonObject("locale").getAsJsonObject("files");
            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                File target = safeChildFile(dir, entry.getKey(), ".properties");
                if (target != null) {
                    write(target, entry.getValue().getAsString());
                }
            }
        }
    }

    /**
     * Resolve an editor-supplied {@code name} as a direct child of {@code dir},
     * rejecting anything that isn't a plain single-segment filename with the
     * expected suffix. Guards the skilltree/locale bundles against path traversal
     * (`..`, separators, absolute paths) in untrusted change payloads — the
     * filenames come straight from the relayed JSON. Returns {@code null} (after
     * logging) for an unsafe name.
     */
    private File safeChildFile(File dir, String name, String requiredSuffix) {
        boolean validName = name != null && !name.isBlank()
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0
                && name.endsWith(requiredSuffix);
        if (validName) {
            Path base = dir.toPath().toAbsolutePath().normalize();
            Path target = base.resolve(name).normalize();
            if (target.startsWith(base) && base.equals(target.getParent())) {
                return target.toFile();
            }
        }
        MyPetApi.getLogger().warning("WebEditor: rejected unsafe file name in change payload: " + name);
        return null;
    }

    /** Hot-reload config, skilltrees and locale. Call on the main thread. */
    public void reload() {
        ConfigurationLoader.loadConfiguration();
        MyPetApi.getSkilltreeManager().clearSkilltrees();
        SkillTreeLoaderJSON.loadSkilltrees(new File(dataFolder, "skilltrees"));
        Locale.init();
    }

    private void writeYaml(JsonObject configs, String key, String fileName) throws IOException {
        if (!configs.has(key)) {
            return;
        }
        String content = configs.getAsJsonObject(key).get("content").getAsString();
        write(new File(dataFolder, fileName), content);
    }

    private File ensureDir(String subDir) {
        File dir = new File(dataFolder, subDir);
        if (!dir.exists() && !dir.mkdirs()) {
            MyPetApi.getLogger().warning("WebEditor: could not create directory " + dir.getPath());
        }
        return dir;
    }

    private static void write(File file, String content) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }
}
