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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.Material;

import java.util.Locale;

@SkillName("Ride")
@SkillProperties(
        parameterNames = {"speed", "addset_speed"},
        parameterTypes = {NBTdatatypes.Float, NBTdatatypes.String},
        parameterDefaultValues = {"0.01", "add"})
public class Ride extends MyPetGenericSkill
{
    public static Material ITEM = Material.STRING;
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
            if (upgrade.getProperties().hasKey("speed"))
            {
                if (!upgrade.getProperties().hasKey("addset_speed") || upgrade.getProperties().getString("addset_speed").equals("speed"))
                {
                    speed += upgrade.getProperties().getFloat("speed");
                }
                else
                {
                    speed = upgrade.getProperties().getFloat("speed");
                }
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
        if (getProperties().hasKey("speed"))
        {
            html = html.replace("value=\"0.000\"", "value=\"" + String.format(Locale.ENGLISH, "%1.3f", getProperties().getFloat("speed")) + "\"");
            if (getProperties().hasKey("addset_speed"))
            {
                if (getProperties().getString("addset_speed").equals("set"))
                {
                    html = html.replace("name=\"addset_speed\" value=\"add\" checked", "name=\"addset_speed\" value=\"add\"");
                    html = html.replace("name=\"addset_speed\" value=\"set\"", "name=\"addset_speed\" value=\"set\" checked");
                }
            }
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