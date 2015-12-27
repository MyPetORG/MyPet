/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagShort;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Material.AIR;
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
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        if (block != null && block.getType() != AIR) {
            info.getCompoundData().put("Block", ItemStackNBTConverter.itemStackToCompund(CraftItemStack.asNMSCopy(block)));
        }
        //info.getValue().put("Screaming", new TagByte("Screaming", isScreaming()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("BlockID")) {
            int id;
            int data = 0;

            if (info.containsKeyAs("BlockID", TagShort.class)) {
                id = info.getAs("BlockID", TagShort.class).getShortData();
            } else {
                id = info.getAs("BlockID", TagInt.class).getIntData();
            }
            if (info.containsKeyAs("BlockData", TagShort.class)) {
                data = info.getAs("BlockData", TagShort.class).getShortData();
            } else if (info.containsKeyAs("BlockData", TagInt.class)) {
                data = info.getAs("BlockData", TagInt.class).getIntData();
            }
            setBlock(new ItemStack(Material.getMaterial(id), 1, (short) data));
        } else if (info.getCompoundData().containsKey("Block")) {
            TagCompound itemStackCompund = info.getAs("Block", TagCompound.class);
            ItemStack block = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compundToItemStack(itemStackCompund));
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
