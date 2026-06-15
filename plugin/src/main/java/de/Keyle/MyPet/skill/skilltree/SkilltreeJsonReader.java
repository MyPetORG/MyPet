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
import de.Keyle.MyPet.api.skill.UpgradeParsers;

import java.util.Optional;

/**
 * Case-insensitive, typed view over a skilltree {@link JsonObject}. Centralizes
 * the case-insensitive key lookup (via {@link UpgradeParsers#get}) that the
 * skilltree format has always used. Value conversions are eager: a malformed
 * value throws, which the caller's {@code Try.tryToLoad} wrapper catches so a
 * single bad field is skipped without losing the rest of the skilltree.
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
        return e == null ? Optional.empty() : Optional.of(e.getAsString());
    }

    public Optional<Integer> optInt(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e == null ? Optional.empty() : Optional.of(e.getAsInt());
    }

    public Optional<Double> optDouble(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e == null ? Optional.empty() : Optional.of(e.getAsDouble());
    }

    public Optional<Boolean> optBool(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e == null ? Optional.empty() : Optional.of(e.getAsBoolean());
    }

    public Optional<JsonArray> optArray(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e == null ? Optional.empty() : Optional.of(e.getAsJsonArray());
    }

    public Optional<JsonObject> optObject(String key) {
        JsonElement e = UpgradeParsers.get(json, key);
        return e == null ? Optional.empty() : Optional.of(e.getAsJsonObject());
    }
}
