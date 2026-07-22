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

package de.Keyle.MyPet.skill.skilltree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.Keyle.MyPet.api.skill.UpgradeParsers;

import java.util.Optional;

/**
 * Case-insensitive, typed view over a skilltree {@link JsonObject}. Centralizes
 * the case-insensitive key lookup (via {@link UpgradeParsers#get}) that the
 * skilltree format has always used. Scalar accessors coerce quoted forms
 * ({@code "5"}, {@code "true"}); objects and arrays never coerce — a value of
 * the wrong JSON shape returns empty instead of throwing, so a key can be
 * probed for multiple shapes (e.g. {@code Weight} as number or object).
 */
public final class SkilltreeJsonReader {

    private final JsonObject json;

    public SkilltreeJsonReader(JsonObject json) {
        this.json = json;
    }

    public boolean has(String key) {
        return UpgradeParsers.get(json, key) != null;
    }

    public Optional<String> optString(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e != null && e.isJsonPrimitive() ? Optional.of(e.getAsString()) : Optional.empty();
    }

    public Optional<Integer> optInt(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return isNumber(e) ? Optional.of(e.getAsInt()) : Optional.empty();
    }

    public Optional<Double> optDouble(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return isNumber(e) ? Optional.of(e.getAsDouble()) : Optional.empty();
    }

    public Optional<Boolean> optBool(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        if (e == null || !e.isJsonPrimitive()) {
            return Optional.empty();
        }
        JsonPrimitive primitive = e.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return Optional.of(primitive.getAsBoolean());
        }
        if (primitive.isString()) {
            String value = primitive.getAsString();
            if ("true".equalsIgnoreCase(value)) {
                return Optional.of(true);
            }
            if ("false".equalsIgnoreCase(value)) {
                return Optional.of(false);
            }
        }
        return Optional.empty();
    }

    public Optional<JsonArray> optArray(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e != null && e.isJsonArray() ? Optional.of(e.getAsJsonArray()) : Optional.empty();
    }

    public Optional<JsonObject> optObject(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e != null && e.isJsonObject() ? Optional.of(e.getAsJsonObject()) : Optional.empty();
    }

    private static boolean isNumber(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = e.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return true;
        }
        if (primitive.isString()) {
            try {
                Double.parseDouble(primitive.getAsString());
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }
}
