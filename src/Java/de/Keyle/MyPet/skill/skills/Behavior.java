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

import de.Keyle.MyPet.entity.types.enderman.MyEnderman;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.EntityLiving;
import net.minecraft.server.v1_4_6.NBTTagCompound;

public class Behavior extends MyPetGenericSkill
{
    private BehaviorState behavior = BehaviorState.Normal;

    public static enum BehaviorState
    {
        Normal(true), Friendly(true), Aggressive(true), Raid(true), Farm(true);

        boolean active;

        BehaviorState(boolean active)
        {
            this.active = active;
        }

        public void setActive(boolean active)
        {
            if (this != Normal)
            {
                this.active = active;
            }
        }

        public boolean isActive()
        {
            return this.active;
        }
    }

    public Behavior()
    {
        super("Behavior", 1);
    }

    public void setBehavior(BehaviorState behaviorState)
    {
        behavior = behaviorState;
        myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", MyPetLanguage.getString("Name_" + behavior.name())));
        if (behavior == BehaviorState.Friendly)
        {
            myPet.getCraftPet().setTarget(null);
        }
    }

    public void activateBehavior(BehaviorState behaviorState)
    {
        if (level > 0)
        {
            behavior = behaviorState;
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", MyPetLanguage.getString("Name_" + behavior.name())));
            if (behavior == BehaviorState.Friendly)
            {
                myPet.getCraftPet().getHandle().b((EntityLiving) null);
            }
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
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
                if (BehaviorState.Friendly.isActive())
                {
                    behavior = BehaviorState.Friendly;
                    myPet.getCraftPet().setTarget(null);
                }
                else if (BehaviorState.Aggressive.isActive())
                {
                    behavior = BehaviorState.Aggressive;
                }
            }
            else if (behavior == BehaviorState.Friendly)
            {
                if (BehaviorState.Aggressive.isActive())
                {
                    behavior = BehaviorState.Aggressive;
                }
                else
                {
                    behavior = BehaviorState.Normal;
                }
            }
            else
            {
                behavior = BehaviorState.Normal;
            }
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_BehaviorState")).replace("%petname%", myPet.petName).replace("%mode%", MyPetLanguage.getString("Name_" + behavior.name())));
        }
        else
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_NoSkill")).replace("%petname%", myPet.petName).replace("%skill%", this.skillName));
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

    @Override
    public void schedule()
    {
        if (myPet instanceof MyEnderman)
        {
            MyEnderman myEnderman = (MyEnderman) myPet;
            if (behavior == BehaviorState.Aggressive)
            {
                if (!myEnderman.isScreaming())
                {
                    myEnderman.setScreaming(true);
                }
            }
            else
            {
                if (myEnderman.isScreaming())
                {
                    myEnderman.setScreaming(false);
                }
            }
        }
    }

    public void reset()
    {
        super.reset();
        behavior = BehaviorState.Normal;
    }
}