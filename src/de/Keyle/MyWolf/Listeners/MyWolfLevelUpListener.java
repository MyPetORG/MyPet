/*
* Copyright (C) 2011 Keyle
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

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.event.LevelUpEvent;
import de.Keyle.MyWolf.util.MyWolfConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.SpoutManager;

public class MyWolfLevelUpListener implements Listener
{
    @EventHandler(priority = EventPriority.NORMAL)
    public void onLevelUp(LevelUpEvent event)
    {
        if (event.getWolf().Status == WolfState.Here && MyWolfConfig.SpoutSounds)
        {
            SpoutManager.getSoundManager().playGlobalCustomSoundEffect(MyWolfPlugin.Plugin, MyWolfConfig.SpoutSoundLevelup, false, event.getWolf().getLocation(), 25);
        }
        if (ConfigBuffer.SkillPerLevel.containsKey(event.getLevel()))
        {
            for (String skill : ConfigBuffer.SkillPerLevel.get(event.getLevel()))
            {
                if (ConfigBuffer.RegisteredSkills.containsKey(skill))
                {
                    ConfigBuffer.RegisteredSkills.get(skill).activate(event.getWolf(), 0);
                }
            }
        }
    }
}
