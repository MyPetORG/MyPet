/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.target;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;

@Compat("v1_17_R1")
public class HurtByTarget implements AIGoal {

	EntityMyPet petEntity;
	MyPet myPet;
	net.minecraft.world.entity.LivingEntity target = null;

	public HurtByTarget(EntityMyPet petEntity) {
		this.petEntity = petEntity;
		myPet = petEntity.getMyPet();
	}

	@Override
	public boolean shouldStart() {

		if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
			return false;
		}
		if (petEntity.getLastHurtByMob() == null) {
			return false;
		}
		if (target != petEntity.getLastHurtByMob()) {
			target = petEntity.getLastHurtByMob();
		}
		if (target == petEntity) {
			return false;
		}
		if (target instanceof ArmorStand) {
			return false;
		}
		if (target instanceof ServerPlayer) {
			Player targetPlayer = (Player) target.getBukkitEntity();

			if (targetPlayer == myPet.getOwner().getPlayer()) {
				return false;
			} else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
				return false;
			}
		} else if (target instanceof EntityMyPet) {
			MyPet targetMyPet = ((EntityMyPet) target).getMyPet();
			if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
				return false;
			}
		} else if (target instanceof TamableAnimal) {
			Method getOwnerReflect = ReflectionUtil.getMethod(TamableAnimal.class, "getOwner"); //Method: getOwner -> mojang mapping maps that to fx() even tho it still is getOwner.
			TamableAnimal tameable = (TamableAnimal) entityLiving;
			try {
				if (tameable.isTame() && getOwnerReflect.invoke(tameable, null) != null) {
					Player tameableOwner = (Player) ((net.minecraft.world.entity.player.Player) getOwnerReflect.invoke(tameable, null)).getBukkitEntity();
					if (myPet.getOwner().equals(tameableOwner)) {
						return false;
					}
				}
			} catch(IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
			}
		}
		return MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), target.getBukkitEntity());
	}

	@Override
	public boolean shouldFinish() {
		if (!petEntity.canMove()) {
			return true;
		}
		if (!petEntity.hasTarget()) {
			return true;
		}

		net.minecraft.world.entity.LivingEntity target = ((CraftLivingEntity) this.petEntity.getMyPetTarget()).getHandle();

		if (target.level != petEntity.level) {
			return true;
		} else if (petEntity.distanceToSqr(target) > 400) {
			return true;
		} else return petEntity.distanceToSqr(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600;
	}

	@Override
	public void start() {
		petEntity.setMyPetTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.GetHurt);
	}

	@Override
	public void finish() {
		petEntity.forgetTarget();
	}
}
