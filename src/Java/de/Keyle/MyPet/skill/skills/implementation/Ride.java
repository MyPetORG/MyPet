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
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.RideInfo;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import org.bukkit.Material;
import org.spout.nbt.FloatTag;
import org.spout.nbt.StringTag;

public class Ride extends RideInfo implements ISkillInstance
{
    public static Material ITEM = Material.STRING;
    private boolean active = false;
    private MyPet myPet;

    public Ride(boolean addedByInheritance)
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
        return active;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof RideInfo)
        {
            active = true;
            if (upgrade.getProperties().getValue().containsKey("speed"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_speed") || ((StringTag) upgrade.getProperties().getValue().get("addset_speed")).getValue().equals("add"))
                {
                    speed += ((FloatTag) upgrade.getProperties().getValue().get("speed")).getValue();
                }
                else
                {
                    speed = ((FloatTag) upgrade.getProperties().getValue().get("speed")).getValue();
                }
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_AddRide")).replace("%petname%", myPet.petName)/*.replace("%speed%",String.format("%1.3f", upgrade.getProperties().getDouble("add")))*/);
                }
            }
        }
    }

    public String getFormattedValue()
    {
        return MyPetLanguage.getString("Name_Speed") + " +" + String.format("%1.3f", speed);
    }

    public void reset()
    {
        speed = 0F;
        active = false;
    }

    public float getSpeed()
    {
        return speed;
    }

    @Override
    public ISkillInstance cloneSkill()
    {
        Ride newSkill = new Ride(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}