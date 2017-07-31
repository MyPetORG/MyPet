/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

package de.Keyle.MyPet.api.skill.skills;

import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillName;
import de.Keyle.MyPet.api.skill.SkillProperties;
import de.Keyle.MyPet.api.skill.SkillProperties.NBTdatatypes;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeSkill;

@SkillName(value = "Beacon", translationNode = "Name.Skill.Beacon")
@SkillProperties(
        parameterNames = {"selection_count", "addset_selection_count"},
        parameterTypes = {NBTdatatypes.Int, NBTdatatypes.String},
        parameterDefaultValues = {"1", "set"})
public class BeaconInfo extends SkillTreeSkill implements SkillInfo {
    protected double range = 0;
    protected int duration = 0;
    protected int selectableBuffs = 0;

    public BeaconInfo(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public SkillInfo cloneSkill() {
        BeaconInfo newSkill = new BeaconInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}