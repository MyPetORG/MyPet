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

package de.Keyle.MyPet.compat.v1_20_R4.entity.ai.target;

import org.bukkit.craftbukkit.v1_20_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_20_R4.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

@Compat("v1_20_R4")
public class BehaviorDuelTarget implements AIGoal {

	private final MyPet myPet;
	private final EntityMyPet petEntity;
	private final ServerPlayer petOwnerEntity;
	private MyPetMinecraftEntity target;
	private MyPetMinecraftEntity duelOpponent = null;
	private final float range;

	public BehaviorDuelTarget(EntityMyPet petEntity, float range) {
		this.petEntity = petEntity;
		this.petOwnerEntity = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
		this.myPet = petEntity.getMyPet();
		this.range = range;
	}

	@Override
	public boolean shouldStart() {
		Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
		if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Duel) {
			return false;
		}
		if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
			return false;
		}
		if (!petEntity.canMove()) {
			return false;
		}
		if (petEntity.hasTarget()) {
			return false;
		}
		if (duelOpponent != null) {
			this.target = duelOpponent;
			return true;
		}

		for (EntityMyPet entityMyPet : this.petEntity.level().getEntitiesOfClass(EntityMyPet.class, this.petOwnerEntity.getBoundingBox().inflate(range, range, range))) {
			MyPet targetMyPet = entityMyPet.getMyPet();

			if (entityMyPet != petEntity && entityMyPet.isAlive()) {
				if (!targetMyPet.getSkills().isActive(BehaviorImpl.class) || (targetMyPet.getEntity().isPresent() && !targetMyPet.getEntity().get().canMove())) {
					continue;
				}
				BehaviorImpl targetbehavior = targetMyPet.getSkills().get(BehaviorImpl.class);
				if (targetbehavior.getBehavior() != BehaviorMode.Duel) {
					continue;
				}
				if (targetMyPet.getDamage() == 0) {
					continue;
				}
				this.target = entityMyPet;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldFinish() {
		if (!petEntity.canMove()) {
			return true;
		} else if (!petEntity.hasTarget()) {
			return true;
		}

		LivingEntity target = ((CraftLivingEntity) this.petEntity.getMyPetTarget()).getHandle();

		Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
		if (behaviorSkill.getBehavior() != BehaviorMode.Duel) {
			return true;
		} else if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
			return true;
		} else if (target.level() != petEntity.level()) {
			return true;
		} else if (petEntity.distanceToSqr(target) > 400) {
			return true;
		} else return petEntity.distanceToSqr(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600;
	}

	@Override
	public void start() {
		petEntity.setMyPetTarget(this.target.getBukkitEntity(), TargetPriority.Duel);
		setDuelOpponent(this.target);
		if (target.getTargetSelector().hasGoal("DuelTarget")) {
			BehaviorDuelTarget duelGoal = (BehaviorDuelTarget) target.getTargetSelector().getGoal("DuelTarget");
			duelGoal.setDuelOpponent(this.petEntity);
		}
	}

	@Override
	public void finish() {
		petEntity.forgetTarget();
		duelOpponent = null;
		target = null;
	}

	public MyPetMinecraftEntity getDuelOpponent() {
		return duelOpponent;
	}

	public void setDuelOpponent(MyPetMinecraftEntity opponent) {
		this.duelOpponent = opponent;
	}
}
