/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.entity.types.wolf;

import de.Keyle.MyWolf.skill.skills.Behavior;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.PathEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftTameableAnimal;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CraftMyWolf extends CraftTameableAnimal
{
    private AnimalTamer owner;

    public CraftMyWolf(CraftServer server, EntityMyWolf wolf)
    {
        super(server, wolf);
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
            if (!getHandle().isMyWolf || !getHandle().MWolf.SkillSystem.hasSkill("Behavior"))
            {
                return;
            }
            Behavior behavior = (Behavior) getHandle().MWolf.SkillSystem.getSkill("Behavior");
            if (behavior.getLevel() <= 0 || behavior.getBehavior() != Behavior.BehaviorState.Friendly)
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
        setPath(null);
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
        if (owner == null && !("").equals(getOwnerName()))
        {
            owner = getServer().getPlayer(getOwnerName());

            if (owner == null)
            {
                owner = getServer().getOfflinePlayer(getOwnerName());
            }
        }
        return owner;
    }

    public void setOwner(AnimalTamer tamer)
    {
        owner = tamer;
        if (owner != null)
        {
            setTamed(true);
            setPath(null);
            if (owner instanceof Player)
            {
                setOwnerName(((Player) owner).getName());
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
    public EntityMyWolf getHandle()
    {
        return (EntityMyWolf) entity;
    }

    @Override
    public String toString()
    {
        return "CraftMyWolf{MyWolf=" + getHandle().isMyWolf() + ",owner=" + getOwner() + ",tame=" + isTamed() + ",sitting=" + isSitting() + "}";
    }

    public EntityType getType()
    {
        return EntityType.WOLF;
    }
}