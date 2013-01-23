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

@SkillName("Thorns")
@SkillProperties(
        parameterNames = {"chance", "addset_chance"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.String},
        parameterDefaultValues = {"5", "add"})
public class Thorns extends MyPetGenericSkill
{
    private int chance = 0;
    private static Random random = new Random();

    public Thorns(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return chance > 0;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Thorns)
        {
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
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_ThornsChance")).replace("%petname%", myPet.petName).replace("%chance%", "" + chance));
                }
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return chance + "%";
    }

    public void reset()
    {
        chance = 0;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("chance"))
        {
            html = html.replace("value=\"0\"", "value=\"" + getProperties().getInt("chance") + "\"");
            if (getProperties().hasKey("addset_chance"))
            {
                if (getProperties().getString("addset_chance").equals("set"))
                {
                    html = html.replace("name=\"addset_chance\" value=\"add\" checked", "name=\"addset_chance\" value=\"add\"");
                    html = html.replace("name=\"addset_chance\" value=\"set\"", "name=\"addset_chance\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    public boolean isActivated()
    {
        return random.nextDouble() < chance / 100.;
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Thorns(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}