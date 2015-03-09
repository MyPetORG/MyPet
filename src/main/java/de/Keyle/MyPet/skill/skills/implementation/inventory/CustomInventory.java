/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation.inventory;

import de.Keyle.MyPet.MyPetPlugin;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class CustomInventory implements IInventory, Listener {
    private String inventroyName;
    private List<ItemStack> items = new ArrayList<ItemStack>();
    private int size = 0;
    private int stackSize = 64;
    private List<HumanEntity> transaction = new ArrayList<HumanEntity>();

    public CustomInventory(String inventroyName, int size) {
        setName(inventroyName);
        setSize(size);
        MyPetPlugin.getPlugin().getServer().getPluginManager().registerEvents(this, MyPetPlugin.getPlugin());
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        for (int i = items.size(); i < size; i++) {
            items.add(i, null);
        }
    }

    public String getName() {
        return inventroyName;
    }

    public void setName(String name) {
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        this.inventroyName = name;
    }

    public ItemStack getItem(int i) {
        if (i <= size) {
            return items.get(i);
        }
        return null;
    }

    public void setItem(int i, ItemStack itemStack) {
        if (i < items.size()) {
            items.set(i, itemStack);
        } else {
            for (int x = items.size(); x < i; x++) {
                items.add(x, null);
            }
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
                if (getItem(i) == null) {
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

    public void dropContentAt(Location loc) {
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        for (int i = 0; i < this.getSize(); i++) {
            ItemStack is = this.splitWithoutUpdate(i);
            if (is != null) {
                is = is.cloneItemStack();
                EntityItem itemEntity = new EntityItem(world, loc.getX(), loc.getY(), loc.getZ(), is);
                itemEntity.pickupDelay = 20;
                world.addEntity(itemEntity);
            }
        }
    }

    public ItemStack splitStack(int i, int j) {
        if (i <= size && items.get(i) != null) {
            ItemStack itemStack;
            if (items.get(i).count <= j) {
                itemStack = items.get(i);
                items.set(i, null);
                return itemStack;
            } else {
                itemStack = items.get(i).a(j);
                if (items.get(i).count == 0) {
                    items.set(i, null);
                }
                return itemStack;
            }
        }
        return null;
    }

    public ItemStack[] getContents() {
        ItemStack[] itemStack = new ItemStack[getSize()];
        for (int i = 0; i < getSize(); i++) {
            itemStack[i] = items.get(i);
        }
        return itemStack;
    }

    public TagCompound save(TagCompound compound) {
        List<TagCompound> itemList = new ArrayList<TagCompound>();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemStack = this.items.get(i);
            if (itemStack != null) {
                TagCompound item = ItemStackNBTConverter.itemStackToCompund(itemStack);
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

            ItemStack itemStack = ItemStackNBTConverter.compundToItemStack(itemCompound);
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
                if (items.get(counterOutside) != null) {
                    for (int counterInside = 0; counterInside < size; counterInside++) {
                        if (items.get(counterInside) == null) {
                            items.set(counterInside, items.get(counterOutside));
                            items.set(counterOutside, null);
                        }
                    }
                }
                if (items.get(counterOutside) == null) {
                    items.remove(counterOutside);
                }
            }
        }
    }

    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(MyPetPlugin.getPlugin())) {
            close();
        }
    }

    public void close() {
        if (transaction.size() > 0) {
            for (HumanEntity humanEntity : new ArrayList<HumanEntity>(transaction)) {
                humanEntity.closeInventory();
            }
        }
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

    public ItemStack splitWithoutUpdate(int i) {
        if (items.get(i) != null) {
            ItemStack itemstack = items.get(i);

            items.set(i, null);
            return itemstack;
        }
        return null;
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
    public void b(int i, int i1) {

    }

    @Override
    public int g() {
        return 0;
    }

    @Override
    public void l() {

    }

    @Override
    public boolean hasCustomName() {
        return this.inventroyName != null;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatComponentText(this.inventroyName);
    }
}