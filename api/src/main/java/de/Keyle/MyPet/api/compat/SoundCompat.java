/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.compat;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.lang.reflect.Method;

public class SoundCompat {

    // Resolved once at class load: org.bukkit.Sound keeps a static valueOf(String)
    // whether it is an enum (<= 1.21.2) or an interface (1.21.3+), so a single
    // reflective lookup serves every supported server. null only if it is absent.
    private static final Method SOUND_VALUE_OF = resolveSoundValueOf();

    private static Method resolveSoundValueOf() {
        try {
            return Sound.class.getMethod("valueOf", String.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Compat<String> ENDERMAN_TELEPORT = new Compat<String>()
            .d("mob.endermen.portal")
            .v("1.9", "entity.endermen.teleport")
            .v("1.13", "entity.enderman.teleport")
            .search();

    public static Compat<String> ITEM_PICKUP = new Compat<String>()
            .d("random.pop")
            .v("1.9", "entity.item.pickup")
            .search();

    public static Compat<String> THORNS_HIT = new Compat<String>()
            .d("damage.thorns")
            .v("1.9", "enchant.thorns.hit")
            .search();

    public static Compat<String> LEVEL_UP = new Compat<String>()
            .d("LEVEL_UP")
            .v("1.9", "ENTITY_PLAYER_LEVELUP")
            .search();

    public static Compat<String> FALL_BIG = new Compat<String>()
            .d("FALL_BIG")
            .v("1.9", "ENTITY_HOSTILE_BIG_FALL")
            .search();

    public static Compat<String> LEVEL_DOWN = new Compat<String>()
            .d("ANVIL_BREAK")
            .v("1.9", "ENTITY_WITHER_BREAK_BLOCK")
            .search();

    /**
     * Plays a sound resolved from a version-specific {@link Sound} constant name
     * (as produced by the {@code Compat<String>} fields above) without baking a
     * static reference to {@link Sound#valueOf(String)} into the bytecode.
     * <p>
     * {@code org.bukkit.Sound} changed kind across Minecraft versions: it is an
     * {@code enum} up to ~1.21.2 and an {@code interface} (with a static
     * {@code valueOf} kept for source-compat) from 1.21.3 on. A compiled
     * {@code Sound.valueOf} call carries the constant-pool tag of whichever API
     * it was built against — {@code Methodref} for the enum, {@code
     * InterfaceMethodref} for the interface. When that tag disagrees with the
     * {@code Sound} kind on the running server the JVM throws
     * {@link IncompatibleClassChangeError} while linking the call.
     * <p>
     * Resolving {@code valueOf} reflectively sidesteps the static call entirely,
     * so the sound resolves correctly whether {@code Sound} is an enum or an
     * interface at runtime, on every supported version (1.8 through current).
     *
     * @param location  where to play the sound (ignored if {@code null} or worldless)
     * @param soundName the {@link Sound} constant name, e.g. from {@link #LEVEL_UP}
     * @param volume    the sound volume
     * @param pitch     the sound pitch
     */
    public static void play(Location location, String soundName, float volume, float pitch) {
        if (location == null || location.getWorld() == null || soundName == null || SOUND_VALUE_OF == null) {
            return;
        }
        try {
            Sound sound = (Sound) SOUND_VALUE_OF.invoke(null, soundName);
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (Throwable t) {
            // Unknown constant for this version, or an unexpected linkage error:
            // skip the sound rather than aborting the surrounding event, but keep
            // a broken Compat mapping diagnosable instead of failing silently.
            MyPetApi.getLogger().warning("Could not play sound '" + soundName + "': " + t);
        }
    }
}
