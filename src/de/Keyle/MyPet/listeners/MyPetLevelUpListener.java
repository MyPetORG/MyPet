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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyPetLevelUpListener implements Listener
{
    @EventHandler
    public void onLevelUp(MyPetLevelUpEvent eventMyPet)
    {
        MyWolf MWolf = eventMyPet.getWolf();
        if (!eventMyPet.isQuiet())
        {
            MWolf.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_LvlUp")).replace("%wolfname%", MWolf.Name).replace("%lvl%", "" + eventMyPet.getLevel()));
        }
        int lvl = eventMyPet.getLevel();
        MyPetSkillTree st = MWolf.skillTree;
        String[] Skills = st.getSkills(lvl);
        if (Skills.length > 0)
        {
            for (String skill : Skills)
            {
                if (MWolf.skillSystem.hasSkill(skill))
                {
                    if (eventMyPet.isQuiet())
                    {
                        MWolf.skillSystem.getSkill(skill).setLevel(MWolf.skillSystem.getSkill(skill).getLevel() + 1);
                    }
                    else
                    {
                        MWolf.skillSystem.getSkill(skill).upgrade();
                    }
                }
            }
        }

        MWolf.setHealth(MWolf.getMaxHealth());
    }
}