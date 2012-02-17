/*
* Copyright (C) 2011-2012 Keyle
*
* This file is part of MyWolf.
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

package de.Keyle.MyWolf.Listeners;

import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.Skill.MyWolfSkill;
import de.Keyle.MyWolf.event.MyWolfLevelUpEvent;
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class MyWolfLevelUpListener implements Listener
{
    @EventHandler()
    public void onLevelUp(MyWolfLevelUpEvent eventMyWolf)
    {

        if (eventMyWolf.getWolf().Status == WolfState.Here && MyWolfConfig.SpoutSounds)
        {
            //SpoutManager.getSoundManager().playGlobalCustomSoundEffect(MyWolfPlugin.Plugin, MyWolfConfig.SpoutSoundLevelup, false, eventMyWolf.getWolf().getLocation(), 25);
            SpoutManager.getSoundManager().playCustomMusic(MyWolfPlugin.Plugin, (SpoutPlayer) eventMyWolf.getOwner(),MyWolfConfig.SpoutSoundLevelup,true);
            eventMyWolf.getWolf().sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_LvlUp")).replace("%wolfname%", eventMyWolf.getWolf().Name).replace("%lvl%", ""+eventMyWolf.getLevel()));
        }
        if (MyWolfSkill.SkillPerLevel.containsKey(eventMyWolf.getLevel()))
        {
            for (String skill : MyWolfSkill.SkillPerLevel.get(eventMyWolf.getLevel()))
            {
                if (MyWolfSkill.RegisteredSkills.containsKey(skill))
                {
                    MyWolfSkill.RegisteredSkills.get(skill).activate(eventMyWolf.getWolf(), 0);
                }
            }
        }
    }
}
