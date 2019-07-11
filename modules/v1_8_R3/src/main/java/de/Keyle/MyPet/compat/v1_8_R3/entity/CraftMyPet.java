/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_8_R3.entity;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.DoNotUse;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftCreature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

@Compat("v1_8_R3")
public class CraftMyPet extends CraftCreature implements MyPetBukkitEntity {
    protected MyPetPlayer petOwner;
    protected EntityMyPet petEntity;

    public CraftMyPet(CraftServer server, EntityMyPet entityMyPet) {
        super(server, entityMyPet);
        petEntity = entityMyPet;
    }

    @DoNotUse
    public void _INVALID_damage(int amount) {
        damage((double) amount);
    }

    @DoNotUse
    public void _INVALID_damage(int amount, Entity source) {
        damage((double) amount, source);
    }

    @DoNotUse
    public int _INVALID_getHealth() {
        return (int) getHealth();
    }

    @DoNotUse
    public int _INVALID_getLastDamage() {
        return (int) getLastDamage();
    }

    @DoNotUse
    public int _INVALID_getMaxHealth() {
        return (int) getMaxHealth();
    }

    @DoNotUse
    public void _INVALID_setHealth(int health) {
        setHealth((double) health);
    }

    @DoNotUse
    public void _INVALID_setLastDamage(int damage) {
        setLastDamage((double) damage);
    }

    @DoNotUse
    public void _INVALID_setMaxHealth(int health) {
    }

    public boolean canMove() {
        return petEntity.canMove();
    }

    @Override
    public void setSitting(boolean sitting) {
        getHandle().setSitting(sitting);
    }

    @Override
    public boolean isSitting() {
        return getHandle().isSitting();
    }

    @Override
    public EntityMyPet getHandle() {
        return petEntity;
    }

    public MyPet getMyPet() {
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
        setTarget(target, TargetPriority.Bukkit);
    }

    @Override
    public void forgetTarget() {
        getHandle().forgetTarget();
    }

    public void setTarget(LivingEntity target, TargetPriority priority) {
        getHandle().setTarget(target, priority);
    }

    @Override
    public String toString() {
        return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",type=" + getPetType() + "}";
    }
}