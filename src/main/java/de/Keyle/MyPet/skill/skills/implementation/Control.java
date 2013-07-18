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
import de.Keyle.MyPet.skill.skills.info.ControlInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.Material;

public class Control extends ControlInfo implements ISkillInstance
{
    public static int CONTROL_ITEM = Material.LEASH.getId();
    private Location moveTo;
    private Location prevMoveTo;
    private boolean active = false;
    private MyPet myPet;

    public Control(boolean addedByInheritance)
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
        if (upgrade instanceof ControlInfo)
        {
            if (!quiet && !active)
            {
                String controlItemName = WordUtils.capitalizeFully(MyPetBukkitUtil.getMaterialName(CONTROL_ITEM).replace("_", " "));
                myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.Skill.Control.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), controlItemName));

            }
            active = true;
        }
    }

    public String getFormattedValue()
    {
        return "";
    }

    public Location getLocation()
    {
        Location tmpMoveTo = moveTo;
        moveTo = null;
        return tmpMoveTo;
    }

    public Location getLocation(boolean delete)
    {
        Location tmpMoveTo = moveTo;
        if (delete)
        {
            moveTo = null;
        }
        return tmpMoveTo;
    }

    public void setMoveTo(Location loc)
    {
        if (!active)
        {
            return;
        }
        if (prevMoveTo != null)
        {
            if (loc.distance(prevMoveTo) > 1)
            {
                moveTo = loc;
                prevMoveTo = loc;
            }
        }
        else
        {
            moveTo = loc;
        }
    }

    public void reset()
    {
        active = false;
        moveTo = null;
        prevMoveTo = null;
    }

    @Override
    public ISkillInstance cloneSkill()
    {
        Control newSkill = new Control(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}