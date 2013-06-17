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
import de.Keyle.MyPet.skill.skills.implementation.Behavior.BehaviorState;

import java.util.HashMap;
import java.util.Map;

@SkillName("Behavior")
@SkillProperties(
        parameterNames = {"friend", "aggro", "farm", "raid", "duel"},
        parameterTypes = {NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean, NBTdatatypes.Boolean},
        parameterDefaultValues = {"true", "true", "true", "true", "true"})
public class BehaviorInfo extends MyPetSkillTreeSkill implements ISkillInfo
{
    protected Map<BehaviorState, Boolean> behaviorActive = new HashMap<BehaviorState, Boolean>();

    public BehaviorInfo(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public ISkillInfo cloneSkill()
    {
        BehaviorInfo newSkill = new BehaviorInfo(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}
