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

package de.Keyle.MyPet.test.skill.skills;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.MyPetSkills;
import de.Keyle.MyPet.skill.SkillProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SkillPropertiesTest
{
    @Test
    public void testSkillProperties()
    {
        MyPetPlugin.registerSkills();
        for (Class<? extends MyPetSkillTreeSkill> registeredSkills : MyPetSkills.getRegisteredSkills())
        {
            SkillProperties sn = registeredSkills.getAnnotation(SkillProperties.class);
            if (sn != null)
            {
                assertEquals(sn.parameterNames().length, sn.parameterTypes().length);
                assertEquals(sn.parameterNames().length, sn.parameterDefaultValues().length);
            }
        }
    }
}
