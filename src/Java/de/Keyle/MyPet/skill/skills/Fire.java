/*
 * Copyright (C) 2011-2013 Keyle
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
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;

import java.util.Random;

@SkillName("Fire")
@SkillProperties(parameterNames = {"add", "duration"}, parameterTypes = {NBTdatatypes.Int, NBTdatatypes.Int})
public class Fire extends MyPetGenericSkill
{
    private int chance = 0;
    private int duration = 0;
    private static Random random = new Random();

    public Fire(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return chance > 0 && duration > 0;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Fire)
        {
            boolean valuesEdit = false;
            if (upgrade.getProperties().hasKey("add"))
            {
                chance += upgrade.getProperties().getInt("add");
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("duration"))
            {
                duration += upgrade.getProperties().getInt("duration");
                valuesEdit = true;
            }
            if (!quiet && valuesEdit)
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_FireChance")).replace("%petname%", myPet.petName).replace("%chance%", "" + chance).replace("%duration%", "" + duration));
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return chance + "% -> " + duration + "sec";
    }

    public void reset()
    {
        chance = 0;
        duration = 0;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("add"))
        {
            html = html.replace("add\" value=\"0\"", "add\" value=\"" + getProperties().getInt("add") + "\"");
        }
        if (getProperties().hasKey("duration"))
        {
            html = html.replace("duration\" value=\"0\"", "duration\" value=\"" + getProperties().getInt("duration") + "\"");
        }
        return html;
    }

    public boolean getFire()
    {
        return random.nextDouble() <= chance / 100.;
    }

    public int getDuration()
    {
        return duration;
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Fire(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}