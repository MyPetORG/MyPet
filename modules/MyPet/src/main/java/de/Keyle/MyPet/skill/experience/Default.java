/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.skill.experience;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.skill.experience.Experience;

public class Default extends Experience {
    private int lastLevel = 1;
    private double lastExpL = Double.NaN;
    private double lastExpC = Double.NaN;
    private double lastExpR = Double.NaN;
    private double lastCurrentExp = 0.0;
    private double lastRequiredExp = 0.0;

    public Default(ActiveMyPet myPet) {
        super(myPet);
    }

    public int getLevel(double exp) {
        if (exp == 0) {
            return 1;
        }

        if (lastExpL == exp) {
            return lastLevel;
        }
        lastExpL = exp;

        // Minecraft:   E = 7 + roundDown( n * 3.5)

        double tmpExp = exp;
        int tmpLvl = 0;

        while (tmpExp >= 7 + Math.floor(tmpLvl * 3.5)) {
            tmpExp -= 7 + Math.floor(tmpLvl * 3.5);
            tmpLvl++;
        }
        lastLevel = tmpLvl + 1;
        return lastLevel;
    }

    public double getRequiredExp(double exp) {
        if (lastExpR == exp) {
            return lastRequiredExp;
        }
        lastExpR = exp;

        lastRequiredExp = 7 + Math.floor((getLevel(exp) - 1) * 3.5);
        return lastRequiredExp;
    }

    public double getCurrentExp(double exp) {
        if (lastExpC == exp) {
            return lastCurrentExp;
        }
        lastExpC = exp;

        double tmpExp = exp;
        int tmplvl = 0;

        while (tmpExp >= 7 + Math.floor(tmplvl * 3.5)) {
            tmpExp -= 7 + Math.floor(tmplvl * 3.5);
            tmplvl++;
        }
        lastCurrentExp = tmpExp;
        return lastCurrentExp;
    }

    @Override
    public double getExpByLevel(int level) {
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

    public boolean isUsable() {
        return true;
    }
}