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
import org.spout.nbt.ByteTag;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import java.io.InputStream;
import java.util.Locale;

@SkillName("Beacon")
@SkillProperties(
        parameterNames = {"1_1", "1_3", "1_11", "1_8", "1_5", "2_1", "2_3", "2_11", "2_8", "2_5", "2_10", "duration", "range", "addset_duration", "addset_range"},
        parameterTypes = {NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Int, NBTdatatypes.Double, NBTdatatypes.String, NBTdatatypes.String},
        parameterDefaultValues = {"true", "true", "true", "true", "true", "true", "true", "true", "true", "true", "true", "8", "5", "add", "add"})
public class BeaconInfo extends MyPetSkillTreeSkill implements ISkillInfo
{
    private static String defaultHTML = null;

    protected double range = 0;
    protected int duration = 0;

    public BeaconInfo(boolean addedByInheritance)
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
        for (int i = 0 ; i < 11 ; i++)
        {
            String name = getClass().getAnnotation(SkillProperties.class).parameterNames()[i];
            if (getProperties().getValue().containsKey(name))
            {
                if (!((ByteTag) getProperties().getValue().get(name)).getBooleanValue())
                {
                    html = html.replace("name=\"" + name + "\" checked", "name=\"" + name + "\"");
                }
            }
        }
        if (getProperties().getValue().containsKey("duration"))
        {
            int duration = ((IntTag) getProperties().getValue().get("duration")).getValue();
            html = html.replace("name=\"duration\" value=\"0\"", "name=\"duration\" value=\"" + duration + "\"");
            if (getProperties().getValue().containsKey("addset_duration"))
            {
                if (((StringTag) getProperties().getValue().get("addset_duration")).getValue().equals("set"))
                {
                    html = html.replace("name=\"addset_duration\" value=\"add\" checked", "name=\"addset_duration\" value=\"add\"");
                    html = html.replace("name=\"addset_duration\" value=\"set\"", "name=\"addset_duration\" value=\"set\" checked");
                }
            }
        }
        if (getProperties().getValue().containsKey("range"))
        {
            double range = ((DoubleTag) getProperties().getValue().get("range")).getValue();
            html = html.replace("name=\"range\" value=\"0.00\"", "name=\"range\" value=\"" + String.format(Locale.ENGLISH, "%1.2f", range) + "\"");
            if (getProperties().getValue().containsKey("addset_range"))
            {
                if (((StringTag) getProperties().getValue().get("addset_range")).getValue().equals("set"))
                {
                    html = html.replace("name=\"addset_range\" value=\"add\" checked", "name=\"addset_range\" value=\"add\"");
                    html = html.replace("name=\"addset_range\" value=\"set\"", "name=\"addset_range\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    public ISkillInfo cloneSkill()
    {
        BeaconInfo newSkill = new BeaconInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}
