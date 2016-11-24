/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagList;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyZombie extends MyPet implements de.Keyle.MyPet.api.entity.types.MyZombie {
    private enum Type {
        NORMAL, HUSK, VILLAGER
    }

    protected boolean isBaby = false;
    protected Type type = Type.NORMAL;
    protected int profession = 0;
    protected Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

    public MyZombie(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public ItemStack[] getEquipment() {
        ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
        for (int i = 0; i < EquipmentSlot.values().length; i++) {
            equipment[i] = getEquipment(EquipmentSlot.getSlotById(i));
        }
        return equipment;
    }

    public ItemStack getEquipment(EquipmentSlot slot) {
        return equipment.get(slot);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        info.getCompoundData().put("Type", new TagInt(type.ordinal()));

        List<TagCompound> itemList = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (getEquipment(slot) != null) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getEquipment(slot));
                item.getCompoundData().put("Slot", new TagInt(slot.getSlotId()));
                itemList.add(item);
            }
        }
        info.getCompoundData().put("Equipment", new TagList(itemList));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Villager")) {
            setVillager(info.getAs("Villager", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Husk")) {
            setHusk(info.getAs("Husk", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Profession")) {
            setProfession(info.getAs("Profession", TagInt.class).getIntData());
        }
        if (info.getCompoundData().containsKey("Type")) {
            int type = info.getAs("Type", TagInt.class).getIntData();
            if (!info.getCompoundData().containsKey("Version")) {
                if (type == 6) {
                    setHusk(true);
                }
            } else if (type > 0 && type < 6) {
                setVillager(true);
                setProfession(type);

            }
            setType(type);
        }
        if (info.getCompoundData().containsKey("Equipment")) {
            TagList equipment = info.get("Equipment");
            for (int i = 0; i < equipment.size(); i++) {
                TagCompound item = equipment.getTag(i);

                ItemStack itemStack = MyPetApi.getPlatformHelper().compundToItemStack(item);
                setEquipment(EquipmentSlot.getSlotById(item.getAs("Slot", TagInt.class).getIntData()), itemStack);
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Zombie;
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        this.isBaby = flag;
        if (status == PetState.Here) {
            getEntity().get().getHandle().updateVisuals();
        }
    }

    @Override
    public boolean isHusk() {
        return type == Type.HUSK;
    }

    @Override
    public void setHusk(boolean flag) {
        type = Type.HUSK;
        if (status == PetState.Here) {
            getEntity().get().getHandle().updateVisuals();
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.11") >= 0) {
                removePet();
                createEntity();
            }
        }
    }

    public boolean isVillager() {
        return type == Type.VILLAGER;
    }

    public void setVillager(boolean flag) {
        type = Type.VILLAGER;
        if (status == PetState.Here) {
            getEntity().get().getHandle().updateVisuals();
        }
    }

    public void setProfession(int type) {
        this.profession = type + 1;
        if (status == PetState.Here) {
            getEntity().get().getHandle().updateVisuals();
            if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.11") >= 0) {
                removePet();
                createEntity();
            }
        }
    }

    @Override
    public int getProfession() {
        return profession - 1;
    }

    @Override
    @Deprecated
    public int getType() {
        return type.ordinal();
    }

    @Override
    public void setType(int type) {
        if (Util.isBetween(0, 2, type)) {
            this.type = Type.values()[type];
            if (status == PetState.Here) {
                getEntity().get().getHandle().updateVisuals();
            }
        }
    }

    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (item == null) {
            equipment.remove(slot);
            getEntity().get().getHandle().updateVisuals();
            return;
        }

        item = item.clone();
        item.setAmount(1);
        equipment.put(slot, item);
        if (status == PetState.Here) {
            getEntity().get().getHandle().updateVisuals();
        }
    }

    @Override
    public void dropEquipment() {
        if (getStatus() == PetState.Here) {
            Location dropLocation = getLocation().get();
            for (ItemStack itemStack : equipment.values()) {
                if (itemStack != null) {
                    dropLocation.getWorld().dropItem(dropLocation, itemStack);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "MyZombie{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", villager=" + isVillager() + ", baby=" + isBaby() + "}";
    }
}