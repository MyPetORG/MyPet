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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.info.HPInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

public class HP extends HPInfo implements ISkillInstance
{
    private MyPet myPet;

    public HP(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public boolean isActive()
    {
        return hpIncrease > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof HPInfo)
        {
            if (upgrade.getProperties().getValue().containsKey("hp"))
            {
                int hp = ((IntTag) upgrade.getProperties().getValue().get("hp")).getValue();
                upgrade.getProperties().getValue().remove("hp");
                DoubleTag doubleTag = new DoubleTag("hp_double", hp);
                upgrade.getProperties().getValue().put("hp_double", doubleTag);
            }
            if (upgrade.getProperties().getValue().containsKey("hp_double"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_hp") || ((StringTag) upgrade.getProperties().getValue().get("addset_hp")).getValue().equals("add"))
                {
                    hpIncrease += ((DoubleTag) upgrade.getProperties().getValue().get("hp_double")).getValue();
                }
                else
                {
                    hpIncrease = ((DoubleTag) upgrade.getProperties().getValue().get("hp_double")).getValue();
                }

                if (getMyPet().getStatus() == PetState.Here)
                {
                    getMyPet().getCraftPet().setMaxHealth(getMyPet().getMaxHealth());
                }

                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.Skill.Hp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), myPet.getMaxHealth()));
                }
            }
        }
    }

    public String getFormattedValue()
    {
        return "+" + hpIncrease;
    }

    public void reset()
    {
        hpIncrease = 0;
    }

    public double getHpIncrease()
    {
        return hpIncrease;
    }

    @Override
    public ISkillInstance cloneSkill()
    {
        HP newSkill = new HP(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}