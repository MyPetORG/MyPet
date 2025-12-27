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

package de.Keyle.MyPet.entity.leashing;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.util.configuration.settings.Setting;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;

@LeashFlagName("Size")
public class SizeFlag implements LeashFlag {

    @Override
    public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
        if (entity instanceof Slime) {
            for (Setting setting : settings.all()) {
                if (Util.isInt(setting.getKey())) {
                    return ((Slime) entity).getSize() >= Integer.parseInt(setting.getKey());
                }
            }
            boolean correctSize = true;
            if (settings.map().containsKey("min") && Util.isInt(settings.map().get("min").getValue())) {
                correctSize = ((Slime) entity).getSize() >= Integer.parseInt(settings.map().get("min").getValue());
            }
            if (settings.map().containsKey("max") && Util.isInt(settings.map().get("max").getValue())) {
                correctSize = correctSize && ((Slime) entity).getSize() <= Integer.parseInt(settings.map().get("max").getValue());
            }
            return correctSize;
        }
        return true;
    }

    @Override
    public String getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        if (entity instanceof Slime) {
            for (Setting setting : settings.all()) {
                if (Util.isInt(setting.getKey())) {
                    return Util.formatText(Translation.getString("Message.Command.CaptureHelper.Requirement.Size.Equal", player), setting.getKey());
                }
            }
            String message = null;
            if (settings.map().containsKey("min") && Util.isInt(settings.map().get("min").getValue())) {
                message = Util.formatText(Translation.getString("Message.Command.CaptureHelper.Requirement.Size.Min", player), settings.map().get("min").getValue());

            }
            if (settings.map().containsKey("max") && Util.isInt(settings.map().get("max").getValue())) {
                if (message != null) {
                    message += ", ";
                } else {
                    message = "";
                }
                message += Util.formatText(Translation.getString("Message.Command.CaptureHelper.Requirement.Size.Min", player), settings.map().get("max").getValue());
            }
            return message;
        }
        return null;
    }
}
