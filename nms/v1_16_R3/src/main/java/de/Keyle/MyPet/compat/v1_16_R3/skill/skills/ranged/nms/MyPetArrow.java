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

package de.Keyle.MyPet.compat.v1_16_R3.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_16_R3.skill.skills.ranged.bukkit.CraftMyPetArrow;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.EntityTippedArrow;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.Items;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.World;

@Compat("v1_16_R3")
public class MyPetArrow extends EntityTippedArrow implements EntityMyPetProjectile {

	protected CraftMyPetArrow bukkitEntity = null;

	public MyPetArrow(World world, EntityMyPet entityMyPet) {
		super(world, entityMyPet);
	}

	@Override
	public EntityMyPet getShooter() {
		return (EntityMyPet) super.getShooter();
	}

	@Override
	public CraftMyPetArrow getBukkitEntity() {
		if (this.bukkitEntity == null) {
			this.bukkitEntity = new CraftMyPetArrow(this.world.getServer(), this);
		}
		return this.bukkitEntity;
	}

	@Override
	public void saveData(NBTTagCompound nbttagcompound) {

	}

	@Override
	protected ItemStack getItemStack() {
		return new ItemStack(Items.ARROW);
	}

	@Override
	public void loadData(NBTTagCompound nbttagcompound) {
	}

	@Override
	public void tick() {
		try {
			super.tick();
			if (this.inGround) {
				die();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean damageEntity(DamageSource damagesource, float f) {
		return false;
	}
}