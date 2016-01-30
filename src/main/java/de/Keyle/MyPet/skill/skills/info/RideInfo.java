/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.gui.skilltreecreator.skills.Ride;
import de.Keyle.MyPet.gui.skilltreecreator.skills.SkillPropertiesPanel;
import de.Keyle.MyPet.skill.skills.SkillName;
import de.Keyle.MyPet.skill.skills.SkillProperties;
import de.Keyle.MyPet.skill.skilltree.SkillTreeSkill;

@SkillName(value = "Ride", translationNode = "Name.Skill.Ride")
@SkillProperties(
        parameterNames = {"speed_percent", "addset_speed", "jump_height", "addset_jump_height", "can_fly"},
        parameterTypes = {SkillProperties.NBTdatatypes.Int, SkillProperties.NBTdatatypes.String, SkillProperties.NBTdatatypes.Double, SkillProperties.NBTdatatypes.String, SkillProperties.NBTdatatypes.Boolean},
        parameterDefaultValues = {"5", "add", "1.25", "set", "false"})
public class RideInfo extends SkillTreeSkill implements ISkillInfo {
    private SkillPropertiesPanel panel = null;

    protected int speedPercent = 0;
    protected double jumpHeigth = 0D;
    protected boolean canFly = false;

    public RideInfo(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public SkillPropertiesPanel getGuiPanel() {
        if (panel == null) {
            panel = new Ride(this.getProperties());
        }
        return panel;
    }

    public ISkillInfo cloneSkill() {
        RideInfo newSkill = new RideInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}