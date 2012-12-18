/*
 * Copyright (C) 2011-2012 Keyle
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

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.DyeColor;

public class MyWolf extends MyPet
{
    protected boolean isSitting = false;
    protected boolean isBaby = false;
    protected boolean isTamed = true;
    protected boolean isAngry = false;
    protected DyeColor collarColor = DyeColor.RED;

    public MyWolf(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Wolf";
    }

    public boolean isSitting()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyWolf) getCraftPet()).isSitting();
        }
        else
        {
            return isSitting;
        }
    }

    public void setSitting(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyWolf) getCraftPet()).setSitting(flag);
        }
        this.isSitting = flag;
    }

    public DyeColor getCollarColor()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyWolf) getCraftPet()).getCollarColor();
        }
        else
        {
            return collarColor;
        }
    }

    public void setCollarColor(DyeColor value)
    {
        if (status == PetState.Here)
        {
            ((CraftMyWolf) getCraftPet()).setCollarColor(value);
        }
        this.collarColor = value;
    }

    public boolean isTamed()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyWolf) getCraftPet()).isTamed();
        }
        else
        {
            return isTamed;
        }
    }

    public void setTamed(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyWolf) getCraftPet()).setTamed(flag);
        }
        this.isTamed = flag;
    }

    public boolean isAngry()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyWolf) getCraftPet()).isAngry();
        }
        else
        {
            return isAngry;
        }
    }

    public void setAngry(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyWolf) getCraftPet()).setAngry(flag);
        }
        this.isAngry = flag;
    }

    public boolean isBaby()
    {
        if (status == PetState.Here)
        {
            return ((CraftMyWolf) getCraftPet()).isBaby();
        }
        else
        {
            return isBaby;
        }
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((CraftMyWolf) getCraftPet()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public NBTTagCompound getExtendedInfo()
    {
        NBTTagCompound info = new NBTTagCompound("Info");
        info.setBoolean("Sitting", isSitting());
        info.setBoolean("Baby", isBaby());
        info.setBoolean("Tamed", isTamed());
        info.setBoolean("Angry", isAngry());
        info.setByte("CollarColor", getCollarColor().getData());
        return info;
    }

    @Override
    public void setExtendedInfo(NBTTagCompound info)
    {
        if (info.hasKey("Sitting"))
        {
            setSitting(info.getBoolean("Sitting"));
        }
        if (info.hasKey("CollarColor"))
        {
            setCollarColor(DyeColor.getByData(info.getByte("CollarColor")));
        }
        if (info.hasKey("Tamed"))
        {
            setTamed(info.getBoolean("Tamed"));
        }
        if (info.hasKey("Baby"))
        {
            setBaby(info.getBoolean("Baby"));
        }
        if (info.hasKey("Angry"))
        {
            setAngry(info.getBoolean("Angry"));
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Wolf;
    }

    @Override
    public String toString()
    {
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", sitting=" + isSitting() + ", collarcolor=" + getCollarColor() + ", baby=" + isBaby() + "}";
    }
}