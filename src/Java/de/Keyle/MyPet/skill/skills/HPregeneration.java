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

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;

@SkillName("HPregeneration")
@SkillProperties(parameterNames = {"add", "remove"}, parameterTypes = {NBTdatatypes.Int, NBTdatatypes.Int})
public class HPregeneration extends MyPetGenericSkill
{
    public static int healtregenTime = 60;
    private int timeCounter = healtregenTime;
    private int timeDecrease = 0;
    private int increaseHpBy = 0;

    public HPregeneration(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return increaseHpBy > 0;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof HPregeneration)
        {
            boolean valuesEdit = false;
            if (upgrade.getProperties().hasKey("add"))
            {
                increaseHpBy += upgrade.getProperties().getInt("add");
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("remove"))
            {
                timeDecrease += upgrade.getProperties().getInt("remove");
                if (timeDecrease < 1)
                {
                    timeDecrease = 1;
                }
                timeCounter -= upgrade.getProperties().getInt("remove");
                valuesEdit = true;
            }
            if (!quiet && valuesEdit)
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddHPregeneration")).replace("%petname%", myPet.petName).replace("%sec%", "" + (healtregenTime - timeDecrease)).replace("%add%", "" + increaseHpBy));
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return "+" + increaseHpBy + MyPetLanguage.getString("Name_HP") + " ->" + (healtregenTime - timeDecrease) + "sec";
    }

    public void reset()
    {
        timeDecrease = 0;
        increaseHpBy = 0;
        timeCounter = healtregenTime;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("add"))
        {
            html = html.replace("add\" value=\"1\"", "add\" value=\"" + getProperties().getInt("add") + "\"");
        }
        if (getProperties().hasKey("remove"))
        {
            html = html.replace("remove\" value=\"1\"", "remove\" value=\"" + getProperties().getInt("remove") + "\"");
        }
        return html;
    }

    public void schedule()
    {
        if (increaseHpBy > 0 && myPet.status == PetState.Here)
        {
            if (timeCounter-- <= 0)
            {
                //myPet.getCraftPet().getHandle().heal(1, EntityRegainHealthEvent.RegainReason.REGEN);
                timeCounter = healtregenTime - timeDecrease;
            }
        }
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new HPregeneration(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}