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
import de.Keyle.MyPet.skill.skills.info.DamageInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

public class Damage extends DamageInfo implements ISkillInstance
{
    private boolean isPassive = true;
    private MyPet myPet;

    public Damage(boolean addedByInheritance)
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
        return damageIncrease > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof DamageInfo)
        {
            if (getMyPet().getDamage() > 0)
            {
                isPassive = false;
            }
            if (upgrade.getProperties().getValue().containsKey("damage"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_damage") || ((StringTag) upgrade.getProperties().getValue().get("addset_damage")).getValue().equals("add"))
                {
                    damageIncrease += ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                }
                else
                {
                    damageIncrease = ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                }
                if (damageIncrease > 0 && isPassive)
                {
                    if (myPet.getStatus() == PetState.Here)
                    {
                        getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().setPathfinder();
                    }
                    isPassive = false;
                }
                else if (damageIncrease <= 0 && !isPassive)
                {
                    if (myPet.getStatus() == PetState.Here)
                    {
                        getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                        getMyPet().getCraftPet().getHandle().setPathfinder();
                    }
                    isPassive = true;
                }
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_AddDamage")).replace("%petname%", myPet.petName).replace("%dmg%", "" + damageIncrease));
                }
            }
        }
    }

    public String getFormattedValue()
    {
        return "+" + damageIncrease;
    }

    public void reset()
    {
        damageIncrease = 0;
    }

    public int getDamageIncrease()
    {
        return damageIncrease;
    }

    public ISkillInstance cloneSkill()
    {
        Damage newSkill = new Damage(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}