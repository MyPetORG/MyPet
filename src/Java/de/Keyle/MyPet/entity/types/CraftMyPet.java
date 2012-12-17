/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.skill.skills.Behavior;
import net.minecraft.server.v1_4_5.EntityCreature;
import org.bukkit.craftbukkit.v1_4_5.CraftServer;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftLivingEntity;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public abstract class CraftMyPet extends CraftCreature
{
    protected AnimalTamer petOwner;

    public CraftMyPet(CraftServer server, EntityMyPet entityMyPet)
    {
        super(server, entityMyPet);
    }

    public void setTarget(LivingEntity target)
    {
        EntityCreature entity = getHandle();
        if (target == null)
        {
            entity.target = null;
        }
        else if (target instanceof CraftLivingEntity)
        {
            if (!getHandle().isMyPet)
            {
                return;
            }
            if (getHandle().myPet.getSkillSystem().hasSkill("Behavior"))
            {
                Behavior behaviorSkill = (Behavior) getHandle().myPet.getSkillSystem().getSkill("Behavior");
                if (behaviorSkill.getLevel() > 0 && behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return;
                }
            }
            entity.setTarget(((CraftLivingEntity) target).getHandle());
        }
    }

    public AnimalTamer getOwner()
    {
        if (petOwner == null && !("").equals(getOwnerName()))
        {
            petOwner = getServer().getPlayer(getOwnerName());

            if (petOwner == null)
            {
                petOwner = getServer().getOfflinePlayer(getOwnerName());
            }
        }
        return petOwner;
    }

    public String getOwnerName()
    {
        return getHandle().myPet.getOwner().getName();
    }

    public MyPet getMyPet()
    {
        return getHandle().getMyPet();
    }

    @Override
    public EntityMyPet getHandle()
    {
        return (EntityMyPet) entity;
    }

    public boolean canMove()
    {
        return getHandle().canMove();
    }

    @Override
    public EntityType getType()
    {
        return EntityType.UNKNOWN;
    }

    @Override
    public String toString()
    {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + "}";
    }
}