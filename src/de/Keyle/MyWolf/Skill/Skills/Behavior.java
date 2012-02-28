/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.Skill.Skills;

import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.configuration.MyWolfYamlConfiguration;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.NBTTagCompound;

public class Behavior extends MyWolfGenericSkill
{
    private BehaviorState Behavior = BehaviorState.Normal;

    public static enum BehaviorState
    {
        Normal, Friendly, Aggressive, Raid
    }

    public Behavior()
    {
        super("Behavior");
    }

    public void setBehavior(BehaviorState behaviorState)
    {
        Behavior = behaviorState;
        MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_BehaviorState")).replace("%wolfname%", MWolf.Name).replace("%mode%", Behavior.name()));
        if(Behavior == BehaviorState.Friendly)
        {
            MWolf.Wolf.setTarget(null);
        }
    }

    public void activateBehavior(BehaviorState behaviorState)
    {
        if (Level > 0)
        {
            Behavior = behaviorState;
            MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_BehaviorState")).replace("%wolfname%", MWolf.Name).replace("%mode%", Behavior.name()));
            if(Behavior == BehaviorState.Friendly)
            {
                MWolf.Wolf.setTarget(null);
            }
        }
        else
        {
            MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_LearnedSkill")).replace("%wolfname%", MWolf.Name).replace("%skill%", this.Name));
        }
    }

    public BehaviorState getBehavior()
    {
        return Behavior;
    }

    @Override
    public void upgrade()
    {
        Level = 1;
        MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_LearnedSkill")).replace("%wolfname%", MWolf.Name).replace("%skill%", this.Name));
    }

    @Override
    public void activate()
    {
        if (Level > 0)
        {
            if (Behavior == BehaviorState.Normal)
            {
                Behavior = BehaviorState.Friendly;
                MWolf.Wolf.setTarget(null);
            }
            else if (Behavior == BehaviorState.Friendly)
            {
                Behavior = BehaviorState.Aggressive;
            }
            else if (Behavior == BehaviorState.Aggressive || Behavior == BehaviorState.Raid)
            {
                Behavior = BehaviorState.Normal;
            }
            MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_BehaviorState")).replace("%wolfname%", MWolf.Name).replace("%mode%", Behavior.name()));
        }
        else 
        {
            MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_LearnedSkill")).replace("%wolfname%", MWolf.Name).replace("%skill%", this.Name));
        }
    }

    public void load(MyWolfYamlConfiguration configuration)
    {
        String b = configuration.getConfig().getString("Wolves." + MWolf.getOwner().getName() + ".behavior", "QwE");
        if(b.equals("QwE"))
        {
            b = configuration.getConfig().getString("Wolves." + MWolf.getOwner().getName() + ".skills.behavior", "Normal");
        }
        Behavior = BehaviorState.valueOf(b);
    }

    @Override
    public void load(NBTTagCompound nbtTagCompound)
    {
        Behavior = BehaviorState.valueOf(nbtTagCompound.getString("Mode"));
    }

    @Override
    public void save(NBTTagCompound nbtTagCompound)
    {
        nbtTagCompound.setString("Mode", Behavior.name());
    }
}
