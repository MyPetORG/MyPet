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

package de.Keyle.MyPet.compat.v1_19_R1_1.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWarden;
import de.Keyle.MyPet.compat.v1_19_R1_1.entity.EntityMyPet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;

import java.util.concurrent.ThreadLocalRandom;

@EntitySize(width = 1.4F, height = 2.7F)
public class EntityMyWarden extends EntityMyPet {
	int heartBeatTimer = 0;
	boolean heartAttack = false;
	boolean emerged = false;

	public EntityMyWarden(Level world, MyPet myPet) {
		super(world, myPet);
		if(ThreadLocalRandom.current().nextInt(  1000 + 1) == 42) {
			this.heartAttack = true;
		}
	}

	@Override
	public boolean attack(Entity entity) {
		boolean flag = false;
		try {
			this.level.broadcastEntityEvent(this, (byte) 4);
			flag = super.attack(entity);
			this.makeSound("entity.warden.attack_impact", 1.0F, 1.0F);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	public void tick() {
		super.tick();
		if(!this.emerged) {
			this.setPose(Pose.EMERGING);
			this.makeSound("entity.warden.emerge");
			Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> this.setPose(Pose.STANDING), 135);
			this.emerged = true;
		}

		if(this.heartAttack) {
			this.playSound(Registry.SOUND_EVENT.get(new ResourceLocation("entity.warden.heartbeat")));
		}
	}

	@Override
	protected String getMyPetDeathSound() {
		return "entity.warden.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.warden.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.warden.ambient";
	}

	@Override
	public MyWarden getMyPet() {
		return (MyWarden) myPet;
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.warden.step", 1.0F, 1.0F);
	}

	public void remove(Entity.RemovalReason entity_removalreason) {
		//Play sound
		this.makeSound("entity.warden.dig");
		this.setPose(Pose.DIGGING);
		if(!MyPetApi.getPlugin().isDisabling()) {
			Bukkit.getScheduler().runTaskLater(MyPetApi.getPlugin(), () -> super.remove(entity_removalreason), 100);
		} else {
			super.remove(entity_removalreason);
		}
	}

	private void makeSound(String leSound) {
		SoundEvent se = Registry.SOUND_EVENT.get(new ResourceLocation(leSound));
		this.playSound(se, 5F,1F);
	}
}
