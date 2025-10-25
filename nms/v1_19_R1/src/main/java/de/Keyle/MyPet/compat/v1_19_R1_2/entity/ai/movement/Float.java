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

package de.Keyle.MyPet.compat.v1_19_R1_2.entity.ai.movement;

import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;

import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R1_2.entity.EntityMyPet;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;

@Compat("v1_19_R1_2")
public class Float implements AIGoal {

	private final EntityMyPet entityMyPet;
	private final Player owner;

	private int lavaCounter = 10;
	private boolean inLava = false;

	public Float(EntityMyPet entityMyPet) {
		this.entityMyPet = entityMyPet;
		//entityMyPet.getNavigation().e(true);
		this.owner = ((CraftPlayer) entityMyPet.getOwner().getPlayer()).getHandle();
		((PathNavigation) entityMyPet.getNavigation()).setCanFloat(true);;
	}

	@Override
	public boolean shouldStart() {
		if(entityMyPet.floatsInLava()) { //Some entities do that
			return entityMyPet.isInWater() || entityMyPet.isInLava();
		}
		return entityMyPet.isInWater();
	}

	@Override
	public void finish() {
		inLava = false;
	}

	@Override
	public void tick() {
		if(entityMyPet.specialFloat()) return;	//Check if the entity has some special floating-behaviour for the liquid it is in right now
		
		entityMyPet.setDeltaMovement(entityMyPet.getDeltaMovement().add(0, 0.05D, 0));

		if (inLava && lavaCounter-- <= 0) {
			if (entityMyPet.getPetNavigation().navigateTo(owner.getBukkitEntity())) {
				lavaCounter = 10;
			}
		}
		if (!inLava && entityMyPet.isEyeInFluid(FluidTags.LAVA)) {
			inLava = true;
		}
	}
}
