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

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.Skill.MyWolfSkillTree;
import de.Keyle.MyWolf.event.MyWolfLevelUpEvent;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyWolfLevelUpListener implements Listener
{
    @EventHandler()
    public void onLevelUp(MyWolfLevelUpEvent eventMyWolf)
    {
        MyWolf MWolf = eventMyWolf.getWolf();
        MWolf.sendMessageToOwner(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_LvlUp")).replace("%wolfname%", MWolf.Name).replace("%lvl%", ""+eventMyWolf.getLevel()));

        int lvl = eventMyWolf.getLevel();
        MyWolfSkillTree st = MWolf.SkillTree;
        String[] Skills = st.getSkills(lvl);
        if (Skills.length > 0)
        {
            for (String skill : Skills)
            {
                if(MWolf.SkillSystem.getSkill(skill) != null)
                {
                    MWolf.SkillSystem.getSkill(skill).upgrade();
                }
            }
        }
    }
}
