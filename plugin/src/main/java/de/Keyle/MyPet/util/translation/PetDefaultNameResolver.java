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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.PlatformHelper;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;

/**
 * Resolves the default display name for a freshly-created pet.
 *
 * <p>Mojang's vanilla {@code entity.minecraft.<bukkit-name>} translation —
 * resolved against the creator's client locale via
 * {@link VanillaTranslationLoader#resolveEntityName(String, String)} — is the
 * authoritative source. When the vanilla map hasn't loaded yet (offline
 * server, CDN error, async race during plugin enable), the loader internally
 * falls back to English mechanically derived from
 * {@link MyPetType#getBukkitName()}.</p>
 *
 * <p>The legacy {@code Name.<PetType>} rows in MyPet's locale files are
 * intentionally <strong>not</strong> consulted here. Mojang's translations
 * cover every shipped entity in every supported locale, so a per-locale
 * MyPet override would be a redundant maintenance burden. Owners and admins
 * who want a custom default name use {@code /petname} to rename after
 * creation.</p>
 */
public final class PetDefaultNameResolver {

    private PetDefaultNameResolver() {}

    public static String resolve(MyPetType type, MyPetPlayer player) {
        return resolve(type, player == null ? null : player.getLanguage());
    }

    public static String resolve(MyPetType type, Player player) {
        if (player == null) return resolve(type, (String) null);
        // PlatformHelper is null on an unsupported NMS version between onLoad and the onEnable
        // setEnabled(false) gate — callers from shop/GUI/event paths can still reach this in that
        // window and would otherwise NPE inside the locale lookup.
        PlatformHelper helper = MyPetApi.getPlatformHelper();
        return resolve(type, helper == null ? null : helper.getPlayerLanguage(player));
    }

    public static String resolve(MyPetType type, String localeTag) {
        return VanillaTranslationLoader.resolveEntityName(type.getBukkitName(), localeTag);
    }
}
