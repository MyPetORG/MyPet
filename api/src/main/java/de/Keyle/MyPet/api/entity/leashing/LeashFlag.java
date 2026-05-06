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

package de.Keyle.MyPet.api.entity.leashing;

import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * A single condition that must be satisfied before a player can leash
 * (tame) a mob as a pet. Implementations are registered with
 * {@link LeashFlagManager} and discovered by their {@link LeashFlagName}
 * annotation.
 * <p>
 * Multiple flags may be configured per pet type in the skilltree; all
 * configured flags must pass for the leash attempt to succeed.
 * <p>
 * Implementations range from simple state checks ({@code BabyFlag},
 * {@code TamedFlag}) to integration gates that defer to external plugins
 * ({@code WorldGuardFlag}, {@code MythicMobsFlag}).
 */
public interface LeashFlag {

    /**
     * Returns a colored prefix component for use in player-facing
     * feedback messages: a green checkmark (✔) when the flag passes, or
     * a red cross (✘) when it fails.
     *
     * @param right {@code true} for pass (green ✔), {@code false} for
     *              fail (red ✘)
     */
    static Component getComponentPrefix(boolean right) {
        if (right) {
            return Component.text("✔ ").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
        } else {
            return Component.text("✘ ").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true);
        }
    }

    /**
     * Evaluates whether this leash condition is met for the given
     * player/entity pair.
     *
     * @param player   the player attempting to leash the mob
     * @param entity   the mob being leashed
     * @param damage   the final damage dealt to the mob in the leash hit
     *                 (some flags gate on minimum damage)
     * @param settings flag-specific parameters from the skilltree config
     *                 (e.g. required size, chance percentage)
     * @return {@code true} if this condition passes
     */
    boolean check(Player player, LivingEntity entity, double damage, Settings settings);

    /**
     * Returns a player-facing message explaining why this flag failed,
     * shown when the leash attempt is denied. Return {@code null} (the
     * default) to suppress feedback for this flag.
     */
    default Component getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        return null;
    }

    /**
     * If {@code true}, the {@code /petinfo} helper display skips this
     * flag - useful for flags that are always-pass or purely internal.
     */
    default boolean ignoredByHelper() {
        return false;
    }
}
