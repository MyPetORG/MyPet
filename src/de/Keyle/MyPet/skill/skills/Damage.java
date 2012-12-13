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

import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;

public class Damage extends MyPetGenericSkill
{
    private boolean isPassive = true;

    public Damage()
    {
        super("Damage");
    }

    public void upgrade(int value)
    {
        if (getMyPet().getDamage() > 0)
        {
            isPassive = false;
        }
        if (value > 0)
        {
            value--;
            this.level += value;
            if (maxLevel != -1 && this.level > maxLevel)
            {
                level = maxLevel - 1;
            }
            if (isPassive)
            {
                getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                getMyPet().getCraftPet().getHandle().setPathfinder();
                isPassive = false;
            }
            upgrade();
        }
    }

    public void setLevel(int level)
    {
        if (getMyPet().getDamage() > 0)
        {
            isPassive = false;
        }
        super.setLevel(level);
        if (isPassive)
        {
            getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().setPathfinder();
            isPassive = false;
        }
    }

    @Override
    public void upgrade()
    {
        if (getMyPet().getDamage() > 0)
        {
            isPassive = false;
        }
        super.upgrade();
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddDemage")).replace("%petname%", myPet.petName).replace("%dmg%", "" + level));
        if (isPassive)
        {
            getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().setPathfinder();
            isPassive = false;
        }
    }
}