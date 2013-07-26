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

import de.Keyle.MyPet.gui.skilltreecreator.skills.Beacon;
import de.Keyle.MyPet.gui.skilltreecreator.skills.SkillPropertiesPanel;
import de.Keyle.MyPet.skill.skills.SkillName;
import de.Keyle.MyPet.skill.skills.SkillProperties;
import de.Keyle.MyPet.skill.skills.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.skill.skilltree.SkillTreeSkill;

@SkillName("Beacon")
@SkillProperties(
        parameterNames = {"1_1", "1_3", "1_11", "1_8", "1_5", "2_1", "2_3", "2_11", "2_8", "2_5", "2_10", "duration", "range", "addset_duration", "addset_range"},
        parameterTypes = {NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Int, NBTdatatypes.Double, NBTdatatypes.String, NBTdatatypes.String},
        parameterDefaultValues = {"true", "true", "true", "true", "true", "true", "true", "true", "true", "true", "true", "8", "5", "add", "add"})
public class BeaconInfo extends SkillTreeSkill implements ISkillInfo
{
    private SkillPropertiesPanel panel = null;

    protected double range = 0;
    protected int duration = 0;

    public BeaconInfo(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public SkillPropertiesPanel getGuiPanel()
    {
        if (panel == null)
        {
            panel = new Beacon(this.getProperties());
        }
        return panel;
    }

    public ISkillInfo cloneSkill()
    {
        BeaconInfo newSkill = new BeaconInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}