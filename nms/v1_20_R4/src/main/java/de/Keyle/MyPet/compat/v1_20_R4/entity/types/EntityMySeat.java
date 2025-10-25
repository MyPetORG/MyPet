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

package de.Keyle.MyPet.compat.v1_20_R4.entity.types;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftLivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class EntityMySeat extends ArmorStand {
	public EntityMySeat(Level level, double x, double y, double z) {
		super(level, x, y, z);
		setSilent(true);
		setNoGravity(true);
		setSmall(true);
		setMarker(true);
		((CraftLivingEntity) getBukkitEntity()).setInvisible(true);
		setInvulnerable(true);
		persist = false;
	}

	public static void mountToPet(Entity passenger, Entity myPet) {
		var seat = new EntityMySeat(myPet.level(), myPet.getX(), myPet.getY(), myPet.getZ());
		if (myPet.level().addFreshEntity(seat, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
			if (seat.startRiding(myPet)) {
				if (passenger.startRiding(seat)) {
					return;
				}
				seat.stopRiding();
			}
			seat.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (getPassengers().isEmpty()) { //Done riding? Begone seat
			stopRiding();
			discard();
		} else if (this.getVehicle() == null) { //Got blown off the thing or something
			ejectPassengers();
			discard();
		}
	}

	//This should never matter but... hey - futureproofing
	@Override
	public boolean dismountsUnderwater() {
		if (getVehicle() != null) {
			return getVehicle().dismountsUnderwater();
		}
		return true;
	}

	@Override
	public boolean isEyeInFluid(TagKey<Fluid> tag) {
		return false;
	}
}
