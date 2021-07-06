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

package de.Keyle.MyPet.compat.v1_17_R1.entity.ai.movement;

import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_17_R1.entity.types.EntityMyCat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.types.EntityMyFox;
import de.Keyle.MyPet.compat.v1_17_R1.entity.types.EntityMyPanda;
import de.Keyle.MyPet.compat.v1_17_R1.entity.types.EntityMyWolf;

@Compat("v1_17_R1")
public class Sit implements AIGoal {

	private final EntityMyPet entityMyPet;
	private boolean sitting = false;

	public Sit(EntityMyPet entityMyPet) {
		this.entityMyPet = entityMyPet;
	}

	@Override
	public boolean shouldStart() {
		if (!(this.entityMyPet instanceof EntityMyWolf) &&
				!(this.entityMyPet instanceof EntityMyCat) &&
				!(this.entityMyPet instanceof EntityMyPanda) &&
				!(this.entityMyPet instanceof EntityMyFox)) {
			return false;
		} else if (this.entityMyPet.isInWater()) {
			return false;
		} else if (!this.entityMyPet.isOnGround()) {
			return false;
		}
		return this.sitting;
	}

	@Override
	public void start() {
		this.entityMyPet.getPetNavigation().stop();
		if (this.entityMyPet instanceof EntityMyWolf) {
			((EntityMyWolf) this.entityMyPet).applySitting(true);
		} else if (this.entityMyPet instanceof EntityMyCat) {
			((EntityMyCat) this.entityMyPet).applySitting(true);
		} else if (this.entityMyPet instanceof EntityMyFox) {
			((EntityMyFox) this.entityMyPet).updateActionsWatcher(1, true);
		} else if (this.entityMyPet instanceof EntityMyPanda) {
			((EntityMyPanda) this.entityMyPet).updateActionsWatcher(8, true);
		}
		entityMyPet.setGoalTarget(null);
	}

	@Override
	public void finish() {
		if (this.entityMyPet instanceof EntityMyWolf) {
			((EntityMyWolf) this.entityMyPet).applySitting(false);
		} else if (this.entityMyPet instanceof EntityMyCat) {
			((EntityMyCat) this.entityMyPet).applySitting(false);
		} else if (this.entityMyPet instanceof EntityMyFox) {
			((EntityMyFox) this.entityMyPet).updateActionsWatcher(1, false);
		} else if (this.entityMyPet instanceof EntityMyPanda) {
			((EntityMyPanda) this.entityMyPet).updateActionsWatcher(8, false);
		}
	}

	public void setSitting(boolean sitting) {
		this.sitting = sitting;
	}

	public boolean isSitting() {
		return this.sitting;
	}

	public void toggleSitting() {
		this.sitting = !this.sitting;
	}
}
