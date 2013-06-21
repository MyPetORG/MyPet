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
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import java.io.InputStream;

@SkillName("Thorns")
@SkillProperties(
        parameterNames = {"chance", "addset_chance"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.String},
        parameterDefaultValues = {"5", "add"})
public class ThornsInfo extends MyPetSkillTreeSkill implements ISkillInfo
{
    private static String defaultHTML = null;

    protected int chance = 0;

    public ThornsInfo(boolean addedByInheritance)
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
        if (getProperties().getValue().containsKey("chance"))
        {
            int chance = ((IntTag) getProperties().getValue().get("chance")).getValue();
            html = html.replace("value=\"0\"", "value=\"" + chance + "\"");
            if (getProperties().getValue().containsKey("addset_chance"))
            {
                if (((StringTag) getProperties().getValue().get("addset_chance")).getValue().equals("set"))
                {
                    html = html.replace("name=\"addset_chance\" value=\"add\" checked", "name=\"addset_chance\" value=\"add\"");
                    html = html.replace("name=\"addset_chance\" value=\"set\"", "name=\"addset_chance\" value=\"set\" checked");
                }
            }
        }
        return html;
    }

    public ISkillInfo cloneSkill()
    {
        ThornsInfo newSkill = new ThornsInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}
