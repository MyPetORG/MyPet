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

package de.Keyle.MyPet.compat.v1_12_R1.entity.types;

import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import net.minecraft.server.v1_12_R1.Material;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class EntityMySeat extends EntityArmorStand {
	public EntityMySeat(World level, double x, double y, double z) {
		super(level, x, y, z);
		setSilent(true);
		setNoGravity(true);
		setSmall(true);
		setMarker(true);
		setInvisible(true);
		setInvulnerable(true);
		//persistent = false;
	}

	public static boolean mountToPet(Entity passenger, Entity myPet) {
		Entity seat = new EntityMySeat(myPet.world, myPet.locX, myPet.locY, myPet.locZ);
		if (myPet.world.addEntity(seat, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
			if (seat.startRiding(myPet)) {
				if (passenger.startRiding(seat)) {
					return true;
				}
				seat.stopRiding();
			}
			seat.die();
		}
		return false;
	}

	@Override
	public void B_() {
		super.B_();
		if (bF().isEmpty()) { //Done riding? Begone seat
			stopRiding();
			die();
		} else if (this.getVehicle() == null || !this.getVehicle().isAlive()) { //Got blown off the thing or something
			ejectPassengers();
			die();
		}
	}

	@Override
	public boolean a(Material material) {	//eyeInFluid
		return false;
	}
}
