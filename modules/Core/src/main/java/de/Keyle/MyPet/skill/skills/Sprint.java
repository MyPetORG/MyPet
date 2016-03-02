/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.SprintInfo;
import de.Keyle.MyPet.api.util.locale.Translation;

public class Sprint extends SprintInfo implements SkillInstance {
    private boolean active = false;
    private MyPet myPet;

    public Sprint(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return active;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof SprintInfo) {
            if (!quiet && !active) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Sprint.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName()));

            }
            active = true;
        }
    }

    public String getFormattedValue() {
        return "";
    }

    public void reset() {
        active = false;
    }

    @Override
    public SkillInstance cloneSkill() {
        Sprint newSkill = new Sprint(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}