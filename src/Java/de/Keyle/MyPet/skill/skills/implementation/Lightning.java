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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.ISkillActive;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.LightningInfo;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.entity.LightningStrike;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import java.util.*;

public class Lightning extends LightningInfo implements ISkillInstance, ISkillActive
{
    private static Random random = new Random();
    public static Map<LightningStrike, MyPet> lightningList = new HashMap<LightningStrike, MyPet>();
    public static boolean isStriking = false;
    private MyPet myPet;

    public Lightning(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public boolean isActive()
    {
        return chance > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof LightningInfo)
        {
            boolean valuesEdit = false;
            if (upgrade.getProperties().getValue().containsKey("chance"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_chance") || ((StringTag) upgrade.getProperties().getValue().get("addset_chance")).getValue().equals("add"))
                {
                    chance += ((IntTag) upgrade.getProperties().getValue().get("chance")).getValue();
                }
                else
                {
                    chance = ((IntTag) upgrade.getProperties().getValue().get("chance")).getValue();
                }
                valuesEdit = true;
            }
            if (!quiet && valuesEdit)
            {
                myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_LightningChance")).replace("%petname%", myPet.petName).replace("%chance%", "" + chance));
            }
        }
    }

    public String getFormattedValue()
    {
        return chance + "%";
    }

    public void reset()
    {
        chance = 0;
    }

    public boolean activate()
    {
        return random.nextDouble() <= chance / 100.;
    }


    public static int countLightnings()
    {
        removeDeadLightnings();
        return lightningList.size();
    }

    public static boolean isSkillLightning(LightningStrike lightningStrike)
    {
        removeDeadLightnings();
        return lightningList.containsKey(lightningStrike);
    }

    private static void removeDeadLightnings()
    {
        List<LightningStrike> deadLightningStrikes = new ArrayList<LightningStrike>();
        for (LightningStrike bolt : lightningList.keySet())
        {
            if (bolt.isDead())
            {
                deadLightningStrikes.add(bolt);
            }
        }
        for (LightningStrike bolt : deadLightningStrikes)
        {
            lightningList.remove(bolt);
        }
    }

    @Override
    public ISkillInstance cloneSkill()
    {
        Lightning newSkill = new Lightning(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}