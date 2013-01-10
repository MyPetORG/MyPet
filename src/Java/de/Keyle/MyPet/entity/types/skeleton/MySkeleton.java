/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.skeleton;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.v1_4_6.ItemStack;
import net.minecraft.server.v1_4_6.NBTTagCompound;

public class MySkeleton extends MyPet
{
    protected boolean isWither = false;

    public MySkeleton(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Skeleton";
    }

    protected void setEquipment(int slot, ItemStack item)
    {
        //this.equipment = equipment;
        if (status == PetState.Here)
        {
            getCraftPet().getHandle().setEquipment(slot, item);
        }
    }

    protected ItemStack[] getEquipment()
    {
        if (status == PetState.Here)
        {
            return getCraftPet().getHandle().getEquipment();
        }
        return new ItemStack[0];
    }

    public void setWither(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMySkeleton) getCraftPet().getHandle()).setWither(flag);
        }
        this.isWither = flag;
    }

    public boolean isWither()
    {
        return isWither;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setBoolean("Wither", isWither());
        /*
        NBTTagList items = new NBTTagList();
        ItemStack[] equipment = getEquipment();
        for (int i = 0 ; i < equipment.length ; i++)
        {
            ItemStack itemStack = equipment[i];
            if (itemStack != null)
            {
                NBTTagCompound item = new NBTTagCompound();
                item.setInt("Slot", i);
                itemStack.save(item);
                items.add(item);
            }
        }
        info.set("Items", items);
        */
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        if (info.hasKey("Wither"))
        {
            setWither(info.getBoolean("Wither"));
        }
        //TODO load equipment
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Skeleton;
    }

    @Override
    public String toString()
    {
        return "MySkeleton{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + "}";
    }
}