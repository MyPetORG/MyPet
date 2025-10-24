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

package de.Keyle.MyPet.compat.v1_20_R1.entity;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftMob;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftEntityEquipment;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@Compat("v1_20_R1")
public class CraftMyPet extends CraftMob implements MyPetBukkitEntity {

	protected MyPetPlayer petOwner;
	protected de.Keyle.MyPet.compat.v1_20_R1.entity.EntityMyPet petEntity;
	protected CraftEntityEquipment fakeEquipment;

	public CraftMyPet(CraftServer server, de.Keyle.MyPet.compat.v1_20_R1.entity.EntityMyPet entityMyPet) {
		super(server, entityMyPet);
		petEntity = entityMyPet;
		fakeEquipment = new FakeEquipment(this);

		Field typeValue = ReflectionUtil.getField(CraftEntity.class, "entityType");
		ReflectionUtil.setFieldValue(typeValue,this, EntityType.BLOCK_DISPLAY);
	}

	@Override
	public boolean canMove() {
		return petEntity.canMove();
	}


	@Override
	public void setSitting(boolean sitting) {
		getHandle().setSitting(sitting);
	}

	@Override
	public boolean isSitting() {
		return getHandle().isSitting();
	}

	@Override
	public boolean isInvisible() {
		return getHandle().isInvisible();
	}

	@Override
	public void setInvisible(boolean invisible) {
	}

	@Override
	public de.Keyle.MyPet.compat.v1_20_R1.entity.EntityMyPet getHandle() {
		return petEntity;
	}

	@Override
	public MyPet getMyPet() {
		return petEntity.getMyPet();
	}

	@Override
	public MyPetPlayer getOwner() {
		if (petOwner == null) {
			petOwner = getMyPet().getOwner();
		}
		return petOwner;
	}

	@Override
	public void removeEntity() {
		getHandle().discard();
	}

	@Override
	public MyPetType getPetType() {
		return getMyPet().getPetType();
	}

	/* This doesn't work rn as getType was made final...
		I saw other plugins do it this way - it should be fine and solve problems with p2 and wg
	//Update - It wasn't! - GriefPrevention didn't like the previous solution!
	//So now this ugly bs will solve it. Hopefully
	@Override
	public EntityType getType() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		String class1 = stackTraceElements[2].getClassName();
		String class2 = stackTraceElements[3].getClassName();
		//all special cases here
		if(class1.contains("worldedit") || class2.contains("worldedit") ||
				class1.contains("plotsquared") || class2.contains("plotsquared") ||
				class1.contains("worldguard") || class2.contains("worldguard")  ||
				class1.contains("towny") || class2.contains("towny")) {
			return EntityType.valueOf(this.getPetType().getBukkitName());
		}

		return EntityType.UNKNOWN;
	} */

	/* I have no clue why I need to override these other deprecated methods also don't need to be implemented so yea...*/
	@Override
	public void setVisibleByDefault(boolean b) {
	}

	@Override
	public boolean isVisibleByDefault() {
		return true;
	}

	@NotNull
	@Override
	public SpawnCategory getSpawnCategory() {
		return SpawnCategory.MISC;
	}

	@Override
	public boolean isInWater() {
		return getHandle().isInWater();
	}

	@Override
	public void remove() {
		// do nothing to prevent other plugins from removing the MyPet
		// user removeEntity() to remove the MyPet
	}

	@Override
	public boolean isPersistent() {
		return false;
	}

	@Override
	public void setPersistent(boolean b) {
	}

	@Override
	public void setHealth(double health) {
		if (health < 0) {
			health = 0;
		}
		if (health > getMaxHealth()) {
			health = getMaxHealth();
		}
		super.setHealth(health);
	}

	@Override
	public int getArrowCooldown() {
		return 0;
	}

	@Override
	public void setArrowCooldown(int i) {

	}

	@Override
	public int getArrowsInBody() {
		return 0;
	}

	@Override
	public void setArrowsInBody(int i) {

	}

	@Override
	public EntityEquipment getEquipment() {
		return fakeEquipment;
	}
	

	@Override
	public void attack(@NotNull Entity entity) {
		this.petEntity.attack(((CraftEntity) entity).getHandle());
	}

	@Override
	public void swingMainHand() {
	}

	@Override
	public void swingOffHand() {
	}

	@Override
	public void setTarget(LivingEntity target) {
		setTarget(target, TargetPriority.Bukkit);
	}

	@Override
	public void setAware(boolean b) {
	}

	@Override
	public boolean isAware() {
		return true;
	}

	@Override
	public void forgetTarget() {
		getHandle().forgetTarget();
	}

	@Override
	public void setTarget(LivingEntity target, TargetPriority priority) {
		getHandle().setMyPetTarget(target, priority);
	}

	@Override
	public String toString() {
		return "CraftMyPet{MyPet=" + getHandle().isMyPet() + ",owner=" + getOwner() + ",type=" + getPetType() + "}";
	}

	private class FakeEquipment extends CraftEntityEquipment {

		public FakeEquipment(CraftLivingEntity entity) {
			super(entity);
		}

		@NotNull
		@Override
		public ItemStack getItemInMainHand() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@NotNull
		@Override
		public ItemStack getItemInOffHand() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@NotNull
		@Override
		public ItemStack getItemInHand() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@Override
		public ItemStack getHelmet() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@Override
		public ItemStack getChestplate() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@Override
		public ItemStack getLeggings() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@Override
		public ItemStack getBoots() {
			return CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
		}

		@NotNull
		@Override
		public ItemStack[] getArmorContents() {
			ItemStack empty = CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.EMPTY);
			return new ItemStack[]{empty, empty, empty, empty};
		}
	}
}
