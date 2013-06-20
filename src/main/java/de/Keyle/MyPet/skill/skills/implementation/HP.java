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
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
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
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.AddHp", myPet.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()).replace("%maxhealth%", "" + (MyPet.getStartHP(myPet.getClass()) + hpIncrease)));
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

    public int getHpIncrease()
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