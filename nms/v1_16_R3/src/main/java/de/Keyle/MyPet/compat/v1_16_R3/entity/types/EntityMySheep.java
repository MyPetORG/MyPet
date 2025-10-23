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

package de.Keyle.MyPet.compat.v1_16_R3.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MySheep;
import de.Keyle.MyPet.compat.v1_16_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_16_R3.entity.ai.movement.EatGrass;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.DyeColor;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static de.Keyle.MyPet.compat.v1_16_R3.CompatManager.ENTITY_LIVING_broadcastItemBreak;

@EntitySize(width = 0.7F, height = 1.2349999f)
public class EntityMySheep extends EntityMyPet {

	private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMySheep.class, DataWatcherRegistry.i);
	private static final DataWatcherObject<Byte> COLOR_WATCHER = DataWatcher.a(EntityMySheep.class, DataWatcherRegistry.a);

	private static final Map<EnumColor, Block> colorMap = new HashMap<>();

	static {
		colorMap.put(EnumColor.WHITE, Blocks.WHITE_WOOL);
		colorMap.put(EnumColor.ORANGE, Blocks.ORANGE_WOOL);
		colorMap.put(EnumColor.MAGENTA, Blocks.MAGENTA_WOOL);
		colorMap.put(EnumColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
		colorMap.put(EnumColor.YELLOW, Blocks.YELLOW_WOOL);
		colorMap.put(EnumColor.LIME, Blocks.LIME_WOOL);
		colorMap.put(EnumColor.PINK, Blocks.PINK_WOOL);
		colorMap.put(EnumColor.GRAY, Blocks.GRAY_WOOL);
		colorMap.put(EnumColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
		colorMap.put(EnumColor.CYAN, Blocks.CYAN_WOOL);
		colorMap.put(EnumColor.PURPLE, Blocks.PURPLE_WOOL);
		colorMap.put(EnumColor.BLUE, Blocks.BLUE_WOOL);
		colorMap.put(EnumColor.BROWN, Blocks.BROWN_WOOL);
		colorMap.put(EnumColor.GREEN, Blocks.GREEN_WOOL);
		colorMap.put(EnumColor.RED, Blocks.RED_WOOL);
		colorMap.put(EnumColor.BLACK, Blocks.BLACK_WOOL);
	}

	public EntityMySheep(World world, MyPet myPet) {
		super(world, myPet);
	}

	@Override
	protected String getDeathSound() {
		return "entity.sheep.death";
	}

	@Override
	protected String getHurtSound() {
		return "entity.sheep.hurt";
	}

	@Override
	protected String getLivingSound() {
		return "entity.sheep.ambient";
	}

	@Override
	public EnumInteractionResult handlePlayerInteraction(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemStack) {
		if (super.handlePlayerInteraction(entityhuman, enumhand, itemStack).a()) {
			return EnumInteractionResult.CONSUME;
		}

		if (getOwner().equals(entityhuman) && itemStack != null && canUseItem()) {
			if (itemStack.getItem() instanceof ItemDye && ((ItemDye) itemStack.getItem()).d().ordinal() != getMyPet().getColor().ordinal() && !getMyPet().isSheared()) {
				getMyPet().setColor(DyeColor.values()[((ItemDye) itemStack.getItem()).d().ordinal()]);
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				return EnumInteractionResult.CONSUME;
			} else if (itemStack.getItem() == Items.SHEARS && Configuration.MyPet.Sheep.CAN_BE_SHEARED && !getMyPet().isSheared()) {
				getMyPet().setSheared(true);
				int woolDropCount = 1 + this.random.nextInt(3);

				for (int j = 0; j < woolDropCount; ++j) {
					EntityItem entityitem = new EntityItem(this.world, this.locX(), this.locY() + 1, this.locZ(), new ItemStack(colorMap.get(EnumColor.values()[getMyPet().getColor().ordinal()])));
					entityitem.pickupDelay = 10;
					entityitem.setMot(entityitem.getMot().add(0, this.random.nextFloat() * 0.05F, 0));
					this.world.addEntity(entityitem);
				}
				makeSound("entity.sheep.shear", 1.0F, 1.0F);
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					try {
						itemStack.damage(1, entityhuman, (entityhuman1) -> entityhuman1.broadcastItemBreak(enumhand));
					} catch (Error e) {
						// TODO REMOVE
						itemStack.damage(1, entityhuman, (entityhuman1) -> {
							try {
								ENTITY_LIVING_broadcastItemBreak.invoke(entityhuman1, enumhand);
							} catch (IllegalAccessException | InvocationTargetException ex) {
								ex.printStackTrace();
							}
						});
					}
				}
				return EnumInteractionResult.CONSUME;
			} else if (Configuration.MyPet.Sheep.GROW_UP_ITEM.compare(itemStack) && getMyPet().isBaby() && getOwner().getPlayer().isSneaking()) {
				if (itemStack != ItemStack.b && !entityhuman.abilities.canInstantlyBuild) {
					itemStack.subtract(1);
					if (itemStack.getCount() <= 0) {
						entityhuman.inventory.setItem(entityhuman.inventory.itemInHandIndex, ItemStack.b);
					}
				}
				getMyPet().setBaby(false);
				return EnumInteractionResult.CONSUME;
			}
		}
		return EnumInteractionResult.PASS;
	}

	@Override
	protected void initDatawatcher() {
		super.initDatawatcher();
		getDataWatcher().register(AGE_WATCHER, false);
		getDataWatcher().register(COLOR_WATCHER, (byte) 0); // color/sheared
	}

	@Override
	public void updateVisuals() {
		getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());

		byte data = (byte) (getMyPet().isSheared() ? 16 : 0);
		getDataWatcher().set(COLOR_WATCHER, (byte) (data & 0xF0 | getMyPet().getColor().ordinal() & 0xF));
	}

	@Override
	public void playPetStepSound() {
		makeSound("entity.sheep.step", 0.15F, 1.0F);
	}

	@Override
	public MySheep getMyPet() {
		return (MySheep) myPet;
	}

	@Override
	public void setPathfinder() {
		super.setPathfinder();
		petPathfinderSelector.addGoal("EatGrass", new EatGrass(this));
	}
}