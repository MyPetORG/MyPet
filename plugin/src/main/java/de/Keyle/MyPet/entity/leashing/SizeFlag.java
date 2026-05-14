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
import org.bukkit.entity.Slime;

import java.util.OptionalInt;

@LeashFlagName("Size")
public class SizeFlag implements LeashFlag {

    @Override
    public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
        if (!(entity instanceof Slime slime)) {
            return true;
        }
        for (Setting setting : settings.entries()) {
            OptionalInt fixed = setting.getInt();
            if (fixed.isPresent()) {
                return slime.getSize() >= fixed.getAsInt();
            }
        }
        boolean correctSize = slime.getSize() >= settings.getInt("min").orElse(Integer.MIN_VALUE);
        correctSize &= slime.getSize() <= settings.getInt("max").orElse(Integer.MAX_VALUE);
        return correctSize;
    }

    @Override
    public Component getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        if (!(entity instanceof Slime)) {
            return null;
        }
        for (Setting setting : settings.entries()) {
            if (setting.getInt().isPresent()) {
                return Locale.getFormattedComponent("Message.Command.CaptureHelper.Requirement.Size.Equal", player, setting.asString());
            }
        }
        Component message = null;
        if (settings.getInt("min").isPresent()) {
            message = Locale.getFormattedComponent("Message.Command.CaptureHelper.Requirement.Size.Min", player, settings.getString("min").orElse(""));
        }
        if (settings.getInt("max").isPresent()) {
            Component maxComponent = Locale.getFormattedComponent("Message.Command.CaptureHelper.Requirement.Size.Min", player, settings.getString("max").orElse(""));
            message = message != null ? message.append(Component.text(", ")).append(maxComponent) : maxComponent;
        }
        return message;
    }
}
