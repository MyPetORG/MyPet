/*
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_R1.EntityCreature;
import org.bukkit.craftbukkit.v1_4_R1.CraftServer;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class CraftMyPet extends CraftCreature
{
    protected MyPetPlayer petOwner;

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
            if (getHandle().myPet.getSkills().isSkillActive("Behavior"))
            {
                Behavior behaviorSkill = (Behavior) getHandle().myPet.getSkills().getSkill("Behavior");
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return;
                }
            }
            entity.setTarget(((CraftLivingEntity) target).getHandle());
        }
    }

    @Override
    public void remove()
    {
        if (getMyPet().status != PetState.Despawned)
        {
            getMyPet().removePet();
            getMyPet().sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Despawn").replace("%petname%", getMyPet().petName)));
        }
        else
        {
            super.remove();
        }
    }

    @Override
    public void setHealth(int health)
    {
        if (health < 0)
        {
            health = 0;
        }
        if (health > getMaxHealth())
        {
            health = getMaxHealth();
        }
        getHandle().setHealth(health);
    }

    public MyPetPlayer getOwner()
    {
        if (petOwner == null)
        {
            petOwner = getHandle().myPet.getOwner();
        }
        return petOwner;
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

    public MyPetType getPetType()
    {
        return getMyPet().getPetType();
    }

    @Override
    public EntityType getType()
    {
        return EntityType.UNKNOWN;
    }

    @Override
    public String toString()
    {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",type=" + getPetType() + "}";
    }
}