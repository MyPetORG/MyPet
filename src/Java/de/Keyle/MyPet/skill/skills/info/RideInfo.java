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

package de.Keyle.MyPet.skill.skills.info;

import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.SkillName;
import de.Keyle.MyPet.skill.SkillProperties;
import de.Keyle.MyPet.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.util.MyPetUtil;
import org.spout.nbt.FloatTag;
import org.spout.nbt.StringTag;

import java.io.InputStream;
import java.util.Locale;

@SkillName("Ride")
@SkillProperties(
        parameterNames = {"speed", "addset_speed"},
        parameterTypes = {NBTdatatypes.Float, NBTdatatypes.String},
        parameterDefaultValues = {"0.01", "add"})
public class RideInfo extends MyPetSkillTreeSkill implements ISkillInfo
{
    private static String defaultHTML = null;

    protected float speed = 0F;

    public RideInfo(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public String getHtml()
    {
        if (defaultHTML == null)
        {
            InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("html/skills/" + getName() + ".html");
            if (htmlStream == null)
            {
                htmlStream = this.getClass().getClassLoader().getResourceAsStream("html/skills/_default.html");
                if (htmlStream == null)
                {
                    return "NoSkillPropertieViewNotFoundError";
                }
            }
            defaultHTML = MyPetUtil.convertStreamToString(htmlStream).replace("#Skillname#", getName());
        }

        String html = defaultHTML;
        if (getProperties().getValue().containsKey("speed"))
        {
            float speed = ((FloatTag) getProperties().getValue().get("speed")).getValue();
            html = html.replace("value=\"0.000\"", "value=\"" + String.format(Locale.ENGLISH, "%1.3f", speed) + "\"");
            if (getProperties().getValue().containsKey("addset_speed"))
            {
                if (((StringTag) getProperties().getValue().get("addset_speed")).getValue().equals("set"))
                {
                    html = html.replace("name=\"addset_speed\" value=\"add\" checked", "name=\"addset_speed\" value=\"add\"");
                    html = html.replace("name=\"addset_speed\" value=\"set\"", "name=\"addset_speed\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    public ISkillInfo cloneSkill()
    {
        RideInfo newSkill = new RideInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}
