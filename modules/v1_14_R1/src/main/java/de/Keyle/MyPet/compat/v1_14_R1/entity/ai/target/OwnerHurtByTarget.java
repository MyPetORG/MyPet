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

package de.Keyle.MyPet.compat.v1_14_R1.entity.ai.target;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_14_R1.entity.EntityMyPet;
import net.minecraft.server.v1_14_R1.EntityArmorStand;
import net.minecraft.server.v1_14_R1.EntityLiving;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EntityTameableAnimal;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Compat("v1_14_R1")
public class OwnerHurtByTarget implements AIGoal {

    private EntityMyPet petEntity;
    private EntityLiving lastDamager;
    private MyPet myPet;
    private EntityPlayer owner;

    public OwnerHurtByTarget(EntityMyPet entityMyPet) {
        this.petEntity = entityMyPet;
        myPet = entityMyPet.getMyPet();
        owner = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
    }

    @Override
    public boolean shouldStart() {
        if (!petEntity.canMove()) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        this.lastDamager = owner.getLastDamager();

        if (this.lastDamager == null || !lastDamager.isAlive()) {
            return false;
        }
        if (lastDamager instanceof EntityArmorStand) {
            return false;
        }
        if (lastDamager == petEntity) {
            return false;
        }
        if (lastDamager instanceof EntityPlayer) {
            if (owner == lastDamager) {
                return false;
            }

            Player targetPlayer = (Player) lastDamager.getBukkitEntity();

            if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
                return false;
            }
        } else if (lastDamager instanceof EntityMyPet) {
            MyPet targetMyPet = ((EntityMyPet) lastDamager).getMyPet();
            if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
                return false;
            }
        } else if (lastDamager instanceof EntityTameableAnimal) {
            EntityTameableAnimal tameable = (EntityTameableAnimal) lastDamager;
            if (tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                if (myPet.getOwner().equals(tameableOwner)) {
                    return false;
                }
            }
        }
        if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), lastDamager.getBukkitEntity())) {
            return false;
        }
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                if (lastDamager instanceof EntityTameableAnimal && ((EntityTameableAnimal) lastDamager).isTamed()) {
                    return false;
                }
                if (lastDamager instanceof EntityMyPet) {
                    return false;
                }
                if (lastDamager instanceof EntityPlayer) {
                    return false;
                }
            }
        }
        return true;
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
        petEntity.setTarget((LivingEntity) this.lastDamager.getBukkitEntity(), TargetPriority.OwnerGetsHurt);
    }

    @Override
    public void finish() {
        petEntity.forgetTarget();
    }
}