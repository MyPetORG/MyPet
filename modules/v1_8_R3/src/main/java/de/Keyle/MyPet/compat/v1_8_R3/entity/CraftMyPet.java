/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_8_R3.entity;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.skill.skills.Behavior;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class CraftMyPet extends CraftCreature implements MyPetBukkitEntity {
    protected MyPetPlayer petOwner;
    protected EntityMyPet petEntity;

    public CraftMyPet(CraftServer server, EntityMyPet entityMyPet) {
        super(server, entityMyPet);
        petEntity = entityMyPet;
    }

    @Deprecated
    public void _INVALID_damage(int amount) {
        damage((double) amount);
    }

    @Deprecated
    public void _INVALID_damage(int amount, Entity source) {
        damage((double) amount, source);
    }

    @Deprecated
    public int _INVALID_getHealth() {
        return (int) getHealth();
    }

    @Deprecated
    public int _INVALID_getLastDamage() {
        return (int) getLastDamage();
    }

    @Deprecated
    public int _INVALID_getMaxHealth() {
        return (int) getMaxHealth();
    }

    @Deprecated
    public void _INVALID_setHealth(int health) {
        setHealth((double) health);
    }

    @Deprecated
    public void _INVALID_setLastDamage(int damage) {
        setLastDamage((double) damage);
    }

    @Deprecated
    public void _INVALID_setMaxHealth(int health) {
    }

    public boolean canMove() {
        return petEntity.canMove();
    }

    @Override
    public EntityMyPet getHandle() {
        return petEntity;
    }

    public ActiveMyPet getMyPet() {
        return petEntity.getMyPet();
    }

    public MyPetPlayer getOwner() {
        if (petOwner == null) {
            petOwner = getMyPet().getOwner();
        }
        return petOwner;
    }

    @Override
    public void removeEntity() {
        getHandle().dead = true;
    }

    public MyPetType getPetType() {
        return getMyPet().getPetType();
    }

    @Override
    public EntityType getType() {
        return EntityType.UNKNOWN;
    }

    @Override
    public void remove() {
        // do nothing to prevent other plugins from removing the MyPet
        // user removeEntity() to remove the MyPet
    }

    @Override
    public void setHealth(double health) {
        if (health < 0) {
            health = 0;
        }
        if (health > getMaxHealth()) {
            health = getMaxHealth();
        }
        super.setHealth(health);
    }

    public void setTarget(LivingEntity target) {
        EntityMyPet entity = getHandle();
        if (target == null) {
            entity.setGoalTarget(null);
        } else if (target instanceof CraftLivingEntity) {
            if (!entity.isMyPet) {
                return;
            }
            if (entity.myPet.getSkills().isSkillActive(Behavior.class)) {
                Behavior behaviorSkill = getMyPet().getSkills().getSkill(Behavior.class);
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly) {
                    return;
                }
            }
            petEntity.setGoalTarget(((CraftLivingEntity) target).getHandle());
        }
    }

    public void setGoalTarget(LivingEntity target) {
        getHandle().goalTarget = ((CraftLivingEntity) target).getHandle();
    }

    @Override
    public String toString() {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",type=" + getPetType() + "}";
    }
}