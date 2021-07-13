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

package de.Keyle.MyPet.compat.v1_17_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_17_R1.entity.ai.attack.MeleeAttack;
import net.minecraft.world.level.World;

@EntitySize(width = 4.F, height = 4.F)
public class EntityMyGhast extends EntityMyPet {

	public EntityMyGhast(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.ghast.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.ghast.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.ghast.ambient";
	}

	@Override
	public void setPathfinder() {
		super.setPathfinder();
		petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 5.5, 20));
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (Configuration.MyPet.Ghast.CAN_GLIDE) {
			if (!this.z && this.getMot().getY() < 0.0D) {
				this.setMot(getMot().d(1, 0.6D, 1));
			}
		}
	}

	/**
	 * -> disable falldamage
	 */
	@Override
	public int d(float f, float f1) {
		if (!Configuration.MyPet.Ghast.CAN_GLIDE) {
			super.e(f, f1);
		}
		return 0;
	}
}
