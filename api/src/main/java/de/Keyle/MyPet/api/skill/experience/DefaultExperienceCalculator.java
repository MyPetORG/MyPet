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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.api.entity.MyPet;

public class DefaultExperienceCalculator implements ExperienceCalculator {

    public double getExpByLevel(MyPet myPet, int level) {
        if (level <= 1) {
            return 0;
        }
        double tmpExp = 0;
        int tmpLvl = 1;

        while (tmpLvl < level) {
            tmpExp += 7 + Math.floor((tmpLvl - 1) * 3.5);
            tmpLvl++;
        }
        return tmpExp;
    }

    @Override
    public long getVersion() {
        return 1;
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "MyPet";
    }
}