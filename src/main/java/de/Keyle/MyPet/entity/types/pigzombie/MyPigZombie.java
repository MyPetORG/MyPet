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

package de.Keyle.MyPet.entity.types.pigzombie;

import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetEquipment;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_6_R2.ItemStack;
import org.bukkit.ChatColor;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.ListTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Material.ROTTEN_FLESH;

@MyPetInfo(food = {ROTTEN_FLESH})
public class MyPigZombie extends MyPet implements IMyPetEquipment
{
    protected boolean isBaby = false;
    protected Map<EquipmentSlot, ItemStack> equipment = new HashMap<EquipmentSlot, ItemStack>();

    public MyPigZombie(MyPetPlayer petOwner)
    {
        super(petOwner);
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyPigZombie) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public void setEquipment(EquipmentSlot slot, ItemStack item)
    {
        item = item.cloneItemStack();
        equipment.put(slot, item);
        if (status == PetState.Here)
        {
            ((EntityMyPigZombie) getCraftPet().getHandle()).setPetEquipment(slot.getSlotId(), item);
        }
    }

    public ItemStack[] getEquipment()
    {
        ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
        for (int i = 0 ; i < EquipmentSlot.values().length ; i++)
        {
            equipment[i] = getEquipment(EquipmentSlot.getSlotById(i));
        }
        return equipment;
    }

    public ItemStack getEquipment(EquipmentSlot slot)
    {
        return equipment.get(slot);
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Baby", new ByteTag("Baby", isBaby()));

        List<CompoundTag> itemList = new ArrayList<CompoundTag>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (getEquipment(slot) != null)
            {
                CompoundTag item = ItemStackNBTConverter.ItemStackToCompund(getEquipment(slot));
                item.getValue().put("Slot", new IntTag("Slot", slot.getSlotId()));
                itemList.add(item);
            }
        }
        info.getValue().put("Equipment", new ListTag<CompoundTag>("Equipment", CompoundTag.class, itemList));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Baby"))
        {
            setBaby(((ByteTag) info.getValue().get("Baby")).getBooleanValue());
        }
        if (info.getValue().containsKey("Equipment"))
        {
            ListTag equipment = (ListTag) info.getValue().get("Equipment");
            for (int i = 0 ; i < equipment.getValue().size() ; i++)
            {
                CompoundTag item = (CompoundTag) equipment.getValue().get(i);

                ItemStack itemStack = ItemStackNBTConverter.CompundToItemStack(item);
                setEquipment(EquipmentSlot.getSlotById(((IntTag) item.getValue().get("Slot")).getValue()), itemStack);
            }
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.PigZombie;
    }

    @Override
    public String toString()
    {
        return "MyPigZombie{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + "}";
    }
}