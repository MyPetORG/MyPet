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

package de.Keyle.MyPet.compat.v1_21_R5.util.inventory;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R5.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Compat("v1_21_R5")
public class CustomInventory implements Container, Listener, de.Keyle.MyPet.api.util.inventory.CustomInventory {

	private String inventoryName = "";
	private final NonNullList<ItemStack> items = NonNullList.withSize(64, ItemStack.EMPTY);
	private int size = 0;
	private int stackSize = 64;
	private final List<HumanEntity> transaction = new ArrayList<>();
	private CraftInventory bukkitInventory = null;

	public CustomInventory() {
		Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
	}

	public CustomInventory(int size, String name) {
		this();
		setSize(size);
		setName(name);
	}
	
	@Override
	public int getContainerSize() {
		return this.size;
	}

	@Override
	public void setSize(int size) {
		size = (int) (size / 9.);
		size *= 9;
		this.size = Util.clamp(size, 9, 54);
		for (int i = items.size(); i < this.size; i++) {
			items.set(i, ItemStack.EMPTY);
		}
	}

	@Override
	public String getName() {
		return inventoryName;
	}

	@Override
	public void setName(String name) {
		if (name != null) {
			name = StringUtils.left(name, 64);
			this.inventoryName = name;
		}
	}

	@Override
	public ItemStack getItem(int i) {
		if (i < size) {
			return items.get(i);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int i, ItemStack itemStack) {
		if (i < items.size()) {
			items.set(i, itemStack);
		} else {
			items.add(i, itemStack);
		}
		setChanged();
	}

	@Override
	public int addItem(org.bukkit.inventory.ItemStack itemAdd) {
		if (itemAdd == null) {
			return 0;
		}
		itemAdd = itemAdd.clone();

		for (int i = 0; i < this.getContainerSize(); i++) {
			CraftItemStack craftItem = CraftItemStack.asCraftMirror(getItem(i));

			if (ItemStackComparator.compareItem(itemAdd, craftItem)) {
				if (craftItem.getAmount() >= craftItem.getMaxStackSize()) {
					continue;
				}
				while (craftItem.getAmount() < craftItem.getMaxStackSize() && itemAdd.getAmount() > 0) {
					craftItem.setAmount(craftItem.getAmount() + 1);
					itemAdd.setAmount(itemAdd.getAmount() - 1);
				}
				if (itemAdd.getAmount() == 0) {
					break;
				}
			}
		}
		if (itemAdd.getAmount() > 0) {
			for (int i = 0; i < this.getContainerSize(); i++) {
				if (getItem(i).isEmpty()) {
					if (itemAdd.getAmount() <= itemAdd.getMaxStackSize()) {
						setItem(i, CraftItemStack.asNMSCopy(itemAdd.clone()));
						itemAdd.setAmount(0);
						break;
					} else {
						CraftItemStack itemStack = (CraftItemStack) itemAdd.clone();
						itemStack.setAmount(itemStack.getMaxStackSize());
						setItem(i, CraftItemStack.asNMSCopy(itemStack));
						itemAdd.setAmount(itemAdd.getAmount() - itemStack.getMaxStackSize());
					}
					if (itemAdd.getAmount() == 0) {
						break;
					}
				}
			}
		}
		return itemAdd.getAmount();
	}

	@Override
	public Inventory getBukkitInventory() {
		if (bukkitInventory == null) {
			bukkitInventory = new CraftInventory(this) {

				public String getTitle() {
					return inventoryName;
				}

				public String getName() {
					return inventoryName;
				}
			};
		}
		return bukkitInventory;
	}

	@Override
	public void dropContentAt(Location loc) {
		Level world = ((CraftWorld) loc.getWorld()).getHandle();
		for (int i = 0; i < this.getContainerSize(); i++) {
			ItemStack is = this.removeItemNoUpdate(i);
			if (is != null && !is.isEmpty()) {
				is = is.copy();
				ItemEntity itemEntity = new ItemEntity(world, loc.getX(), loc.getY(), loc.getZ(), is);
				itemEntity.pickupDelay = 20;
				world.addFreshEntity(itemEntity);
			}
		}
	}

	@Override
	public ItemStack removeItem(int slot, int subtract) {
		if (slot < size && !items.get(slot).isEmpty()) {
			if (items.get(slot).getCount() <= subtract) {
				ItemStack itemStack = items.get(slot);
				items.set(slot, ItemStack.EMPTY);
				return itemStack;
			} else {
				ItemStack splittedStack = items.get(slot).split(subtract);

				if (items.get(slot).getCount() == 0) {
					items.set(slot, ItemStack.EMPTY);
				}
				return splittedStack;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public List<ItemStack> getContents() {
		List<ItemStack> itemStack = new LinkedList<>();
		for (int i = 0; i < getContainerSize(); i++) {
			itemStack.add(i, items.get(i));
		}
		return itemStack;
	}

	@Override
	public TagCompound save(TagCompound compound) {
		List<TagCompound> itemList = new ArrayList<>();
		for (int i = 0; i < this.items.size(); i++) {
			ItemStack itemStack = this.items.get(i);
			if (!itemStack.isEmpty()) {
				TagCompound item = ItemStackNBTConverter.itemStackToCompound(itemStack);
				item.getCompoundData().put("Slot", new TagByte((byte) i));
				itemList.add(item);
			}
		}
		compound.getCompoundData().put("Items", new TagList(itemList));
		return compound;
	}

	@Override
	public void load(TagCompound nbtTagCompound) {
		TagList items = nbtTagCompound.getAs("Items", TagList.class);

		for (int i = 0; i < items.size(); i++) {
			TagCompound itemCompound = items.getTagAs(i, TagCompound.class);

			// Make sure old items are compatible
			if (itemCompound.containsKey("Count"))
				itemCompound.put("count", itemCompound.get("Count"));

			ItemStack itemStack = ItemStackNBTConverter.compoundToItemStack(itemCompound);
			setItem(itemCompound.getAs("Slot", TagByte.class).getByteData(), itemStack);
		}
	}

	@Override
	public boolean stillValid(net.minecraft.world.entity.player.Player entityHuman) {
		return true;
	}

	@Override
	public void onOpen(CraftHumanEntity who) {
		this.transaction.add(who);
	}

	@Override
	public void onClose(CraftHumanEntity who) {
		this.transaction.remove(who);
		if (items.size() > this.size) {
			for (int counterOutside = items.size() - 1; counterOutside >= this.size; counterOutside--) {
				if (!items.get(counterOutside).isEmpty()) {
					for (int counterInside = 0; counterInside < size; counterInside++) {
						if (items.get(counterInside).isEmpty()) {
							items.set(counterInside, items.get(counterOutside));
							items.set(counterOutside, ItemStack.EMPTY);
						}
					}
				}
			}
		}
	}

	@EventHandler
	void onPluginDisable(PluginDisableEvent event) {
		if (event.getPlugin().equals(MyPetApi.getPlugin())) {
			close();
		}
	}

	@Override
	public void close() {
		if (transaction.size() > 0) {
			for (HumanEntity humanEntity : new ArrayList<>(transaction)) {
				humanEntity.closeInventory();
			}
		}
	}

	@Override
	public void open(Player player) {
		ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();
		AbstractContainerMenu container = new CraftContainer(getBukkitInventory(), entityPlayer, entityPlayer.nextContainerCounter());
		container = CraftEventFactory.callInventoryOpenEvent(entityPlayer, container);
		if (container != null) {
			MenuType<?> customSize = MenuType.GENERIC_9x1;
			switch (this.getContainerSize()) {
				case 18:
					customSize = MenuType.GENERIC_9x2;
					break;
				case 27:
					customSize = MenuType.GENERIC_9x3;
					break;
				case 36:
					customSize = MenuType.GENERIC_9x4;
					break;
				case 45:
					customSize = MenuType.GENERIC_9x5;
					break;
				case 54:
					customSize = MenuType.GENERIC_9x6;
					break;
			}
			entityPlayer.connection.send(new ClientboundOpenScreenPacket(container.containerId, customSize, Component.translatable(this.getName())));
			entityPlayer.containerMenu = container;
			entityPlayer.initMenu(container);
		}
	}

	@Override
	public List<HumanEntity> getViewers() {
		return this.transaction;
	}

	@Override
	public InventoryHolder getOwner() {
		return null;
	}

	@Override
	public int getMaxStackSize() {
		return stackSize;
	}

	@Override
	public void setMaxStackSize(int i) {
		this.stackSize = i;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public ItemStack removeItemNoUpdate(int i) {
		if (!items.get(i).isEmpty()) {
			ItemStack itemstack = items.get(i);

			items.set(i, ItemStack.EMPTY);
			return itemstack;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public void clearContent() {
	}

	public boolean isNotEmpty() {
		return !this.isEmpty();
	}

	@Override
	public boolean isEmpty() {
		return items.size() == 0;
	}
}
