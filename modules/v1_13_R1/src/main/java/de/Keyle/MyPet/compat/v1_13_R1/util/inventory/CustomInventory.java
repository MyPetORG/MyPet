/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_13_R1.util.inventory;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import net.minecraft.server.v1_13_R1.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Compat("v1_13_R1")
public class CustomInventory implements IInventory, Listener, de.Keyle.MyPet.api.util.inventory.CustomInventory {

    private String inventroyName = "";
    private NonNullList<ItemStack> items = NonNullList.a(64, ItemStack.a);
    private int size = 0;
    private int stackSize = 64;
    private List<HumanEntity> transaction = new ArrayList<>();
    private CraftInventory bukkitInventory = null;

    public CustomInventory() {
        Bukkit.getPluginManager().registerEvents(this, MyPetApi.getPlugin());
    }

    public CustomInventory(int size, String name) {
        this();
        setSize(size);
        setName(name);
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        size = (int) (size / 9.);
        size *= 9;
        this.size = Util.clamp(size, 0, 54);
        for (int i = items.size(); i < this.size; i++) {
            items.set(i, ItemStack.a);
        }
    }

    public String getName() {
        return inventroyName;
    }

    public void setName(String name) {
        if (name != null) {
            name = StringUtils.left(name, 64);
            this.inventroyName = name;
        }
    }

    public ItemStack getItem(int i) {
        if (i < size) {
            return items.get(i);
        }
        return ItemStack.a;
    }

    public void setItem(int i, ItemStack itemStack) {
        if (i < items.size()) {
            items.set(i, itemStack);
        } else {
            items.add(i, itemStack);
        }
        update();
    }

    public int addItem(org.bukkit.inventory.ItemStack itemAdd) {
        if (itemAdd == null) {
            return 0;
        }
        itemAdd = itemAdd.clone();

        for (int i = 0; i < this.getSize(); i++) {
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
            for (int i = 0; i < this.getSize(); i++) {
                if (getItem(i) == ItemStack.a) {
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
            bukkitInventory = new CraftInventory(this);
        }
        return bukkitInventory;
    }

    public void dropContentAt(Location loc) {
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        for (int i = 0; i < this.getSize(); i++) {
            ItemStack is = this.splitWithoutUpdate(i);
            if (is != null && !is.isEmpty()) {
                is = is.cloneItemStack();
                EntityItem itemEntity = new EntityItem(world, loc.getX(), loc.getY(), loc.getZ(), is);
                itemEntity.pickupDelay = 20;
                world.addEntity(itemEntity);
            }
        }
    }

    public ItemStack splitStack(int slot, int subtract) {
        if (slot < size && items.get(slot) != ItemStack.a) {
            if (items.get(slot).getCount() <= subtract) {
                ItemStack itemStack = items.get(slot);
                items.set(slot, ItemStack.a);
                return itemStack;
            } else {
                ItemStack splittedStack = items.get(slot).cloneAndSubtract(subtract);

                if (items.get(slot).getCount() == 0) {
                    items.set(slot, ItemStack.a);
                }
                return splittedStack;
            }
        }
        return ItemStack.a;
    }

    public List<ItemStack> getContents() {
        List<ItemStack> itemStack = new LinkedList<>();
        for (int i = 0; i < getSize(); i++) {
            itemStack.add(i, items.get(i));
        }
        return itemStack;
    }

    public TagCompound save(TagCompound compound) {
        List<TagCompound> itemList = new ArrayList<>();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemStack = this.items.get(i);
            if (itemStack != ItemStack.a) {
                TagCompound item = ItemStackNBTConverter.itemStackToCompound(itemStack);
                item.getCompoundData().put("Slot", new TagByte((byte) i));
                itemList.add(item);
            }
        }
        compound.getCompoundData().put("Items", new TagList(itemList));
        return compound;
    }

    public void load(TagCompound nbtTagCompound) {
        TagList items = nbtTagCompound.getAs("Items", TagList.class);

        for (int i = 0; i < items.size(); i++) {
            TagCompound itemCompound = items.getTagAs(i, TagCompound.class);

            ItemStack itemStack = ItemStackNBTConverter.compoundToItemStack(itemCompound);
            setItem(itemCompound.getAs("Slot", TagByte.class).getByteData(), itemStack);
        }
    }

    public boolean a(EntityHuman entityHuman) {
        return true;
    }

    public void startOpen(EntityHuman paramEntityHuman) {
    }

    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
        if (items.size() > this.size) {
            for (int counterOutside = items.size() - 1; counterOutside >= this.size; counterOutside--) {
                if (items.get(counterOutside) != ItemStack.a) {
                    for (int counterInside = 0; counterInside < size; counterInside++) {
                        if (items.get(counterInside) == ItemStack.a) {
                            items.set(counterInside, items.get(counterOutside));
                            items.set(counterOutside, ItemStack.a);
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

    public void close() {
        if (transaction.size() > 0) {
            for (HumanEntity humanEntity : new ArrayList<>(transaction)) {
                humanEntity.closeInventory();
            }
        }
    }

    @Override
    public void open(Player player) {
        EntityPlayer eh = ((CraftPlayer) player).getHandle();
        eh.openContainer(this);
    }

    public void closeContainer(EntityHuman paramEntityHuman) {
    }

    public List<HumanEntity> getViewers() {
        return this.transaction;
    }

    public InventoryHolder getOwner() {
        return null;
    }

    public int getMaxStackSize() {
        return stackSize;
    }

    public void setMaxStackSize(int i) {
        this.stackSize = i;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    public ItemStack splitWithoutUpdate(int i) {
        if (items.get(i) != ItemStack.a) {
            ItemStack itemstack = items.get(i);

            items.set(i, ItemStack.a);
            return itemstack;
        }
        return ItemStack.a;
    }

    public void update() {
    }

    public boolean b(int paramInt, ItemStack paramItemStack) {
        return true;
    }

    @Override
    public int getProperty(int i) {
        return 0;
    }

    @Override
    public void setProperty(int i, int i1) {

    }

    @Override
    public void clear() {
    }

    @Override
    public int h() {
        return 0;
    }

    @Override
    public boolean P_() {
        return items.size() == 0;
    }

    @Override
    public boolean hasCustomName() {
        return this.inventroyName != null;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatComponentText(this.inventroyName);
    }

    @Nullable
    @Override
    public IChatBaseComponent getCustomName() {
        return new ChatComponentText(this.inventroyName);
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return new ChatComponentText(this.inventroyName);
    }
}