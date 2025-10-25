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
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MyHorse extends MyPet implements de.Keyle.MyPet.api.entity.types.MyHorse {
    public boolean baby = false;
    protected byte horseType = 0;
    protected int variant = 0;
    public ItemStack armor = null;
    public ItemStack chest = null;
    public ItemStack saddle = null;

    public MyHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public int getAge() {
        return baby ? -24000 : 0;
    }

    public void setAge(int value) {
        if (value < 0) {
            setBaby(true);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public ItemStack getArmor() {
        return armor;
    }

    public boolean hasArmor() {
        return armor != null;
    }

    public void setArmor(ItemStack item) {
        if (item != null &&
                !item.getType().name().equals("LEATHER_HORSE_ARMOR") &&
                item.getType() != EnumSelector.find(Material.class, "IRON_BARDING", "IRON_HORSE_ARMOR") &&
                item.getType() != EnumSelector.find(Material.class, "GOLD_BARDING", "GOLDEN_HORSE_ARMOR") &&
                item.getType() != EnumSelector.find(Material.class, "DIAMOND_BARDING", "DIAMOND_HORSE_ARMOR")) {
            return;
        }

        this.armor = item;
        if (this.armor != null) {
            this.armor.setAmount(1);
        }

        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
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
        this.chest = item;
        if (this.chest != null) {
            this.chest.setAmount(1);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
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
        this.saddle = item;
        if (this.saddle != null) {
            this.saddle.setAmount(1);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Type", new TagByte(getHorseType()));
        info.getCompoundData().put("Variant", new TagInt(getVariant()));
        if (hasArmor()) {
            info.getCompoundData().put("Armor", MyPetApi.getPlatformHelper().itemStackToCompund(getArmor()));
        }
        info.getCompoundData().put("Age", new TagInt(getAge()));
        if (hasChest()) {
            info.getCompoundData().put("Chest", MyPetApi.getPlatformHelper().itemStackToCompund(getChest()));
        }
        if (hasSaddle()) {
            info.getCompoundData().put("Saddle", MyPetApi.getPlatformHelper().itemStackToCompund(getSaddle()));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("Type")) {
            setHorseType(info.getAs("Type", TagByte.class).getByteData());
        }
        if (info.containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Variant")) {
            setVariant(info.getAs("Variant", TagInt.class).getIntData());
        }
        if (info.containsKeyAs("Armor", TagInt.class)) {
            int armorType = info.getAs("Armor", TagInt.class).getIntData();
            if (armorType != 0) {
                //ItemStack item = new ItemStack(Material.getMaterial(416 + armorType));
                //setArmor(item);
            }
        } else if (info.containsKeyAs("Armor", TagCompound.class)) {
            TagCompound itemTag = info.get("Armor");
            try {
                ItemStack item = MyPetApi.getPlatformHelper().compundToItemStack(itemTag);
                setArmor(item);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Armor item from pet data!");
            }
        }
        if (info.containsKey("Age")) {
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
            try {
                ItemStack item = MyPetApi.getPlatformHelper().compundToItemStack(itemTag);
                setChest(item);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Chest item from pet data!");
            }
        }
        if (info.containsKeyAs("Saddle", TagByte.class)) {
            boolean saddle = info.getAs("Saddle", TagByte.class).getBooleanData();
            if (saddle) {
                ItemStack item = new ItemStack(Material.SADDLE);
                setSaddle(item);
            }
        } else if (info.containsKeyAs("Saddle", TagCompound.class)) {
            TagCompound itemTag = info.get("Saddle");
            try {
                ItemStack item = MyPetApi.getPlatformHelper().compundToItemStack(itemTag);
                setSaddle(item);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Saddle item from pet data!");
            }
        }
    }

    public byte getHorseType() {
        return horseType;
    }

    public void setHorseType(byte horseType) {
        horseType = (byte) Math.min(Math.max(0, horseType), 4);
        this.horseType = horseType;

        if (horseType != 0) {
            setVariant(0);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
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
        return "MyHorse{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", type=" + horseType + ", variant=" + variant + ", armor=" + armor + ", saddle=" + saddle + ", chest=" + chest + "}";
    }
}