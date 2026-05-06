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

package de.Keyle.MyPet.migration.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.SkilltreeMigration;
import de.Keyle.MyPet.migration.SkilltreeMigrationContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Converts legacy color-code strings in MyPet skilltree ({@code .st.json}) files to
 * MiniMessage format. The v4 skilltree loader feeds {@code Name}, {@code Display},
 * {@code Description} entries, and {@code Notifications} messages directly into the
 * MiniMessage-aware rendering pipeline, so any surviving {@code &}/{@code §} codes
 * render as literal text.
 * <p>
 * Idempotent: converted strings no longer contain legacy codes, so the detection regex
 * won't match on a second run.
 */
@Migration(
        version = "4.0.0",
        description = "Convert legacy color codes in skilltree JSON files to MiniMessage"
)
public class MigrateSkilltreeColorCodesToMiniMessage implements SkilltreeMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");

    private static final Pattern LEGACY_CODE = Pattern.compile("[§&][0-9a-fk-orxA-FK-ORX]");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /** Top-level scalar string keys that carry user-visible colored text. */
    private static final String[] STRING_KEYS = {"Name", "Display"};

    /** Top-level array-of-strings keys. */
    private static final String[] STRING_ARRAY_KEYS = {"Description"};

    /** Top-level object keys whose string values carry user-visible colored text. */
    private static final String[] STRING_VALUE_OBJECT_KEYS = {"Notifications"};

    @Override
    public void migrate(SkilltreeMigrationContext ctx) throws MigrationException {
        List<File> files = ctx.getSkilltreeFiles();
        if (files.isEmpty()) {
            LOG.info("No skilltree files found — skipping skilltree color-code migration.");
            return;
        }

        int totalConverted = 0;
        int filesUpdated = 0;
        int filesUntouched = 0;

        for (File file : files) {
            try {
                JsonObject root = ctx.readSkilltree(file);
                FileResult result = migrateFile(root, file.getName());
                if (result.changed) {
                    ctx.writeSkilltree(file, root);
                    filesUpdated++;
                    totalConverted += result.conversions;
                } else {
                    filesUntouched++;
                }
            } catch (Exception e) {
                LOG.warning("Failed to process skilltree " + file.getName()
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving file unchanged");
            }
        }

        LOG.info("Skilltree color-code migration complete.");
        LOG.info("  Files updated: " + filesUpdated + ".");
        LOG.info("  Files untouched: " + filesUntouched + ".");
        LOG.info("  Total strings converted: " + totalConverted + ".");
    }

    private FileResult migrateFile(JsonObject root, String fileName) {
        FileResult result = new FileResult();

        for (String key : STRING_KEYS) {
            if (convertStringProperty(root, key, fileName)) {
                result.recordChange();
            }
        }

        for (String key : STRING_ARRAY_KEYS) {
            int changed = convertStringArrayProperty(root, key, fileName);
            if (changed > 0) {
                result.recordChanges(changed);
            }
        }

        for (String key : STRING_VALUE_OBJECT_KEYS) {
            int changed = convertStringValueObjectProperty(root, key, fileName);
            if (changed > 0) {
                result.recordChanges(changed);
            }
        }

        return result;
    }

    private boolean convertStringProperty(JsonObject obj, String key, String fileName) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return false;
        }
        String oldValue = obj.get(key).getAsString();
        if (oldValue == null || oldValue.isEmpty() || !hasLegacyCode(oldValue)) {
            return false;
        }
        try {
            String newValue = convert(oldValue);
            obj.addProperty(key, newValue);
            LOG.info("Converted " + fileName + "::" + key
                    + ": " + oldValue + "  →  " + newValue);
            return true;
        } catch (Exception e) {
            LOG.warning("Failed to convert " + fileName + "::" + key
                    + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + " — leaving unchanged");
            return false;
        }
    }

    private int convertStringArrayProperty(JsonObject obj, String key, String fileName) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return 0;
        }
        JsonArray array = obj.getAsJsonArray(key);
        JsonArray newArray = new JsonArray(array.size());
        int changed = 0;
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonPrimitive()) {
                newArray.add(element);
                continue;
            }
            String oldValue = element.getAsString();
            if (oldValue == null || !hasLegacyCode(oldValue)) {
                newArray.add(element);
                continue;
            }
            try {
                String newValue = convert(oldValue);
                newArray.add(newValue);
                changed++;
                LOG.info("Converted " + fileName + "::" + key + "[" + i + "]"
                        + ": " + oldValue + "  →  " + newValue);
            } catch (Exception e) {
                newArray.add(element);
                LOG.warning("Failed to convert " + fileName + "::" + key + "[" + i + "]"
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving unchanged");
            }
        }
        if (changed > 0) {
            obj.add(key, newArray);
        }
        return changed;
    }

    /**
     * Converts string values within a nested JSON object (e.g. {@code Notifications}, whose
     * keys are level rules and whose values are notification messages). Non-string values
     * are left unchanged.
     */
    private int convertStringValueObjectProperty(JsonObject obj, String key, String fileName) {
        if (!obj.has(key) || !obj.get(key).isJsonObject()) {
            return 0;
        }
        JsonObject nested = obj.getAsJsonObject(key);
        int changed = 0;
        for (Map.Entry<String, JsonElement> entry : nested.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive()) {
                continue;
            }
            String oldValue = value.getAsString();
            if (oldValue == null || !hasLegacyCode(oldValue)) {
                continue;
            }
            try {
                String newValue = convert(oldValue);
                nested.addProperty(entry.getKey(), newValue);
                changed++;
                LOG.info("Converted " + fileName + "::" + key + "." + entry.getKey()
                        + ": " + oldValue + "  →  " + newValue);
            } catch (Exception e) {
                LOG.warning("Failed to convert " + fileName + "::" + key
                        + "." + entry.getKey()
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving unchanged");
            }
        }
        return changed;
    }

    private static boolean hasLegacyCode(String value) {
        return LEGACY_CODE.matcher(value).find();
    }

    private static String convert(String oldValue) {
        // Normalize § to & so one legacy serializer handles both. Both prefix characters
        // use the same code alphabet, so a straight character replace is safe.
        String normalized = oldValue.replace('§', '&');
        Component component = LEGACY.deserialize(normalized);
        return MINI_MESSAGE.serialize(component);
    }

    private static final class FileResult {
        boolean changed;
        int conversions;

        void recordChange() {
            changed = true;
            conversions++;
        }

        void recordChanges(int count) {
            if (count > 0) {
                changed = true;
                conversions += count;
            }
        }
    }
}
