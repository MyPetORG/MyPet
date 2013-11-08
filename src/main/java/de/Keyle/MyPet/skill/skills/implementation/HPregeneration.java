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

package de.Keyle.MyPet.skill.skills.implementation;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.entity.types.CraftMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.info.HPregenerationInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import net.minecraft.server.v1_6_R3.EntityLiving;
import net.minecraft.server.v1_6_R3.PotionBrewer;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

public class HPregeneration extends HPregenerationInfo implements ISkillInstance, IScheduler {
    private int timeCounter = 0;
    private MyPet myPet;

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

    public void upgrade(ISkillInfo upgrade, boolean quiet) {
        if (upgrade instanceof HPregenerationInfo) {
            boolean valuesEdit = false;
            if (upgrade.getProperties().getValue().containsKey("hp")) {
                int hp = ((IntTag) upgrade.getProperties().getValue().get("hp")).getValue();
                upgrade.getProperties().getValue().remove("hp");
                DoubleTag doubleTag = new DoubleTag("hp_double", hp);
                upgrade.getProperties().getValue().put("hp_double", doubleTag);
            }
            if (upgrade.getProperties().getValue().containsKey("hp_double")) {
                if (!upgrade.getProperties().getValue().containsKey("addset_hp") || ((StringTag) upgrade.getProperties().getValue().get("addset_hp")).getValue().equals("add")) {
                    increaseHpBy += ((DoubleTag) upgrade.getProperties().getValue().get("hp_double")).getValue();
                } else {
                    increaseHpBy = ((DoubleTag) upgrade.getProperties().getValue().get("hp_double")).getValue();
                }
                valuesEdit = true;
            }
            if (upgrade.getProperties().getValue().containsKey("time")) {
                if (!upgrade.getProperties().getValue().containsKey("addset_time") || ((StringTag) upgrade.getProperties().getValue().get("addset_time")).getValue().equals("add")) {
                    regenTime -= ((IntTag) upgrade.getProperties().getValue().get("time")).getValue();
                } else {
                    regenTime = ((IntTag) upgrade.getProperties().getValue().get("time")).getValue();
                }
                if (regenTime < 1) {
                    regenTime = 1;
                }
                timeCounter = regenTime;
                valuesEdit = true;
            }
            if (!quiet && valuesEdit) {
                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.HpRegeneration.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), increaseHpBy, regenTime));
            }
        }
    }

    public String getFormattedValue() {
        return "+" + increaseHpBy + Locales.getString("Name.HP", myPet.getOwner().getLanguage()) + " ->" + regenTime + "sec";
    }

    public void reset() {
        regenTime = 0;
        increaseHpBy = 0;
        timeCounter = 0;
    }

    public void schedule() {
        if (increaseHpBy > 0 && myPet.getStatus() == PetState.Here) {
            if (timeCounter-- <= 0) {
                if (myPet.getHealth() < myPet.getMaxHealth()) {
                    addPotionGraphicalEffect(myPet.getCraftPet(), 0x00FF00, 40); //Green Potion Effect
                    myPet.getCraftPet().getHandle().heal((float) increaseHpBy, EntityRegainHealthEvent.RegainReason.REGEN);
                }
                timeCounter = regenTime;
            }
        }
    }

    @Override
    public ISkillInstance cloneSkill() {
        HPregeneration newSkill = new HPregeneration(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }

    public void addPotionGraphicalEffect(CraftMyPet entity, int color, int duration) {
        final EntityLiving entityLiving = entity.getHandle();
        entityLiving.getDataWatcher().watch(7, new Integer(color));

        Bukkit.getScheduler().scheduleSyncDelayedTask(MyPetPlugin.getPlugin(), new Runnable() {
            public void run() {
                int potionEffects = 0;
                if (!entityLiving.effects.isEmpty()) {
                    potionEffects = PotionBrewer.a(entityLiving.effects.values());
                }
                entityLiving.getDataWatcher().watch(7, new Integer(potionEffects));
            }
        }, duration);
    }
}