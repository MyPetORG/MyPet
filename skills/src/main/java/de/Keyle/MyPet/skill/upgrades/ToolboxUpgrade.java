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

package de.Keyle.MyPet.skill.upgrades;

import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.Upgrade;
import de.Keyle.MyPet.api.skill.modifier.UpgradeBooleanModifier;
import de.Keyle.MyPet.api.skill.skills.Toolbox;

import java.util.EnumMap;
import java.util.Map;

@SkillName("Toolbox")
public class ToolboxUpgrade implements Upgrade<Toolbox> {

    protected final Map<Toolbox.Station, UpgradeBooleanModifier> stationModifiers = new EnumMap<>(Toolbox.Station.class);

    /** Sets the unlock modifier for one station; {@code null} modifiers are ignored. */
    public ToolboxUpgrade setStationModifier(Toolbox.Station station, UpgradeBooleanModifier modifier) {
        if (modifier != null) {
            stationModifiers.put(station, modifier);
        }
        return this;
    }

    @Override
    public void apply(Toolbox skill) {
        stationModifiers.forEach((station, modifier) -> skill.getStation(station).addUpgrade(modifier));
    }

    @Override
    public void invert(Toolbox skill) {
        stationModifiers.forEach((station, modifier) -> skill.getStation(station).removeUpgrade(modifier));
    }
}
