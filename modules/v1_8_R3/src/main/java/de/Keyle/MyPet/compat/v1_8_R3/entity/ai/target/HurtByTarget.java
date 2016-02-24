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

package de.Keyle.MyPet.compat.v1_8_R3.entity.ai.target;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTameableAnimal;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

public class HurtByTarget extends AIGoal {
    EntityMyPet petEntity;
    ActiveMyPet myPet;
    EntityLiving target = null;

    public HurtByTarget(EntityMyPet petEntity) {
        this.petEntity = petEntity;
        myPet = petEntity.getMyPet();
    }

    @Override
    public boolean shouldStart() {

        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (petEntity.getLastDamager() == null) {
            return false;
        }
        if (target != petEntity.getLastDamager()) {
            target = petEntity.getLastDamager();
        }
        if (target == petEntity) {
            return false;
        }
        if (target instanceof EntityArmorStand) {
            return false;
        }
        if (target instanceof EntityPlayer) {
            Player targetPlayer = (Player) target.getBukkitEntity();

            if (targetPlayer == myPet.getOwner().getPlayer()) {
                return false;
            } else if (!MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
                return false;
            }
        } else if (target instanceof EntityMyPet) {
            ActiveMyPet targetMyPet = ((EntityMyPet) target).getMyPet();
            if (!MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
                return false;
            }
        } else if (target instanceof EntityTameableAnimal) {
            EntityTameableAnimal tameable = (EntityTameableAnimal) target;
            if (tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                if (myPet.getOwner().equals(tameableOwner)) {
                    return false;
                }
            }
        }
        return MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), target.getBukkitEntity());
    }

    @Override
    public boolean shouldFinish() {
        if (!petEntity.canMove()) {
            return true;
        }
        if (!petEntity.hasTarget()) {
            return true;
        }

        EntityLiving target = ((CraftLivingEntity) this.petEntity.getTarget()).getHandle();

        if (target.world != petEntity.world) {
            return true;
        } else if (petEntity.h(target) > 400) {
            return true;
        } else if (petEntity.h(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        petEntity.setTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.GetHurt);
    }

    @Override
    public void finish() {
        petEntity.setGoalTarget(null, EntityTargetEvent.TargetReason.FORGOT_TARGET, false);
    }
}