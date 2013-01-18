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
@SkillProperties(parameterNames = {"chance", "duration", "addset_chance", "addset_duration"}, parameterTypes = {NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.String, NBTdatatypes.String})
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
            if (upgrade.getProperties().hasKey("chance"))
            {
                if (!upgrade.getProperties().hasKey("addset_chance") || upgrade.getProperties().getString("addset_chance").equals("add"))
                {
                    chance += upgrade.getProperties().getInt("chance");
                }
                else
                {
                    chance = upgrade.getProperties().getInt("chance");
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("duration"))
            {
                if (!upgrade.getProperties().hasKey("addset_duration") || upgrade.getProperties().getString("addset_duration").equals("add"))
                {
                    duration += upgrade.getProperties().getInt("duration");
                }
                else
                {
                    duration = upgrade.getProperties().getInt("duration");
                }
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
        if (getProperties().hasKey("chance"))
        {
            html = html.replace("chance\" value=\"0\"", "chance\" value=\"" + getProperties().getInt("chance") + "\"");
            if (getProperties().hasKey("addset_chance"))
            {
                if (getProperties().getString("addset_chance").equals("set"))
                {
                    html = html.replace("name=\"addset_chance\" value=\"add\" checked", "name=\"addset_chance\" value=\"add\"");
                    html = html.replace("name=\"addset_chance\" value=\"set\"", "name=\"addset_chance\" value=\"set\" checked");
                }
            }
        }
        if (getProperties().hasKey("duration"))
        {
            html = html.replace("duration\" value=\"0\"", "duration\" value=\"" + getProperties().getInt("duration") + "\"");
            if (getProperties().hasKey("addset_duration"))
            {
                if (getProperties().getString("addset_duration").equals("set"))
                {
                    html = html.replace("name=\"addset_duration\" value=\"add\" checked", "name=\"addset_duration\" value=\"add\"");
                    html = html.replace("name=\"addset_duration\" value=\"set\"", "name=\"addset_duration\" value=\"set\" checked");
                }
            }
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