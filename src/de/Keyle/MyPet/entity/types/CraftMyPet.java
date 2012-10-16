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
import net.minecraft.server.EntityCreature;
import net.minecraft.server.PathEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftTameableAnimal;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public abstract class CraftMyPet extends CraftTameableAnimal
{
    protected AnimalTamer petOwner;

    public CraftMyPet(CraftServer server, EntityMyPet petEntity)
    {
        super(server, petEntity);
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
            if (!getHandle().isMyPet || !getHandle().myPet.getSkillSystem().hasSkill("Behavior"))
            {
                return;
            }
            Behavior behaviorSkill = (Behavior) getHandle().myPet.getSkillSystem().getSkill("Behavior");
            if (behaviorSkill.getLevel() <= 0 || behaviorSkill.getBehavior() != Behavior.BehaviorState.Friendly)
            {
                return;
            }
            entity.target = ((CraftLivingEntity) target).getHandle();
            entity.pathEntity = entity.world.findPath(entity, entity.target, 16.0F, true, false, false, true);
        }
    }

    public boolean isSitting()
    {
        return getHandle().isSitting();
    }

    public void setSitting(boolean sitting)
    {
        getHandle().setSitting(sitting);
    }

    public boolean isTamed()
    {
        return getHandle().isTamed();
    }

    public void setTamed(boolean tame)
    {
        getHandle().setTamed(tame);
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

    public void setOwner(AnimalTamer tamer)
    {
        petOwner = tamer;
        if (petOwner != null)
        {
            setTamed(true);
            setPath(null);
            if (petOwner instanceof Player)
            {
                setOwnerName(((Player) petOwner).getName());
            }
            else
            {
                setOwnerName("");
            }
        }
        else
        {
            setTamed(false);
            setOwnerName("");
        }
    }

    public String getOwnerName()
    {
        return getHandle().getOwnerName();
    }

    public void setOwnerName(String ownerName)
    {
        getHandle().setOwnerName(ownerName);
    }

    private void setPath(PathEntity pathentity)
    {
        getHandle().setPathEntity(pathentity);
    }

    @Override
    public EntityMyPet getHandle()
    {
        return (EntityMyPet) entity;
    }

    @Override
    public String toString()
    {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",sitting=" + isSitting() + "}";
    }

    public abstract EntityType getType();
}