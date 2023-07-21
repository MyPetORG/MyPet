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

package de.Keyle.MyPet.compat.v1_19_R3.entity.ai.navigation;

import org.bukkit.craftbukkit.v1_19_R3.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.navigation.NavigationParameters;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R3.entity.EntityMyAquaticPet;
import de.Keyle.MyPet.compat.v1_19_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R3.entity.ai.movement.MyPetAquaticMoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

@Compat("v1_19_R3")
public class VanillaNavigation extends AbstractNavigation {

	PathNavigation nav;

	public VanillaNavigation(EntityMyPet entityMyPet) {
		super(entityMyPet);
		nav = (PathNavigation) entityMyPet.getNavigation();
	}

	public VanillaNavigation(EntityMyPet entityMyPet, NavigationParameters parameters) {
		super(entityMyPet, parameters);
		nav = (PathNavigation) entityMyPet.getNavigation();
	}

	@Override
	public void stop() {
		nav.stop();
	}

	@Override
	public boolean navigateTo(double x, double y, double z) {
		if (this.nav.moveTo(x, y, z, 1.D)) {
			applyNavigationParameters();
			return true;
		}
		return false;
	}

	@Override
	public boolean navigateTo(LivingEntity entity) {
		return navigateTo(((CraftLivingEntity) entity).getHandle());
	}

	public boolean navigateTo(net.minecraft.world.entity.LivingEntity entity) {
		if (this.nav.moveTo(entity, 1.D)) {
			applyNavigationParameters();
			return true;
		}
		return false;
	}

	@Override
	public void tick() {
		//This switches between movesets enabling the pet to move naturally on land and water
		EntityMyPet petEntity = (EntityMyPet) this.entityMyPet;
		if(petEntity.isInWaterOrBubble() && this.entityMyPet instanceof EntityMyAquaticPet
				&& !(petEntity.getMoveControl() instanceof MyPetAquaticMoveControl)) {
			petEntity.switchMovement(new MyPetAquaticMoveControl(petEntity));
		} else if(!petEntity.isInWaterOrBubble() && petEntity.getMoveControl() instanceof MyPetAquaticMoveControl) {
			petEntity.switchMovement(new MoveControl(petEntity));
		}
		
		nav.tick();
	}

	@Override
	public void applyNavigationParameters() {
		this.nav.setCanFloat(parameters.avoidWater());
		((EntityMyPet) this.entityMyPet)
				.getAttribute(Attributes.MOVEMENT_SPEED)
				.setBaseValue(parameters.speed() + parameters.speedModifier());
	}
}
