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

package de.Keyle.MyPet.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.gui.FloatRange;
import de.Keyle.MyPet.api.gui.HeadSkin;
import de.Keyle.MyPet.api.gui.ItemAppearance;
import de.Keyle.MyPet.api.gui.SoundSpec;
import de.Keyle.MyPet.api.gui.ValidationException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Stateless JSON parsing helpers used by every section codec and the menu loader. */
public final class JsonHelpers {

    private JsonHelpers() {}

    // --- Position / region ----------------------------------------------------

    /** Parses {@code {"col":N,"row":N}} or {@code {"slot":N}} into a 2-element int[] {col, row}. */
    public static int[] parsePosition(JsonObject obj, String fieldName, int rows) {
        if (obj == null) throw new ValidationException(fieldName + " is required");
        int col, row;
        if (obj.has("slot")) {
            int slot = obj.get("slot").getAsInt();
            col = slot % 9;
            row = slot / 9;
        } else if (obj.has("col") && obj.has("row")) {
            col = obj.get("col").getAsInt();
            row = obj.get("row").getAsInt();
        } else {
            throw new ValidationException(fieldName + " must have either {col,row} or {slot}");
        }
        if (col < 0 || col >= 9) throw new ValidationException(fieldName + ".col out of range: " + col);
        if (row < 0 || row >= rows) throw new ValidationException(fieldName + ".row out of range: " + row);
        return new int[]{col, row};
    }

    // --- Item appearance ------------------------------------------------------

    /** Decodes an `item` JSON object into {@link ItemAppearance}. */
    public static ItemAppearance parseItem(JsonObject obj, String path) {
        if (obj == null) throw new ValidationException(path + " is required");
        String matName = requireString(obj, "material", path);
        Material mat = Material.matchMaterial(matName);
        if (mat == null) throw new ValidationException(path + ".material unknown: " + matName);

        String title = obj.has("title") ? obj.get("title").getAsString() : " ";
        List<String> lore = new ArrayList<>();
        if (obj.has("lore")) {
            for (JsonElement el : obj.getAsJsonArray("lore")) {
                lore.add(el.getAsString());
            }
        }
        boolean glow = obj.has("glow") && obj.get("glow").getAsBoolean();
        int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 1;
        int cmd = obj.has("custom-model-data") ? obj.get("custom-model-data").getAsInt() : 0;
        HeadSkin head = HeadSkin.STEVE;
        if (obj.has("head") && !obj.get("head").isJsonNull()) {
            String raw = obj.get("head").getAsString();
            try {
                head = HeadSkin.valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException(path + ".head unknown value '" + raw
                    + "' (expected steve, alex, or viewer)");
            }
        }
        return new ItemAppearance(mat, title, lore, glow, amount, cmd, head, null);
    }

    // --- Sound spec -----------------------------------------------------------

    /** Decodes a sound field which may be: object, array, JSON null, or absent. Absent returns {@link SoundSpec.Silent}. */
    public static SoundSpec parseSoundOrSilent(JsonElement element, String path) {
        if (element == null || element.isJsonNull()) return SoundSpec.Silent.INSTANCE;
        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            if (arr.isEmpty()) throw new ValidationException(path + " sound list must not be empty");
            List<SoundSpec> options = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                options.add(parseSoundElement(arr.get(i), path + "[" + i + "]"));
            }
            return new SoundSpec.Choice(options);
        }
        return parseSoundElement(element, path);
    }

    private static SoundSpec parseSoundElement(JsonElement element, String path) {
        if (!element.isJsonObject()) throw new ValidationException(path + " must be an object or list");
        JsonObject obj = element.getAsJsonObject();
        String keyStr = requireString(obj, "key", path);
        Key key;
        try {
            key = Key.key(keyStr);
        } catch (Exception e) {
            throw new ValidationException(path + ".key invalid sound key: " + keyStr, e);
        }
        Sound.Source source = obj.has("category")
            ? parseSoundSource(obj.get("category").getAsString(), path + ".category")
            : Sound.Source.MASTER;

        Object pitch = parseNumberOrRange(obj.get("pitch"), 1.0f, 0.5f, 2.0f, path + ".pitch");
        Object volume = parseNumberOrRange(obj.get("volume"), 1.0f, 0.0f, Float.MAX_VALUE, path + ".volume");

        if (pitch instanceof Float p && volume instanceof Float v) {
            return new SoundSpec.Fixed(key, v, p, source);
        }
        FloatRange pitchR  = pitch  instanceof FloatRange pr ? pr : FloatRange.of((float) pitch);
        FloatRange volumeR = volume instanceof FloatRange vr ? vr : FloatRange.of((float) volume);
        return new SoundSpec.Range(key, volumeR, pitchR, source);
    }

    private static Sound.Source parseSoundSource(String name, String path) {
        try {
            return Sound.Source.valueOf(name.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(path + " unknown category: " + name);
        }
    }

    /** Returns Float (fixed) or FloatRange (range) or the default Float. */
    private static Object parseNumberOrRange(JsonElement el, float def, float lo, float hi, String path) {
        if (el == null || el.isJsonNull()) return Float.valueOf(def);
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            float v = el.getAsFloat();
            requireRange(v, lo, hi, path);
            return Float.valueOf(v);
        }
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if (!o.has("min") || !o.has("max")) {
                throw new ValidationException(path + " range requires {min,max}");
            }
            float min = o.get("min").getAsFloat();
            float max = o.get("max").getAsFloat();
            requireRange(min, lo, hi, path + ".min");
            requireRange(max, lo, hi, path + ".max");
            return new FloatRange(min, max);
        }
        throw new ValidationException(path + " must be a number or {min,max} object");
    }

    private static void requireRange(float v, float lo, float hi, String path) {
        if (Float.isNaN(v) || v < lo || v > hi) {
            throw new ValidationException(path + " out of range [" + lo + "," + hi + "]: " + v);
        }
    }

    // --- Misc -----------------------------------------------------------------

    public static String requireString(JsonObject obj, String field, String path) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            throw new ValidationException(path + "." + field + " is required");
        }
        return obj.get(field).getAsString();
    }

    public static int requireInt(JsonObject obj, String field, String path) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            throw new ValidationException(path + "." + field + " is required");
        }
        return obj.get(field).getAsInt();
    }

    public static boolean optBoolean(JsonObject obj, String field, boolean def) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsBoolean() : def;
    }

    // --- Deep merge -----------------------------------------------------------

    /**
     * Deep-merges {@code overlay} onto {@code base}, returning a new {@link JsonObject}.
     * Objects merge recursively; primitives and arrays in {@code overlay} replace those in {@code base}.
     * JSON nulls in {@code overlay} explicitly set the key to null (sentinel for "silent" sound, etc.).
     */
    public static JsonObject deepMerge(JsonObject base, JsonObject overlay) {
        JsonObject out = base.deepCopy();
        for (var entry : overlay.entrySet()) {
            String k = entry.getKey();
            JsonElement v = entry.getValue();
            if (v.isJsonObject() && out.has(k) && out.get(k).isJsonObject()) {
                out.add(k, deepMerge(out.getAsJsonObject(k), v.getAsJsonObject()));
            } else {
                out.add(k, v.deepCopy());
            }
        }
        return out;
    }
}
