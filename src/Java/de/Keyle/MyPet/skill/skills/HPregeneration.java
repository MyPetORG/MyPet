/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.event.entity.EntityRegainHealthEvent;

@SkillName("HPregeneration")
@SkillProperties(
        parameterNames = {"hp", "time", "addset_hp", "addset_time"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.Int, NBTdatatypes.String, NBTdatatypes.String},
        parameterDefaultValues = {"1", "1", "add", "add"})
public class HPregeneration extends MyPetGenericSkill
{
    public static int START_REGENERATION_TIME = 60;
    private int timeCounter = START_REGENERATION_TIME;
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
            if (upgrade.getProperties().hasKey("hp"))
            {
                if (!upgrade.getProperties().hasKey("addset_hp") || upgrade.getProperties().getString("addset_hp").equals("add"))
                {
                    increaseHpBy += upgrade.getProperties().getInt("hp");
                }
                else
                {
                    increaseHpBy = upgrade.getProperties().getInt("hp");
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().hasKey("time"))
            {
                if (!upgrade.getProperties().hasKey("addset_time") || upgrade.getProperties().getString("addset_time").equals("add"))
                {
                    timeDecrease += upgrade.getProperties().getInt("time");
                }
                else
                {
                    timeDecrease = upgrade.getProperties().getInt("time");
                }
                if (timeDecrease < 1)
                {
                    timeDecrease = 1;
                }
                timeCounter = START_REGENERATION_TIME - timeDecrease;
                valuesEdit = true;
            }
            if (!quiet && valuesEdit)
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddHPregeneration")).replace("%petname%", myPet.petName).replace("%sec%", "" + (START_REGENERATION_TIME - timeDecrease)).replace("%hp%", "" + increaseHpBy));
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return "+" + increaseHpBy + MyPetLanguage.getString("Name_HP") + " ->" + (START_REGENERATION_TIME - timeDecrease) + "sec";
    }

    public void reset()
    {
        timeDecrease = 0;
        increaseHpBy = 0;
        timeCounter = START_REGENERATION_TIME;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("hp"))
        {
            html = html.replace("hp\" value=\"0\"", "hp\" value=\"" + getProperties().getInt("hp") + "\"");
            if (getProperties().hasKey("addset_hp"))
            {
                if (getProperties().getString("addset_hp").equals("set"))
                {
                    html = html.replace("name=\"addset_hp\" value=\"add\" checked", "name=\"addset_hp\" value=\"add\"");
                    html = html.replace("name=\"addset_hp\" value=\"set\"", "name=\"addset_hp\" value=\"set\" checked");
                }
            }
        }
        if (getProperties().hasKey("time"))
        {
            html = html.replace("time\" value=\"0\"", "time\" value=\"" + getProperties().getInt("time") + "\"");
            if (getProperties().hasKey("addset_time"))
            {
                if (getProperties().getString("addset_time").equals("set"))
                {
                    html = html.replace("name=\"addset_time\" value=\"add\" checked", "name=\"addset_time\" value=\"add\"");
                    html = html.replace("name=\"addset_time\" value=\"set\"", "name=\"addset_time\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    public void schedule()
    {
        if (increaseHpBy > 0 && myPet.getStatus() == PetState.Here)
        {
            if (timeCounter-- <= 0)
            {
                myPet.getCraftPet().getHandle().heal(increaseHpBy, EntityRegainHealthEvent.RegainReason.REGEN);
                timeCounter = START_REGENERATION_TIME - timeDecrease;
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