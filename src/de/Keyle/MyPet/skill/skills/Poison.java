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

import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;

import java.util.Random;

public class Poison extends MyPetGenericSkill
{
    public static int ChancePerLevel = 5;
    private int ChanceToPoison = ChancePerLevel;
    private static Random random = new Random();

    public Poison()
    {
        super("Poison");
        ChanceToPoison = ChancePerLevel;
    }

    @Override
    public void upgrade()
    {
        Level++;
        ChanceToPoison = Level * ChancePerLevel;
        MWolf.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_PoisonChance")).replace("%wolfname%", MWolf.Name).replace("%chance%", "" + ChanceToPoison));
    }

    public boolean getPoison()
    {
        return random.nextDouble() <= ChanceToPoison / 100;
    }
}