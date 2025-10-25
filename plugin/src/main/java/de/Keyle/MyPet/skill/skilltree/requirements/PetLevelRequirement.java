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

package de.Keyle.MyPet.skill.skilltree.requirements;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;
import de.Keyle.MyPet.api.skill.skilltree.requirements.RequirementName;
import de.Keyle.MyPet.api.util.configuration.settings.Setting;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;

@RequirementName("PetLevel")
public class PetLevelRequirement implements Requirement {

    @Override
    public boolean check(Skilltree skilltree, MyPet pet, Settings settings) {
        int level = pet.getExperience().getLevel();

        if (settings.map().containsKey("min") || settings.map().containsKey("max")) {
            boolean levelInRange = true;
            if (settings.map().containsKey("min") && Util.isInt(settings.map().get("min").getValue())) {
                levelInRange = level >= Integer.parseInt(settings.map().get("min").getValue());
            }
            if (settings.map().containsKey("max") && Util.isInt(settings.map().get("max").getValue())) {
                levelInRange = levelInRange && level <= Integer.parseInt(settings.map().get("max").getValue());
            }
            return levelInRange;
        } else {
            for (Setting setting : settings.all()) {
                if (Util.isInt(setting.getKey())) {
                    return level >= Integer.parseInt(setting.getKey());
                }
            }
        }
        return true;
    }
}
