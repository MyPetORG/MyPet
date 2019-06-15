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
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MyLlama extends MyPet implements de.Keyle.MyPet.api.entity.types.MyLlama {

    public boolean baby = false;
    protected byte horseType = 0;
    protected int variant = 0;
    public ItemStack armor = null;
    public ItemStack chest = null;
    public ItemStack decor = null;

    public MyLlama(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public ItemStack getChest() {
        return chest;
    }

    public boolean hasChest() {
        return chest != null;
    }

    public void setChest(ItemStack item) {
        if (item != null && item.getType() != Material.CHEST && item.getType() != Material.TRAPPED_CHEST) {
            return;
        }
        this.chest = item;
        if (this.chest != null) {
            this.chest.setAmount(1);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public ItemStack getDecor() {
        return decor;
    }

    public boolean hasDecor() {
        return decor != null;
    }

    public void setDecor(ItemStack item) {
        if (item != null) {
            switch (item.getType().name()) {
                case "CARPET":
                case "RED_CARPET":
                case "BLACK_CARPET":
                case "CYAN_CARPET":
                case "BLUE_CARPET":
                case "BROWN_CARPET":
                case "GRAY_CARPET":
                case "GREEN_CARPET":
                case "LIME_CARPET":
                case "PINK_CARPET":
                case "ORANGE_CARPET":
                case "MAGENTA_CARPET":
                case "LIGHT_GRAY_CARPET":
                case "LIGHT_BLUE_CARPET":
                case "PURPLE_CARPET":
                case "WHITE_CARPET":
                case "YELLOW_CARPET":
                    break;
                default:
                    return;
            }
        }
        this.decor = item;
        if (this.decor != null) {
            this.decor.setAmount(1);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Variant", new TagInt(getVariant()));
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        if (hasChest()) {
            info.getCompoundData().put("Chest", MyPetApi.getPlatformHelper().itemStackToCompund(getChest()));
        }
        if (hasDecor()) {
            info.getCompoundData().put("Decor", MyPetApi.getPlatformHelper().itemStackToCompund(getDecor()));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Variant")) {
            setVariant(info.getAs("Variant", TagInt.class).getIntData());
        }
        if (info.containsKeyAs("Chest", TagCompound.class)) {
            TagCompound itemTag = info.get("Chest");
            try {
                ItemStack item = MyPetApi.getPlatformHelper().compundToItemStack(itemTag);
                setChest(item);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Chest item from pet data!");
            }
        }
        if (info.containsKeyAs("Decor", TagCompound.class)) {
            TagCompound itemTag = info.get("Decor");
            try {
                ItemStack item = MyPetApi.getPlatformHelper().compundToItemStack(itemTag);
                setDecor(item);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Decor item from pet data!");
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Llama;
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        if (horseType != 0) {
            this.variant = 0;
        } else if (variant >= 0 && variant <= 6) {
            this.variant = variant;
        } else if (variant >= 256 && variant <= 262) {
            this.variant = variant;
        } else if (variant >= 512 && variant <= 518) {
            this.variant = variant;
        } else if (variant >= 768 && variant <= 774) {
            this.variant = variant;
        } else if (variant >= 1024 && variant <= 1030) {
            this.variant = variant;
        } else {
            this.variant = 0;
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public boolean isBaby() {
        return baby;
    }

    public void setBaby(boolean flag) {
        this.baby = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public String toString() {
        return "MyLlama{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", type=" + horseType + ", variant=" + variant + ", armor=" + armor + ", decor=" + decor + ", chest=" + chest + "}";
    }
}