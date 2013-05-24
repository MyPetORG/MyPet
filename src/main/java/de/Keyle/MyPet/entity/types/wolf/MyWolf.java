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

package de.Keyle.MyPet.entity.types.wolf;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.Tamed;
import static org.bukkit.Material.RAW_BEEF;
import static org.bukkit.Material.RAW_CHICKEN;

@MyPetInfo(food = {RAW_BEEF, RAW_CHICKEN}, leashFlags = {Tamed})
public class MyWolf extends MyPet implements IMyPetBaby
{
    protected boolean isSitting = false;
    protected boolean isBaby = false;
    protected boolean isTamed = false;
    protected boolean isAngry = false;
    protected DyeColor collarColor = DyeColor.RED;

    public MyWolf(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Wolf";
    }

    public boolean isSitting()
    {
        return isSitting;
    }

    public void setSitting(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyWolf) getCraftPet().getHandle()).setSitting(flag);
        }
        this.isSitting = flag;
    }

    public DyeColor getCollarColor()
    {
        return collarColor;
    }

    public void setCollarColor(DyeColor value)
    {
        if (status == PetState.Here)
        {
            ((EntityMyWolf) getCraftPet().getHandle()).setCollarColor(value.getDyeData());
        }
        this.collarColor = value;
    }

    public boolean isTamed()
    {
        return isTamed;
    }

    public void setTamed(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyWolf) getCraftPet().getHandle()).setTamed(flag);
        }
        this.isTamed = flag;
    }

    public boolean isAngry()
    {
        return isAngry;
    }

    public void setAngry(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyWolf) getCraftPet().getHandle()).setAngry(flag);
        }
        this.isAngry = flag;
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyWolf) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Sitting", new ByteTag("Sitting", isSitting()));
        info.getValue().put("Baby", new ByteTag("Baby", isBaby()));
        info.getValue().put("Tamed", new ByteTag("Tamed", isTamed()));
        info.getValue().put("Angry", new ByteTag("Angry", isAngry()));
        info.getValue().put("CollarColor", new ByteTag("CollarColor", getCollarColor().getDyeData()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Sitting"))
        {
            setSitting(((ByteTag) info.getValue().get("Sitting")).getBooleanValue());
        }
        if (info.getValue().containsKey("CollarColor"))
        {
            setCollarColor(DyeColor.getByDyeData(((ByteTag) info.getValue().get("CollarColor")).getValue()));
        }
        if (info.getValue().containsKey("Tamed"))
        {
            setTamed(((ByteTag) info.getValue().get("Tamed")).getBooleanValue());
        }
        if (info.getValue().containsKey("Baby"))
        {
            setBaby(((ByteTag) info.getValue().get("Baby")).getBooleanValue());
        }
        if (info.getValue().containsKey("Angry"))
        {
            setAngry(((ByteTag) info.getValue().get("Angry")).getBooleanValue());
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
        return "MyWolf{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", sitting=" + isSitting() + ", collarcolor=" + getCollarColor() + ", baby=" + isBaby() + "}";
    }
}