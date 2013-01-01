/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;

import java.util.Random;

public class Fire extends MyPetGenericSkill
{
    public static int chancePerLevel = 5;
    public static int duration = 3;
    private static Random random = new Random();

    public Fire()
    {
        super("Fire");
    }

    @Override
    public void upgrade()
    {
        super.upgrade();
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_FireChance")).replace("%petname%", myPet.petName).replace("%chance%", "" + level * chancePerLevel));
    }

    public boolean getFire()
    {
        return random.nextDouble() <= level * chancePerLevel / 100.;
    }
}