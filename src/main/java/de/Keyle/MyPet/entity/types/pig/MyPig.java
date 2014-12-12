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

package de.Keyle.MyPet.entity.types.pig;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.ConfigItem;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Material.CARROT_ITEM;

@MyPetInfo(food = {CARROT_ITEM})
public class MyPig extends MyPet implements IMyPetBaby {
    public static ConfigItem GROW_UP_ITEM;

    protected boolean isBaby = false;
    public ItemStack saddle = null;

    public MyPig(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        if (hasSaddle()) {
            info.getCompoundData().put("Saddle", ItemStackNBTConverter.itemStackToCompund(CraftItemStack.asNMSCopy(getSaddle())));
        }
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.containsKeyAs("Saddle", TagByte.class)) {
            boolean saddle = info.getAs("Saddle", TagByte.class).getBooleanData();
            if (saddle) {
                ItemStack item = new ItemStack(Material.SADDLE);
                setSaddle(item);
            }
        } else if (info.containsKeyAs("Saddle", TagCompound.class)) {
            TagCompound itemTag = info.get("Saddle");
            ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compundToItemStack(itemTag));
            setSaddle(item);
        }
        if (info.getCompoundData().containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Pig;
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyPig) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public ItemStack getSaddle() {
        return saddle;
    }

    public boolean hasSaddle() {
        return saddle != null;
    }

    public void setSaddle(ItemStack item) {
        if (item != null && item.getType() != Material.SADDLE) {
            return;
        }

        this.saddle = item;
        if (this.saddle != null) {
            this.saddle.setAmount(1);
        }

        if (status == PetState.Here) {
            ((EntityMyPig) getCraftPet().getHandle()).setSaddle(this.saddle != null);
        }
    }

    @Override
    public String toString() {
        return "MyPig{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", saddle=" + hasSaddle() + ", baby=" + isBaby() + "}";
    }
}