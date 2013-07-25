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
import de.Keyle.MyPet.skill.ISkillActive;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skills.info.KnockbackInfo;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import net.minecraft.server.v1_6_R2.MathHelper;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.spout.nbt.IntTag;
import org.spout.nbt.StringTag;

import java.util.Random;

public class Knockback extends KnockbackInfo implements ISkillInstance, ISkillActive
{
    private static Random random = new Random();
    private MyPet myPet;

    public Knockback(boolean addedByInheritance)
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
        return chance > 0;
    }

    public void upgrade(ISkillInfo upgrade, boolean quiet)
    {
        if (upgrade instanceof KnockbackInfo)
        {
            if (upgrade.getProperties().getValue().containsKey("chance"))
            {
                if (!upgrade.getProperties().getValue().containsKey("addset_chance") || ((StringTag) upgrade.getProperties().getValue().get("addset_chance")).getValue().equals("add"))
                {
                    chance += ((IntTag) upgrade.getProperties().getValue().get("chance")).getValue();
                }
                else
                {
                    chance = ((IntTag) upgrade.getProperties().getValue().get("chance")).getValue();
                }
                chance = Math.min(chance, 100);
                if (!quiet)
                {
                    myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skill.Knockback.Upgrade", myPet.getOwner().getLanguage()), myPet.getPetName(), chance));
                }
            }
        }
    }

    public String getFormattedValue()
    {
        return chance + "%";
    }

    public void reset()
    {
        chance = 0;
    }

    public boolean activate()
    {
        return random.nextDouble() < chance / 100.;
    }

    public void knockbackTarget(LivingEntity target)
    {
        ((CraftEntity) target).getHandle().g(-MathHelper.sin(myPet.getLocation().getYaw() * 3.141593F / 180.0F) * 2 * 0.5F, 0.1D, MathHelper.cos(myPet.getLocation().getYaw() * 3.141593F / 180.0F) * 2 * 0.5F);
    }

    @Override
    public ISkillInstance cloneSkill()
    {
        Knockback newSkill = new Knockback(this.isAddedByInheritance());
        newSkill.setProperties(getProperties());
        return newSkill;
    }
}