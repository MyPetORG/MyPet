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
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.ai.navigation.Navigation;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;

@Compat("v1_17_R1")
public class Float implements AIGoal {

	private final EntityMyPet entityMyPet;
	private final EntityPlayer owner;

	private int lavaCounter = 10;
	private boolean inLava = false;

	public Float(EntityMyPet entityMyPet) {
		this.entityMyPet = entityMyPet;
		//entityMyPet.getNavigation().e(true);
		this.owner = ((CraftPlayer) entityMyPet.getOwner().getPlayer()).getHandle();
		((Navigation) entityMyPet.getNavigation()).c(true);
	}

	@Override
	public boolean shouldStart() {
		return entityMyPet.isInWater();
	}

	@Override
	public void finish() {
		inLava = false;
	}

	@Override
	public void tick() {
		entityMyPet.setMot(entityMyPet.getMot().add(0, 0.05D, 0));

		if (inLava && lavaCounter-- <= 0) {
			if (entityMyPet.getPetNavigation().navigateTo(owner.getBukkitEntity())) {
				lavaCounter = 10;
			}
		}
		if (!inLava && entityMyPet.a(TagsFluid.c)) {
			inLava = true;
		}
	}
}
