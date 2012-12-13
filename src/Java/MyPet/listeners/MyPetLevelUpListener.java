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

package Java.MyPet.listeners;

import Java.MyPet.event.MyPetLevelUpEvent;
import Java.MyPet.skill.MyPetSkillTreeSkill;
import Java.MyPet.util.MyPetLanguage;
import Java.MyPet.util.MyPetUtil;
import Java.MyPet.entity.types.MyPet;
import Java.MyPet.skill.MyPetSkillTree;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPetLevelUpListener implements Listener
{
    @EventHandler
    public void onLevelUp(MyPetLevelUpEvent eventMyPet)
    {
        MyPet myPet = eventMyPet.getPet();
        if (!eventMyPet.isQuiet())
        {
            myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_LvlUp")).replace("%petname%", myPet.petName).replace("%lvl%", "" + eventMyPet.getLevel()));
        }
        int lvl = eventMyPet.getLevel();
        MyPetSkillTree skillTree = myPet.getSkillTree();
        if (skillTree.hasLevel(lvl))
        {
            Map<String,Integer> skillLevelUpgradeCount = new HashMap<String, Integer>();
            List<MyPetSkillTreeSkill> skillList = skillTree.getLevel(lvl).getSkills();
            for (MyPetSkillTreeSkill skill : skillList)
            {
                if(skillLevelUpgradeCount.containsKey(skill.getName()))
                {
                    skillLevelUpgradeCount.put(skill.getName(),skillLevelUpgradeCount.get(skill.getName())+1);
                }
                else
                {
                    skillLevelUpgradeCount.put(skill.getName(),1);
                }
            }
            for(String skill : skillLevelUpgradeCount.keySet())
            {
                if (myPet.getSkillSystem().hasSkill(skill))
                {
                    if (eventMyPet.isQuiet())
                    {
                        myPet.getSkillSystem().getSkill(skill).setLevel(myPet.getSkillSystem().getSkill(skill).getLevel() + skillLevelUpgradeCount.get(skill));
                    }
                    else
                    {
                        myPet.getSkillSystem().getSkill(skill).upgrade(skillLevelUpgradeCount.get(skill));
                    }
                }
            }
        }
        myPet.setHealth(myPet.getMaxHealth());
        myPet.setHungerValue(100);
    }
}