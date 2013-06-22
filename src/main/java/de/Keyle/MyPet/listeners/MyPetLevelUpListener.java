/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetConfiguration;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class MyPetLevelUpListener implements Listener
{
    @EventHandler
    public void onLevelUp(MyPetLevelUpEvent event)
    {
        MyPet myPet = event.getPet();
        if (!event.isQuiet())
        {
            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.LvlUp", event.getOwner().getLanguage())).replace("%petname%", myPet.getPetName()).replace("%lvl%", "" + event.getLevel()));

            if (MyPetExperience.FIREWORK_ON_LEVELUP)
            {
                Location location = myPet.getLocation();
                location.setY(location.getY() - 1.5);
                location.setPitch(-90);
                Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
                FireworkEffect fwe = FireworkEffect.builder().with(Type.STAR).withColor(Color.fromRGB(MyPetConfiguration.LEVELUP_FIREWORK_COLOR)).withTrail().withFlicker().build();
                FireworkMeta fwm = fw.getFireworkMeta();
                fwm.addEffect(fwe);
                fwm.addEffect(fwe);
                fwm.addEffect(fwe);
                fwm.setPower(0);
                fw.setFireworkMeta(fwm);
            }
        }
        int lvl = event.getLevel();
        MyPetSkillTree skillTree = myPet.getSkillTree();
        if (skillTree != null && skillTree.hasLevel(lvl))
        {
            List<ISkillInfo> skillList = skillTree.getLevel(lvl).getSkills();
            for (ISkillInfo skill : skillList)
            {
                myPet.getSkills().getSkill(skill.getName()).upgrade(skill, event.isQuiet());
            }
        }
        if (myPet.getStatus() == PetState.Here)
        {
            myPet.setHealth(myPet.getMaxHealth());
            myPet.setHungerValue(100);
        }
    }
}