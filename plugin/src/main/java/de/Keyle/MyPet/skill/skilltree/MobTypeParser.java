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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves a skilltree {@code MobTypes} array (supports {@code *} and {@code -Type} negation) into a set of {@link PetType}. */
public final class MobTypeParser {

    private MobTypeParser() {
    }

    public static Set<PetType> parse(JsonArray mobTypeArray, String skilltreeID) {
        List<PetType> availableTypes = PetType.all();
        Set<PetType> mobTypes = new HashSet<>();
        if (mobTypeArray.isEmpty()) {
            mobTypes.addAll(availableTypes);
        } else {
            boolean allNegative = true;
            for (JsonElement o : mobTypeArray) {
                String type = o.getAsString();
                if (!type.startsWith("-")) {
                    allNegative = false;
                    break;
                }
            }
            if (allNegative) {
                mobTypes.addAll(availableTypes);
            }
            mobTypeArray.forEach(jsonElement -> {
                String type = jsonElement.getAsString();
                if (type.equals("*")) {
                    mobTypes.addAll(availableTypes);
                } else {
                    boolean negative = false;
                    if (type.startsWith("-")) {
                        type = type.substring(1);
                        negative = true;
                    }
                    PetType mobType = PetType.byNameOrNull(type);
                    if (mobType == null) {
                        MyPetApi.getLogger().warning("Skilltree '" + skilltreeID + "': Unknown mob type '" + type + "' - skipping (not a valid Pet type or not available in this Minecraft version)");
                    } else if (mobType.checkMinecraftVersion()) {
                        if (negative) {
                            mobTypes.remove(mobType);
                        } else {
                            mobTypes.add(mobType);
                        }
                    }
                }
            });
        }
        return mobTypes;
    }
}
