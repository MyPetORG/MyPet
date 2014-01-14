/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.gui.skilltreecreator.skills.SkillPropertiesPanel;
import de.Keyle.MyPet.gui.skilltreecreator.skills.Stomp;
import de.Keyle.MyPet.skill.skills.SkillName;
import de.Keyle.MyPet.skill.skills.SkillProperties;
import de.Keyle.MyPet.skill.skilltree.SkillTreeSkill;

@SkillName("Stomp")
@SkillProperties(
        parameterNames = {"chance", "addset_chance", "damage", "addset_damage"},
        parameterTypes = {SkillProperties.NBTdatatypes.Int, SkillProperties.NBTdatatypes.String, SkillProperties.NBTdatatypes.Double, SkillProperties.NBTdatatypes.String},
        parameterDefaultValues = {"5", "add", "3.0", "add"})
public class StompInfo extends SkillTreeSkill implements ISkillInfo {
    private SkillPropertiesPanel panel = null;

    protected int chance = 0;
    protected double damage = 0;

    public StompInfo(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public SkillPropertiesPanel getGuiPanel() {
        if (panel == null) {
            panel = new Stomp(this.getProperties());
        }
        return panel;
    }

    public ISkillInfo cloneSkill() {
        StompInfo newSkill = new StompInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}