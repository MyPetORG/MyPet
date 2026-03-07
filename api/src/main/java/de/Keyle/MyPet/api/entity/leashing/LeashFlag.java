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

package de.Keyle.MyPet.api.entity.leashing;

import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface LeashFlag {

    static Component getComponentPrefix(boolean right) {
        if (right) {
            return Component.text("\u2714 ").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
        } else {
            return Component.text("\u2718 ").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true);
        }
    }

    boolean check(Player player, LivingEntity entity, double damage, Settings settings);

    default Component getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        return null;
    }

    default boolean ignoredByHelper() {
        return false;
    }
}
