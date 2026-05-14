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

package de.Keyle.MyPet.entity.leashing;

import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.util.configuration.settings.Setting;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.OptionalDouble;

@LeashFlagName("BelowHP")
public class BelowHpFlag implements LeashFlag {

    @Override
    public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
        for (Setting setting : settings.entries()) {
            OptionalDouble threshold = parseThreshold(setting, entity.getMaxHealth());
            if (threshold.isPresent()) {
                return entity.getHealth() - damage <= threshold.getAsDouble();
            }
        }
        return true;
    }

    @Override
    public Component getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        double health = 0;
        for (Setting setting : settings.entries()) {
            OptionalDouble threshold = parseThreshold(setting, entity.getMaxHealth());
            if (threshold.isPresent()) {
                health = threshold.getAsDouble();
                break;
            }
        }
        return Locale.getFormattedComponent("Message.Command.CaptureHelper.Requirement.BelowHP", player, String.format("%1.2f", health));
    }

    /**
     * Resolve a single setting to an absolute HP threshold. Supports two
     * syntaxes that share this flag's positional slot:
     * <ul>
     *   <li>{@code "50%"} — percent of {@code maxHealth}</li>
     *   <li>{@code "5"} or {@code "5.0"} — absolute HP value</li>
     * </ul>
     */
    private static OptionalDouble parseThreshold(Setting setting, double maxHealth) {
        String raw = setting.asString();
        if (raw.endsWith("%")) {
            try {
                int percent = Integer.parseInt(raw.substring(0, raw.length() - 1));
                return OptionalDouble.of(maxHealth * percent / 100.0);
            } catch (NumberFormatException ignored) {
                return OptionalDouble.empty();
            }
        }
        return setting.getDouble();
    }
}
