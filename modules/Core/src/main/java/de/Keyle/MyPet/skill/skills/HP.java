/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skills.HPInfo;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

public class HP extends HPInfo implements SkillInstance {
    private MyPet myPet;

    public HP(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return hpIncrease > 0;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof HPInfo) {
            if (upgrade.getProperties().getCompoundData().containsKey("hp")) {
                int hp = upgrade.getProperties().getAs("hp", TagInt.class).getIntData();
                upgrade.getProperties().getCompoundData().remove("hp");
                TagDouble TagDouble = new TagDouble(hp);
                upgrade.getProperties().getCompoundData().put("hp_double", TagDouble);
            }
            if (upgrade.getProperties().getCompoundData().containsKey("hp_double")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_hp") || ((TagString) upgrade.getProperties().getAs("addset_hp", TagString.class)).getStringData().equals("add")) {
                    hpIncrease += upgrade.getProperties().getAs("hp_double", TagDouble.class).getDoubleData();
                } else {
                    hpIncrease = upgrade.getProperties().getAs("hp_double", TagDouble.class).getDoubleData();
                }

                if (getMyPet().getStatus() == PetState.Here) {
                    getMyPet().getEntity().setMaxHealth(getMyPet().getMaxHealth());
                }

                if (!quiet) {
                    myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.Hp.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), myPet.getMaxHealth()));
                }
            }
        }
    }

    public String getFormattedValue() {
        return "+" + hpIncrease;
    }

    public void reset() {
        hpIncrease = 0;
    }

    public double getHpIncrease() {
        return hpIncrease;
    }

    @Override
    public SkillInstance cloneSkill() {
        HP newSkill = new HP(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}