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

package de.Keyle.MyPet.compat.v1_21_R4.entity.ai.movement;

import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R4.entity.EntityMyPet;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;

@Compat("v1_21_R4")
public class MyPetRandomFly extends MyPetRandomStroll {

	protected EntityMyPet petEntity;

	public MyPetRandomFly(EntityMyPet petEntity, int startDistance) {
		super(petEntity, startDistance);
		this.petEntity = petEntity;
	}

	@Override
	protected Vec3 getPosition() {
		Vec3 vec3d = this.petEntity.getViewVector(0.0F);
		boolean flag = true;
		Vec3 vec3d1 = HoverRandomPos.getPos(this.petEntity, 8, 7, vec3d.x, vec3d.z, 1.5707964F, 3, 1);

		return vec3d1 != null ? vec3d1 : AirAndWaterRandomPos.getPos(this.petEntity, 8, 4, -2, vec3d.x, vec3d.z, 1.5707963705062866D);
	}

	@Override
	protected void applySpeed() {
		double walkSpeed = owner.getAbilities().walkingSpeed+0.4f;
		nav.getParameters().addSpeedModifier("RandomStroll", walkSpeed);
	}
}
