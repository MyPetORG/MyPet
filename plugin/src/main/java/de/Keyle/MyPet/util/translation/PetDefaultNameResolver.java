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

package de.Keyle.MyPet.util.translation;

import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.EntityType;

/**
 * Resolves the default display name for a freshly-created pet.
 *
 * <p>Lookup chain:</p>
 * <ol>
 *   <li>Mojang's vanilla {@code entity.minecraft.<type>} translation, rendered against
 *       the creator's client locale via {@link VanillaTranslationLoader#resolveEntityName}.</li>
 *   <li>English mechanically derived from the Bukkit {@code EntityType} enum
 *       ({@code HAPPY_GHAST → "Happy Ghast"}), used when the vanilla registry isn't
 *       loaded or the type doesn't map to a Bukkit enum.</li>
 * </ol>
 */
public final class PetDefaultNameResolver {

    private PetDefaultNameResolver() {}

    public static String resolve(PetType type, MyPetPlayer player) {
        try {
            EntityType entityType = EntityType.valueOf(type.getBukkitName());
            return VanillaTranslationLoader.resolveEntityName(entityType, player.getLanguage());
        } catch (IllegalArgumentException e) {
            // Third-party PetType with no Bukkit enum counterpart — humanize the CamelCase name.
            return humanizeCamelCase(type.name());
        }
    }

    private static String humanizeCamelCase(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }
}
