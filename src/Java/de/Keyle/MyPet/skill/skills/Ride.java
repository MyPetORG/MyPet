/*
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.Material;

@SkillName("Ride")
@SkillProperties(parameterNames = {"add"}, parameterTypes = {NBTdatatypes.Float})
public class Ride extends MyPetGenericSkill
{
    public static Material item = Material.STRING;
    private float speed = 0F;
    private boolean active = false;

    public Ride(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public void upgrade(MyPetSkillTreeSkill upgrade, boolean quiet)
    {
        if (upgrade instanceof Ride)
        {
            active = true;
            if (upgrade.getProperties().hasKey("add"))
            {
                speed += upgrade.getProperties().getFloat("add");
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AddRide")).replace("%petname%", myPet.petName)/*.replace("%speed%",String.format("%1.3f", upgrade.getProperties().getDouble("add")))*/);
                }
            }
        }
    }

    @Override
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
    public String getHtml()
    {
        String html = super.getHtml();
        if (getProperties().hasKey("add"))
        {
            html = html.replace("value=\"0.0\"", "value=\"" + getProperties().getFloat("add") + "\"");
        }
        return html;
    }

    @Override
    public MyPetSkillTreeSkill cloneSkill()
    {
        MyPetSkillTreeSkill newSkill = new Ride(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}