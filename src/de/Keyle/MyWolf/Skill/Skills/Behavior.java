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
import de.Keyle.MyWolf.util.MyWolfConfiguration;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class Behavior extends MyWolfGenericSkill
{
    private BehaviorState Behavior = BehaviorState.Normal;

    private int schedulerCounter = 0;

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
    }

    public void activateBehavior(BehaviorState behaviorState)
    {
        if (Level > 0)
        {
            Behavior = behaviorState;
            MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_BehaviorState")).replace("%wolfname%", MWolf.Name).replace("%mode%", Behavior.name()));
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

    @Override
    public void schedule()
    {
        schedulerCounter++;
        if(schedulerCounter >= 10)
        {
            schedulerCounter = 0;
            if (Behavior == BehaviorState.Aggressive)
            {
                if (MWolf.Wolf.getTarget() == null || MWolf.Wolf.getTarget().isDead())
                {
                    for (Entity e : MWolf.Wolf.getNearbyEntities(10, 10, 10))
                    {
                        if (MyWolfUtil.getCreatureType(e) != null)
                        {
                            MWolf.Wolf.setTarget((LivingEntity) e);
                        }
                    }
                }
            }
        }
    }

    public void load(MyWolfConfiguration configuration)
    {
        String b = configuration.getConfig().getString("Wolves." + MWolf.getOwnerName() + ".behavior", "QwE");
        if(b.equals("QwE"))
        {
            b = configuration.getConfig().getString("Wolves." + MWolf.getOwnerName() + ".skills.behavior", "Normal");
        }
        Behavior = BehaviorState.valueOf(b);
    }

    @Override
    public void save(MyWolfConfiguration configuration)
    {
        configuration.getConfig().set("Wolves." + MWolf.getOwnerName() + ".skills.behavior", Behavior.name());
    }
}
