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
import de.Keyle.MyPet.api.entity.types.MyRavager;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.RideImpl;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

@EntitySize(width = 1.95F, height = 2.2F)
public class EntityMyRavager extends EntityMyPet {

	protected static final DataWatcherObject<Boolean> RAID_WATCHER = DataWatcher.a(EntityMyRavager.class, DataWatcherRegistry.i);

	public EntityMyRavager(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.ravager.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.ravager.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.ravager.ambient";
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(RAID_WATCHER, false);

	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.ravager.step", 0.15F, 1.0F);
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(itemStack)) {
			if (myPet.getSkills().isActive(RideImpl.class) && canMove()) {
				getOwner().sendMessage("Unfortunately, Ravagers can not be ridden (Minecraft limitation)", 5000);
				return EnumInteractionResult.b;
			}
		}
		return super.handlePlayerInteraction(entityhuman, enumhand, itemStack);
	}

	@Override
	public MyRavager getMyPet() {
		return (MyRavager) myPet;
	}
}
