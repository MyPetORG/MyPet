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

package de.Keyle.MyPet.entity.types.horse;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.TagType;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.Tamed;
import static org.bukkit.Material.*;

@MyPetInfo(food = {SUGAR, WHEAT, APPLE}, leashFlags = {Tamed})
public class MyHorse extends MyPet implements IMyPetBaby {
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
    public CompoundTag getExtendedInfo() {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Type", new ByteTag("Type", getHorseType()));
        info.getValue().put("Variant", new IntTag("Variant", getVariant()));
        if (hasArmor()) {
            info.getValue().put("Armor", ItemStackNBTConverter.ItemStackToCompund(CraftItemStack.asNMSCopy(getArmor()), "Armor"));
        }
        info.getValue().put("Age", new IntTag("Age", getAge()));
        if (hasChest()) {
            info.getValue().put("Chest", ItemStackNBTConverter.ItemStackToCompund(CraftItemStack.asNMSCopy(getChest()), "Chest"));
        }
        if (hasSaddle()) {
            info.getValue().put("Saddle", ItemStackNBTConverter.ItemStackToCompund(CraftItemStack.asNMSCopy(getSaddle()), "Saddle"));
        }
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info) {
        if (info.getValue().containsKey("Type")) {
            setHorseType(((ByteTag) info.getValue().get("Type")).getValue());
        }
        if (info.getValue().containsKey("Variant")) {
            setVariant(((IntTag) info.getValue().get("Variant")).getValue());
        }
        if (info.getValue().containsKey("Armor")) {
            if (info.getValue().get("Armor").getType() == TagType.TAG_INT) {
                int armorType = ((IntTag) info.getValue().get("Armor")).getValue();
                if (armorType != 0) {
                    ItemStack item = new ItemStack(Material.getMaterial(416 + armorType));
                    setArmor(item);
                }
            } else {
                CompoundTag itemTag = ((CompoundTag) info.getValue().get("Armor"));
                ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.CompundToItemStack(itemTag));
                setArmor(item);
            }
        }
        if (info.getValue().containsKey("Age")) {
            setAge(((IntTag) info.getValue().get("Age")).getValue());
        }
        if (info.getValue().containsKey("Chest")) {
            if (info.getValue().get("Chest").getType() == TagType.TAG_BYTE) {
                boolean chest = ((ByteTag) info.getValue().get("Chest")).getBooleanValue();
                if (chest) {
                    ItemStack item = new ItemStack(Material.CHEST);
                    setChest(item);
                }
            } else {
                CompoundTag itemTag = ((CompoundTag) info.getValue().get("Chest"));
                ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.CompundToItemStack(itemTag));
                setChest(item);
            }
        }
        if (info.getValue().containsKey("Saddle")) {
            if (info.getValue().get("Saddle").getType() == TagType.TAG_BYTE) {
                boolean saddle = ((ByteTag) info.getValue().get("Saddle")).getBooleanValue();
                if (saddle) {
                    ItemStack item = new ItemStack(Material.SADDLE);
                    setSaddle(item);
                }
            } else {
                CompoundTag itemTag = ((CompoundTag) info.getValue().get("Saddle"));
                ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.CompundToItemStack(itemTag));
                setSaddle(item);
            }
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