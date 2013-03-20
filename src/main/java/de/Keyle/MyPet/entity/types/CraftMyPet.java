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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_5_R1.EntityCreature;
import org.bukkit.craftbukkit.v1_5_R1.CraftServer;
import org.bukkit.craftbukkit.v1_5_R1.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_5_R1.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class CraftMyPet extends CraftCreature
{
    protected MyPetPlayer petOwner;
    protected EntityMyPet petEntity;

    public CraftMyPet(CraftServer server, EntityMyPet entityMyPet)
    {
        super(server, entityMyPet);
        petEntity = entityMyPet;
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
                Behavior behaviorSkill = (Behavior) getMyPet().getSkills().getSkill("Behavior");
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return;
                }
            }
            petEntity.setTarget(((CraftLivingEntity) target).getHandle());
        }
    }

    @Override
    public void remove()
    {
        if (getMyPet().getStatus() != PetState.Despawned)
        {
            getMyPet().removePet();
            getMyPet().sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_Despawn").replace("%petname%", getMyPet().petName)));
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
        petEntity.setHealth(health);
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
        return petEntity.getMyPet();
    }

    @Override
    public EntityMyPet getHandle()
    {
        return petEntity;
    }

    public boolean canMove()
    {
        return petEntity.canMove();
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