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

import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.NBTTagCompound;

public class Behavior extends MyPetGenericSkill
{
    private BehaviorState behavior = BehaviorState.Normal;

    public static enum BehaviorState
    {
        Normal, Friendly, Aggressive, Raid, Farm
    }

    public Behavior()
    {
        super("Behavior");
    }

    public void setBehavior(BehaviorState behaviorState)
    {
        behavior = behaviorState;
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", behavior.name()));
        if (behavior == BehaviorState.Friendly)
        {
            myPet.getPet().setTarget(null);
        }
    }

    public void activateBehavior(BehaviorState behaviorState)
    {
        if (level > 0)
        {
            behavior = behaviorState;
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", behavior.name()));
            if (behavior == BehaviorState.Friendly)
            {
                myPet.getPet().setTarget(null);
            }
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_LearnedSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
        }
    }

    public BehaviorState getBehavior()
    {
        return behavior;
    }

    @Override
    public void upgrade()
    {
        level = 1;
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_LearnedSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
    }

    @Override
    public void activate()
    {
        if (level > 0)
        {
            if (behavior == BehaviorState.Normal)
            {
                behavior = BehaviorState.Friendly;
                myPet.getPet().setTarget(null);
            }
            else if (behavior == BehaviorState.Friendly)
            {
                behavior = BehaviorState.Aggressive;
            }
            else if (behavior == BehaviorState.Aggressive || behavior == BehaviorState.Raid)
            {
                behavior = BehaviorState.Normal;
            }
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", behavior.name()));
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_LearnedSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
        }
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        behavior = BehaviorState.valueOf(nbtTagCompound.getString("Mode"));
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound(skillName);
        nbtTagCompound.setString("Mode", behavior.name());
        return nbtTagCompound;
    }
}