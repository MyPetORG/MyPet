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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Keyle.MyPet.api.gui.*;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads bundled and overlay JSON, deep-merges them per key, and validates the
 * result into a {@link MenuDefinition}. Throws {@link ValidationException} on any
 * structural / semantic error; the registry catches and falls back to bundled-only.
 */
public final class MenuLoader {

    private MenuLoader() {}

    /** Read raw JSON from an input stream. Closes the stream. */
    public static JsonObject read(InputStream in) {
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(r);
            if (!el.isJsonObject()) throw new ValidationException("top-level JSON must be an object");
            return el.getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new ValidationException("read failure", e);
        }
    }

    /**
     * Parse a (merged) top-level JSON object into a {@link MenuDefinition}.
     * Performs every top-level validation rule listed in the spec.
     */
    public static MenuDefinition load(String menuId, JsonObject merged, int rowsClampMin, int rowsClampMax) {
        requireOnlyKnownTopLevelKeys(menuId, merged);

        String title = JsonHelpers.requireString(merged, "title", menuId);
        int rows = JsonHelpers.requireInt(merged, "rows", menuId);
        if (rows < 1 || rows > 6) throw new ValidationException(menuId + ".rows must be 1..6, was " + rows);
        if (rows < rowsClampMin || rows > rowsClampMax) {
            throw new ValidationException(menuId + ".rows " + rows + " outside handler bounds ["
                + rowsClampMin + "," + rowsClampMax + "]");
        }

        boolean escBack = JsonHelpers.optBoolean(merged, "esc-supports-back", false);
        SoundSpec soundOpen  = JsonHelpers.parseSoundOrSilent(merged.get("sound-on-open"),  menuId + ".sound-on-open");
        SoundSpec soundClose = JsonHelpers.parseSoundOrSilent(merged.get("sound-on-close"), menuId + ".sound-on-close");
        SoundSpec soundBack  = merged.has("sound-on-back") && !merged.get("sound-on-back").isJsonNull()
            ? JsonHelpers.parseSoundOrSilent(merged.get("sound-on-back"), menuId + ".sound-on-back")
            : null;

        if (!merged.has("sections") || !merged.get("sections").isJsonObject()) {
            throw new ValidationException(menuId + ".sections must be an object");
        }
        Map<String, Section> sections = parseSections(menuId, rows, merged.getAsJsonObject("sections"));

        return new MenuDefinition(menuId, title, rows, escBack, soundOpen, soundClose, soundBack, sections);
    }

    private static Map<String, Section> parseSections(String menuId, int rows, JsonObject obj) {
        Set<String> sectionIds = obj.keySet();
        Map<String, Section> result = new LinkedHashMap<>();

        for (String sid : sectionIds) {
            JsonElement el = obj.get(sid);
            if (el == null || !el.isJsonObject()) {
                throw new ValidationException(menuId + ".sections." + sid + " must be an object");
            }
            JsonObject sObj = el.getAsJsonObject();
            String typeId = JsonHelpers.requireString(sObj, "type", menuId + ".sections." + sid);
            SectionType<?> stype = SectionTypeRegistry.byId(typeId).orElseThrow(
                () -> new ValidationException(menuId + ".sections." + sid + ".type unknown: " + typeId
                    + closeMatchHint(typeId)));
            CodecContext ctx = new CodecContext(menuId, rows, sectionIds);
            Section decoded = stype.codec().decode(sid, sObj, ctx);
            if (!stype.sectionClass().isInstance(decoded)) {
                throw new ValidationException(menuId + ".sections." + sid
                    + ": codec returned wrong section class");
            }
            result.put(sid, decoded);
        }

        validateNoSlotOverlap(menuId, rows, result);
        return result;
    }

    private static void requireOnlyKnownTopLevelKeys(String menuId, JsonObject obj) {
        Set<String> known = Set.of("title", "rows", "esc-supports-back",
            "sound-on-open", "sound-on-close", "sound-on-back", "sections");
        for (String k : obj.keySet()) {
            if (!known.contains(k)) {
                throw new ValidationException(menuId + ": unknown top-level key '" + k
                    + "'. Known: " + known);
            }
        }
    }

    /** Compute owned-slot sets across every section and ensure no two sections claim the same slot. */
    private static void validateNoSlotOverlap(String menuId, int rows, Map<String, Section> sections) {
        Set<Integer> seen = new HashSet<>();
        for (var e : sections.entrySet()) {
            Section s = e.getValue();
            @SuppressWarnings({"rawtypes", "unchecked"})
            SectionRenderer renderer = (SectionRenderer) s.type().renderer();
            if (renderer.decorative()) continue;
            Set<Integer> owned = renderer.ownedSlots(s);
            for (int slot : owned) {
                if (slot < 0 || slot >= rows * 9) {
                    throw new ValidationException(menuId + ".sections." + e.getKey()
                        + " renders slot " + slot + " outside menu bounds");
                }
                if (!seen.add(slot)) {
                    throw new ValidationException(menuId + ".sections." + e.getKey()
                        + " overlaps another section at slot " + slot);
                }
            }
        }
    }

    @Nullable
    private static String closeMatchHint(String typo) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String id : SectionTypeRegistry.all().keySet()) {
            int d = levenshtein(typo, id);
            if (d < bestDist) { bestDist = d; best = id; }
        }
        return (best != null && bestDist <= 2) ? " (did you mean '" + best + "'?)" : "";
    }

    private static int levenshtein(String a, String b) {
        int[][] d = new int[a.length()+1][b.length()+1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i-1) == b.charAt(j-1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i-1][j]+1, d[i][j-1]+1), d[i-1][j-1]+cost);
            }
        }
        return d[a.length()][b.length()];
    }
}
