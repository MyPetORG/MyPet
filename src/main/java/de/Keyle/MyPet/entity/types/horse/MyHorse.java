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

package de.Keyle.MyPet.entity.types.horse;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.ConfigItem;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.Tamed;
import static org.bukkit.Material.*;

@MyPetInfo(food = {SUGAR, WHEAT, APPLE}, leashFlags = {Tamed})
public class MyHorse extends MyPet implements IMyPetBaby {
    public static ConfigItem GROW_UP_ITEM;

    public int age = 0;
    protected byte horseType = 0;
    protected int variant = 0;
    public ItemStack armor = null;
    public ItemStack chest = null;
    public ItemStack saddle = null;

    public MyHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int value) {
        value = Math.min(0, (Math.max(-24000, value)));
        value = value - (value % 1000);
        if (status == PetState.Here) {
            ((EntityMyHorse) getCraftPet().getHandle()).setAge(value);
        }
        this.age = value;
    }

    public ItemStack getArmor() {
        return armor;
    }

    public boolean hasArmor() {
        return armor != null;
    }

    public void setArmor(ItemStack item) {
        if (item != null && item.getType() != Material.IRON_BARDING && item.getType() != Material.GOLD_BARDING && item.getType() != Material.DIAMOND_BARDING) {
            return;
        }

        this.armor = item;
        if (this.armor != null) {
            this.armor.setAmount(1);
        }

        if (status == PetState.Here) {
            ((EntityMyHorse) getCraftPet().getHandle()).setArmor(hasArmor() ? getArmor().getTypeId() - 416 : 0);
        }
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
        if (status == PetState.Here) {
            ((EntityMyHorse) getCraftPet().getHandle()).setChest(item != null);
        }
        this.chest = item;
        if (this.chest != null) {
            this.chest.setAmount(1);
        }
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
        if (status == PetState.Here) {
            ((EntityMyHorse) getCraftPet().getHandle()).setSaddle(item != null);
        }
        this.saddle = item;
        if (this.saddle != null) {
            this.saddle.setAmount(1);
        }
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        info.getCompoundData().put("Type", new TagByte(getHorseType()));
        info.getCompoundData().put("Variant", new TagInt(getVariant()));
        if (hasArmor()) {
            info.getCompoundData().put("Armor", ItemStackNBTConverter.itemStackToCompund(CraftItemStack.asNMSCopy(getArmor())));
        }
        info.getCompoundData().put("Age", new TagInt(getAge()));
        if (hasChest()) {
            info.getCompoundData().put("Chest", ItemStackNBTConverter.itemStackToCompund(CraftItemStack.asNMSCopy(getChest())));
        }
        if (hasSaddle()) {
            info.getCompoundData().put("Saddle", ItemStackNBTConverter.itemStackToCompund(CraftItemStack.asNMSCopy(getSaddle())));
        }
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("Type")) {
            setHorseType(info.getAs("Type", TagByte.class).getByteData());
        }
        if (info.getCompoundData().containsKey("Variant")) {
            setVariant(info.getAs("Variant", TagInt.class).getIntData());
        }
        if (info.containsKeyAs("Armor", TagInt.class)) {
            int armorType = info.getAs("Armor", TagInt.class).getIntData();
            if (armorType != 0) {
                ItemStack item = new ItemStack(Material.getMaterial(416 + armorType));
                setArmor(item);
            }
        } else if (info.containsKeyAs("Armor", TagCompound.class)) {
            TagCompound itemTag = info.get("Armor");
            ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compundToItemStack(itemTag));
            setArmor(item);
        }
        if (info.getCompoundData().containsKey("Age")) {
            setAge(info.getAs("Age", TagInt.class).getIntData());
        }
        if (info.containsKeyAs("Chest", TagByte.class)) {
            boolean chest = info.getAs("Chest", TagByte.class).getBooleanData();
            if (chest) {
                ItemStack item = new ItemStack(Material.CHEST);
                setChest(item);
            }
        } else if (info.containsKeyAs("Chest", TagCompound.class)) {
            TagCompound itemTag = info.get("Chest");
            ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compundToItemStack(itemTag));
            setChest(item);
        }
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
    }

    public byte getHorseType() {
        return horseType;
    }

    public void setHorseType(byte horseType) {
        horseType = (byte) Math.min(Math.max(0, horseType), 4);
        this.horseType = horseType;
        if (status == PetState.Here) {
            ((EntityMyHorse) getCraftPet().getHandle()).setHorseType(horseType);
        }

        if (horseType != 0) {
            setVariant(0);
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Horse;
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
            ((EntityMyHorse) getCraftPet().getHandle()).setVariant(this.variant);
        }
    }

    @Override
    public boolean isBaby() {
        return age < 0;
    }

    public void setBaby(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyHorse) getCraftPet().getHandle()).setBaby(flag);
        }
        if (flag) {
            this.age = -24000;
        } else {
            this.age = 0;
        }
    }

    @Override
    public String toString() {
        return "MyHorse{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", type=" + horseType + ", variant=" + variant + ", armor=" + armor + ", saddle=" + saddle + ", chest=" + chest + "}";
    }
}