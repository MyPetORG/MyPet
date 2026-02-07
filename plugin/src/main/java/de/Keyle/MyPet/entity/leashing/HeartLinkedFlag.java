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

import de.Keyle.MyPet.api.entity.leashing.LeashFlag;
import de.Keyle.MyPet.api.entity.leashing.LeashFlagName;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * LeashFlag for entities that can only be captured through non-damage means.
 * Specifically designed for heart-linked Creaking which must be captured by
 * destroying their Creaking Heart block rather than through combat damage.
 *
 * This flag passes when damage is 0 (indicating heart-based capture) and
 * fails when damage is greater than 0 (indicating combat-based capture).
 */
@LeashFlagName("HeartLinked")
public class HeartLinkedFlag implements LeashFlag {

    @Override
    public boolean check(Player player, LivingEntity entity, double damage, Settings settings) {
        // Pass when damage is 0 (heart destruction capture)
        // Fail when damage > 0 (combat-based capture attempt)
        return damage == 0;
    }

    @Override
    public String getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        return Translation.getString("Message.Command.CaptureHelper.Requirement.HeartLinked", player);
    }
}
