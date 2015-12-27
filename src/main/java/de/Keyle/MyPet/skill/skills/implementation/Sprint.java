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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.SprintInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;

public class Sprint extends SprintInfo implements ISkillInstance {
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

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof SprintInfo) {
            if (!quiet && !active) {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Sprint.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName()));

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
    public ISkillInstance cloneSkill() {
        Sprint newSkill = new Sprint(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}