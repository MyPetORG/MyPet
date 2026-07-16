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

import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Server-side audit of an applied web-editor change: compares the on-disk config
 * (before it is overwritten) against the incoming payload and renders a
 * human-readable, value-level summary, then broadcasts it to the console and any
 * online player with {@code mypet.admin.notify}.
 *
 * <p>The diff is computed from the parsed YAML (Bukkit's own {@link YamlConfiguration}),
 * not the raw text, so it reports the values that actually changed and is immune to
 * comment loss / reformatting introduced by the editor re-serializing the file.
 *
 * <p>{@link #summarize} MUST be called before {@code ConfigApplier.writeChanges},
 * while the old file contents are still on disk.
 */
public final class WebEditorChanges {

    /** Permission required (in addition to console) to receive apply notifications. Registered in plugin.yml (default: op). */
    public static final String NOTIFY_PERMISSION = "MyPet.admin.notify";

    private static final int MAX_LINES = 30;

    private WebEditorChanges() {
    }

    private static final String[][] YAML_FILES = {
            {"config", "config.yml"},
            {"pet-config", "pet-config.yml"},
            {"exp-config", "exp-config.yml"},
            {"pet-shops", "pet-shops.yml"},
            {"hooks-config", "hooks-config.yml"},
    };

    /** Build the value-level change lines for the files in the payload. */
    public static List<Component> summarize(File dataFolder, JsonObject changedConfigs) {
        List<Component> lines = new ArrayList<>();
        for (String[] entry : YAML_FILES) {
            addYamlChanges(lines, dataFolder, changedConfigs, entry[0], entry[1]);
        }
        addBundleSummary(lines, changedConfigs, "skilltrees", "skilltrees");
        addBundleSummary(lines, changedConfigs, "locale", "locale");
        return lines;
    }

    /** Notify console + permissioned players that {@code who} applied changes. */
    public static void broadcast(String who, List<Component> changes) {
        Component header = Component.text("[MyPet] ", NamedTextColor.AQUA)
                .append(Component.text(who, NamedTextColor.WHITE))
                .append(Component.text(" applied web-editor changes:", NamedTextColor.AQUA));

        List<Component> body = new ArrayList<>();
        if (changes.isEmpty()) {
            body.add(indent(Component.text("(no value-level changes detected)", NamedTextColor.GRAY)));
        } else {
            int shown = Math.min(changes.size(), MAX_LINES);
            for (int i = 0; i < shown; i++) {
                body.add(indent(changes.get(i)));
            }
            if (changes.size() > MAX_LINES) {
                body.add(indent(Component.text("…and " + (changes.size() - MAX_LINES) + " more", NamedTextColor.GRAY)));
            }
        }

        Bukkit.getConsoleSender().sendMessage(header);
        body.forEach(line -> Bukkit.getConsoleSender().sendMessage(line));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(NOTIFY_PERMISSION)) {
                player.sendMessage(header);
                body.forEach(player::sendMessage);
            }
        }
    }

    private static void addYamlChanges(List<Component> lines, File dataFolder, JsonObject configs, String key, String fileName) {
        if (!configs.has(key)) {
            return;
        }
        JsonObject entry = configs.getAsJsonObject(key);
        if (!entry.has("content")) {
            return;
        }
        YamlConfiguration oldCfg = parse(readOrEmpty(new File(dataFolder, fileName)));
        YamlConfiguration newCfg = parse(entry.get("content").getAsString());

        Set<String> keys = new TreeSet<>();
        keys.addAll(leafKeys(oldCfg));
        keys.addAll(leafKeys(newCfg));
        for (String path : keys) {
            String before = str(oldCfg.get(path));
            String after = str(newCfg.get(path));
            if (Objects.equals(before, after)) {
                continue;
            }
            lines.add(changeLine(fileName, path, before, after));
        }
    }

    private static void addBundleSummary(List<Component> lines, JsonObject configs, String key, String label) {
        if (!configs.has(key)) {
            return;
        }
        JsonObject entry = configs.getAsJsonObject(key);
        int count = entry.has("files") ? entry.getAsJsonObject("files").size() : 0;
        lines.add(Component.text(label, NamedTextColor.GRAY)
                .append(Component.text(": " + count + " file" + (count == 1 ? "" : "s") + " updated", NamedTextColor.GRAY)));
    }

    private static Component changeLine(String fileName, String path, String before, String after) {
        Component line = Component.text(fileName + "  ", NamedTextColor.DARK_AQUA)
                .append(Component.text(path + ": ", NamedTextColor.GRAY));
        if (before == null) {
            return line.append(Component.text(after, NamedTextColor.GREEN))
                    .append(Component.text(" (added)", NamedTextColor.DARK_GRAY));
        }
        if (after == null) {
            return line.append(Component.text(before, NamedTextColor.RED).decorate(TextDecoration.STRIKETHROUGH))
                    .append(Component.text(" (removed)", NamedTextColor.DARK_GRAY));
        }
        return line.append(Component.text(before, NamedTextColor.RED).decorate(TextDecoration.STRIKETHROUGH))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(after, NamedTextColor.GREEN));
    }

    private static Set<String> leafKeys(YamlConfiguration cfg) {
        Set<String> out = new HashSet<>();
        for (String key : cfg.getKeys(true)) {
            if (!cfg.isConfigurationSection(key)) {
                out.add(key);
            }
        }
        return out;
    }

    private static YamlConfiguration parse(String content) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(content);
        } catch (InvalidConfigurationException e) {
            MyPetApi.getLogger().warning("WebEditor: could not parse config for change summary: " + e.getMessage());
        }
        return cfg;
    }

    private static Component indent(Component line) {
        return Component.text("  ").append(line);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String readOrEmpty(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
