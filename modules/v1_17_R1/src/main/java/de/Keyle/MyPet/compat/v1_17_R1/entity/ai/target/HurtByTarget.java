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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Compat("v1_17_R1")
public class HurtByTarget implements AIGoal {

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
		if (target instanceof EntityArmorStand) {
			return false;
		}
		if (target instanceof EntityPlayer) {
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
		} else if (target instanceof EntityTameableAnimal) {
			EntityTameableAnimal tameable = (EntityTameableAnimal) target;
			if (tameable.isTamed() && tameable.getOwner() != null) {
				Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
				if (myPet.getOwner().equals(tameableOwner)) {
					return false;
				}
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

		EntityLiving target = ((CraftLivingEntity) this.petEntity.getTarget()).getHandle();

		if (target.t != petEntity.t) {
			return true;
		} else if (petEntity.f(target) > 400) {
			return true;
		} else return petEntity.f(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600;
	}

	@Override
	public void start() {
		petEntity.setTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.GetHurt);
	}

	@Override
	public void finish() {
		petEntity.forgetTarget();
	}
}
