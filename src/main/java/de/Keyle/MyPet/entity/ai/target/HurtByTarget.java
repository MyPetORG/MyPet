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

package de.Keyle.MyPet.entity.ai.target;

import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.PvPChecker;
import net.minecraft.server.v1_6_R3.EntityLiving;
import net.minecraft.server.v1_6_R3.EntityPlayer;
import net.minecraft.server.v1_6_R3.EntityTameableAnimal;
import org.bukkit.entity.Player;

public class HurtByTarget extends AIGoal {
    EntityMyPet petEntity;
    MyPet myPet;
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
        if (target instanceof EntityPlayer) {
            Player targetPlayer = (Player) target.getBukkitEntity();

            if (targetPlayer == myPet.getOwner().getPlayer()) {
                return false;
            } else if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetPlayer)) {
                return false;
            }
        } else if (target instanceof EntityMyPet) {
            MyPet targetMyPet = ((EntityMyPet) target).getMyPet();
            if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer())) {
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
        if (!PvPChecker.canHurtCitizens(target.getBukkitEntity())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldFinish() {
        EntityLiving entityliving = petEntity.getGoalTarget();

        if (!petEntity.canMove()) {
            return true;
        } else if (entityliving == null) {
            return true;
        } else if (!entityliving.isAlive()) {
            return true;
        } else if (petEntity.getGoalTarget().world != petEntity.world) {
            return true;
        } else if (petEntity.e(petEntity.getGoalTarget()) > 400) {
            return true;
        } else if (petEntity.e(petEntity.getOwner().getEntityPlayer()) > 600) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        petEntity.setGoalTarget(this.target);
    }

    @Override
    public void finish() {
        petEntity.setGoalTarget(null);
    }
}