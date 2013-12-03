/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.entity.types.enderman;


import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.ShortTag;
import org.spout.nbt.TagType;

import static org.bukkit.Material.SOUL_SAND;

@MyPetInfo(food = {SOUL_SAND})
public class MyEnderman extends MyPet {

    public boolean isScreaming = false;
    public ItemStack block = null;

    public MyEnderman(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public ItemStack getBlock() {
        return block;
    }

    @Override
    public CompoundTag getExtendedInfo() {
        CompoundTag info = super.getExtendedInfo();
        if (block != null) {
            info.getValue().put("Block", ItemStackNBTConverter.ItemStackToCompund(CraftItemStack.asNMSCopy(block), "Block"));
        }
        //info.getValue().put("Screaming", new ByteTag("Screaming", isScreaming()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info) {
        if (info.getValue().containsKey("BlockID")) {
            int id;
            int data = 0;
            if (info.getValue().get("BlockID").getType() == TagType.TAG_SHORT) {
                id = ((ShortTag) info.getValue().get("BlockID")).getValue();
            } else {
                id = ((IntTag) info.getValue().get("BlockID")).getValue();
            }
            if (info.getValue().containsKey("BlockData")) {
                if (info.getValue().get("BlockData").getType() == TagType.TAG_SHORT) {
                    data = ((ShortTag) info.getValue().get("BlockData")).getValue();
                } else {
                    data = ((IntTag) info.getValue().get("BlockData")).getValue();
                }
            }
            setBlock(new ItemStack(id, 1, (short) data));
        } else if (info.getValue().containsKey("Block")) {
            CompoundTag itemStackCompund = (CompoundTag) info.getValue().get("Block");
            ItemStack block = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.CompundToItemStack(itemStackCompund));
            setBlock(block);
        }

        //setScreaming((()info.getValue().get("Screaming")).getBooleanValue());
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Enderman;
    }

    public boolean isScreaming() {
        return isScreaming;
    }

    public void setScreaming(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyEnderman) getCraftPet().getHandle()).setScreaming(flag);
        }
        this.isScreaming = flag;
    }

    public boolean hasBlock() {
        return block != null;
    }

    public void setBlock(ItemStack block) {
        if (block != null) {
            this.block = block.clone();
            this.block.setAmount(1);

            if (status == PetState.Here) {
                ((EntityMyEnderman) getCraftPet().getHandle()).setBlock(this.block.getTypeId(), this.block.getData().getData());
            }
        } else {
            if (status == PetState.Here) {
                ((EntityMyEnderman) getCraftPet().getHandle()).setBlock(0, 0);
            }
            this.block = null;
        }
    }

    @Override
    public String toString() {
        return "MyEnderman{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ",Block=" + block + "}";
    }
}
