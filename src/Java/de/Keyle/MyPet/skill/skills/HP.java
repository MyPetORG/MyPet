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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

@SkillName("HP")
@SkillProperties(
        parameterNames = {"hp", "addset_hp"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.String},
        parameterDefaultValues = {"1", "add"})
public class HP extends MyPetGenericSkill
{
    private int hpIncrease = 0;

    public HP(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return hpIncrease > 0;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof HP)
        {
            if (upgrade.getProperties().getValue().containsKey("hp"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_hp") || ((StringTag) upgrade.getProperties().getValue().get("addset_hp")).getValue().equals("add"))
                {
                    hpIncrease += ((IntTag) upgrade.getProperties().getValue().get("hp")).getValue();
                }
                else
                {
                    hpIncrease = ((IntTag) upgrade.getProperties().getValue().get("hp")).getValue();
                }

                if (getMyPet().getStatus() == PetState.Here)
                {
                    getMyPet().getCraftPet().setMaxHealth(getMyPet().getMaxHealth());
                }

                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddHP")).replace("%petname%", myPet.petName).replace("%maxhealth%", "" + (MyPet.getStartHP(myPet.getClass()) + hpIncrease)));
                }
            }
        }
    }

    @Override
    public String getFormattedValue()
    {
        return "+" + hpIncrease;
    }

    public void reset()
    {
        hpIncrease = 0;
    }

    public int getHpIncrease()
    {
        return hpIncrease;
    }

    @Override
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().getValue().containsKey("hp"))
        {
            int hp = ((IntTag) getProperties().getValue().get("hp")).getValue();
            html = html.replace("value=\"0\"", "value=\"" + hp + "\"");
            if (getProperties().getValue().containsKey("addset_hp"))
            {
                if (((StringTag) getProperties().getValue().get("addset_hp")).getValue().equals("set"))
                {
                    html = html.replace("name=\"addset_hp\" value=\"add\" checked", "name=\"addset_hp\" value=\"add\"");
                    html = html.replace("name=\"addset_hp\" value=\"set\"", "name=\"addset_hp\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new HP(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}