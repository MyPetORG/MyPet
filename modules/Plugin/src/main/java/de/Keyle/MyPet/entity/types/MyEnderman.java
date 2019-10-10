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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagShort;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Material.AIR;

public class MyEnderman extends MyPet implements de.Keyle.MyPet.api.entity.types.MyEnderman {

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
            info.getCompoundData().put("Block", MyPetApi.getPlatformHelper().itemStackToCompund(block));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("BlockName")) {
            ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
            String id = info.getAs("BlockName", TagString.class).getStringData();
            MaterialHolder materialHolder = itemDatabase.getByID(id);
            if (materialHolder != null) {
                Material material = materialHolder.getMaterial();
                if (material != null) {
                    if (MyPetApi.getCompatUtil().isCompatible("1.13")) {
                        setBlock(new ItemStack(material, 1));
                    } else {
                        short data = materialHolder.getLegacyId().getData();
                        setBlock(new ItemStack(material, 1, data));
                    }
                    setBlock(new ItemStack(material, 1));
                }
            }
        } else if (info.containsKey("BlockID")) {
            int id;
            byte data = 0;
            if (info.containsKeyAs("BlockID", TagShort.class)) {
                id = info.getAs("BlockID", TagShort.class).getShortData();
            } else {
                id = info.getAs("BlockID", TagInt.class).getIntData();
            }
            if (info.containsKeyAs("BlockData", TagShort.class)) {
                data = (byte) info.getAs("BlockData", TagShort.class).getShortData();
            } else if (info.containsKeyAs("BlockData", TagInt.class)) {
                data = (byte) info.getAs("BlockData", TagInt.class).getIntData();
            }

            ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
            MaterialHolder materialHolder = itemDatabase.getByLegacyId(id, data);
            if (materialHolder != null) {
                Material material = materialHolder.getMaterial();
                if (MyPetApi.getCompatUtil().isCompatible("1.13")) {
                    setBlock(new ItemStack(material, 1));
                } else {
                    setBlock(new ItemStack(material, 1, data));
                }
            }
        } else if (info.containsKey("Block")) {
            TagCompound itemStackCompund = info.getAs("Block", TagCompound.class);
            try {
                ItemStack block = MyPetApi.getPlatformHelper().compundToItemStack(itemStackCompund);
                setBlock(block);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Block item from pet data!");
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Enderman;
    }

    public boolean isScreaming() {
        return isScreaming;
    }

    public void setScreaming(boolean flag) {
        this.isScreaming = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean hasBlock() {
        return block != null;
    }

    public void setBlock(ItemStack block) {
        if (block != null) {
            this.block = block.clone();
            this.block.setAmount(1);

            if (status == PetState.Here) {
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
            }
        } else {
            this.block = null;
            if (status == PetState.Here) {
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
            }
        }
    }

    @Override
    public String toString() {
        return "MyEnderman{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ",Block=" + block + "}";
    }
}
