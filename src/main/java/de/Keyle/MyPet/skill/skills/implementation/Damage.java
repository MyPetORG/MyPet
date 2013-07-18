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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.info.DamageInfo;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.ChatColor;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

public class Damage extends DamageInfo implements ISkillInstance
{
    private MyPet myPet;

    public Damage(boolean addedByInheritance)
    {
        super(addedByInheritance);
    }

    public void setMyPet(MyPet myPet)
    {
        this.myPet = myPet;
    }

    public MyPet getMyPet()
    {
        return myPet;
    }

    public boolean isActive()
    {
        return damage > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof DamageInfo)
        {
            boolean isPassive = damage <= 0;
            if (upgrade.getProperties().getValue().containsKey("damage"))
            {
                int damage = ((IntTag) upgrade.getProperties().getValue().get("damage")).getValue();
                upgrade.getProperties().getValue().remove("damage");
                DoubleTag doubleTag = new DoubleTag("damage_double", damage);
                upgrade.getProperties().getValue().put("damage_double", doubleTag);
            }
            if (upgrade.getProperties().getValue().containsKey("damage_double"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_damage") || ((StringTag) upgrade.getProperties().getValue().get("addset_damage")).getValue().equals("add"))
                {
                    damage += ((DoubleTag) upgrade.getProperties().getValue().get("damage_double")).getValue();
                }
                else
                {
                    damage = ((DoubleTag) upgrade.getProperties().getValue().get("damage_double")).getValue();
                }
                if (!quiet)
                {
                    myPet.sendMessageToOwner(MyPetUtil.formatText(MyPetLocales.getString("Message.Skill.Damage.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), damage));
                }
            }
            if (isPassive != (damage <= 0))
            {
                if (myPet.getStatus() == PetState.Here)
                {
                    getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
                    getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
                    getMyPet().getCraftPet().getHandle().setPathfinder();
                    if (damage == 0)
                    {
                        getMyPet().getCraftPet().getHandle().setGoalTarget(null);
                    }
                }
            }
        }
    }

    public String getFormattedValue()
    {
        return " -> " + ChatColor.GOLD + damage + ChatColor.RESET + " " + MyPetLocales.getString("Name.Damage", myPet.getOwner());
    }

    public void reset()
    {
        damage = 0;
        if (myPet.getStatus() == PetState.Here)
        {
            getMyPet().getCraftPet().getHandle().petPathfinderSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().petTargetSelector.clearGoals();
            getMyPet().getCraftPet().getHandle().setPathfinder();
            getMyPet().getCraftPet().getHandle().setGoalTarget(null);
        }
    }

    public double getDamage()
    {
        return damage;
    }

    public ISkillInstance cloneSkill()
    {
        Damage newSkill = new Damage(isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}