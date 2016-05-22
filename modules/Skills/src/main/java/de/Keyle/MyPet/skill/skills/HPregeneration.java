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
import de.Keyle.MyPet.api.skill.skills.HPregenerationInfo;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagDouble;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;
import org.bukkit.Color;

public class HPregeneration extends HPregenerationInfo implements SkillInstance, Scheduler {
    private int timeCounter = 0;
    private MyPet myPet;
    protected boolean particles = false;

    public HPregeneration(boolean addedByInheritance) {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet) {
        this.myPet = myPet;
    }

    public MyPet getMyPet() {
        return myPet;
    }

    public boolean isActive() {
        return increaseHpBy > 0;
    }

    public void upgrade(SkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof HPregenerationInfo) {
            boolean valuesEdit = false;
            if (upgrade.getProperties().getCompoundData().containsKey("hp")) {
                int hp = upgrade.getProperties().getAs("hp", TagInt.class).getIntData();
                upgrade.getProperties().getCompoundData().remove("hp");
                TagDouble TagDouble = new TagDouble(hp);
                upgrade.getProperties().getCompoundData().put("hp_double", TagDouble);
            }
            if (upgrade.getProperties().getCompoundData().containsKey("hp_double")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_hp") || upgrade.getProperties().getAs("addset_hp", TagString.class).getStringData().equals("add")) {
                    increaseHpBy += upgrade.getProperties().getAs("hp_double", TagDouble.class).getDoubleData();
                } else {
                    increaseHpBy = upgrade.getProperties().getAs("hp_double", TagDouble.class).getDoubleData();
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getCompoundData().containsKey("time")) {
                if (!upgrade.getProperties().getCompoundData().containsKey("addset_time") || upgrade.getProperties().getAs("addset_time", TagString.class).getStringData().equals("add")) {
                    regenTime -= upgrade.getProperties().getAs("time", TagInt.class).getIntData();
                } else {
                    regenTime = upgrade.getProperties().getAs("time", TagInt.class).getIntData();
                }
                if (regenTime < 1) {
                    regenTime = 1;
                }
                timeCounter = regenTime;
                valuesEdit = true;
            }
            if (!quiet && valuesEdit) {
                myPet.getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skill.HpRegeneration.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), increaseHpBy, regenTime));
            }
        }
    }

    public String getFormattedValue() {
        return "+" + increaseHpBy + Translation.getString("Name.HP", myPet.getOwner().getLanguage()) + " ->" + regenTime + "sec";
    }

    public void reset() {
        regenTime = 0;
        increaseHpBy = 0;
        timeCounter = 0;
    }

    public void schedule() {
        if (myPet.getStatus() == PetState.Here) {
            if (increaseHpBy > 0) {
                if (timeCounter-- <= 0) {
                    if (myPet.getHealth() < myPet.getMaxHealth()) {
                        if (!particles) {
                            particles = true;
                            myPet.getEntity().get().getHandle().showPotionParticles(Color.LIME);
                        }
                        myPet.getEntity().get().setHealth(myPet.getHealth() + increaseHpBy);
                    }
                    timeCounter = regenTime;
                } else {
                    particles = false;
                }
            }
            if (particles) {
                particles = false;
                myPet.getEntity().get().getHandle().hidePotionParticles();
            }
        } else if (particles) {
            particles = false;
        }
    }

    @Override
    public SkillInstance cloneSkill() {
        HPregeneration newSkill = new HPregeneration(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}