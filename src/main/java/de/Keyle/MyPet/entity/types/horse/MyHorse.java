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
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;

import static org.bukkit.Material.*;

@MyPetInfo(food = {SUGAR, WHEAT, APPLE})
public class MyHorse extends MyPet
{
    protected byte horseType = 0;
    protected int variant = 0;
    public boolean isBaby = false;

    public MyHorse(MyPetPlayer petOwner)
    {
        super(petOwner);
    }

    public byte getHorseType()
    {
        return horseType;
    }

    public void setHorseType(byte horseType)
    {
        this.horseType = horseType;
        if (status == PetState.Here)
        {
            ((EntityMyHorse) getCraftPet().getHandle()).setHorseType(horseType);
        }

        if (horseType != 0)
        {
            setVariant(0);
        }
    }

    public int getVariant()
    {
        return variant;
    }

    public void setVariant(int variant)
    {
        if (horseType != 0)
        {
            this.variant = 0;
        }
        else if (variant >= 0 && variant <= 6)
        {
            this.variant = variant;
        }
        else if (variant >= 256 && variant <= 262)
        {
            this.variant = variant;
        }
        else if (variant >= 512 && variant <= 518)
        {
            this.variant = variant;
        }
        else if (variant >= 768 && variant <= 774)
        {
            this.variant = variant;
        }
        else if (variant >= 1024 && variant <= 1030)
        {
            this.variant = variant;
        }
        else
        {
            this.variant = 0;
        }
        if (status == PetState.Here)
        {
            ((EntityMyHorse) getCraftPet().getHandle()).setVariant(this.variant);
        }
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        this.isBaby = flag;
        if (status == PetState.Here)
        {
            ((EntityMyHorse) getCraftPet().getHandle()).setBaby(flag);
        }
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Type", new ByteTag("Type", getHorseType()));
        info.getValue().put("Variant", new IntTag("Variant", getVariant()));
        info.getValue().put("Baby", new ByteTag("Baby", isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Type"))
        {
            setHorseType(((ByteTag) info.getValue().get("Type")).getValue());
        }
        if (info.getValue().containsKey("Variant"))
        {
            setVariant(((IntTag) info.getValue().get("Variant")).getValue());
        }
        if (info.getValue().containsKey("Baby"))
        {
            setBaby(((ByteTag) info.getValue().get("Baby")).getBooleanValue());
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Horse;
    }

    @Override
    public String toString()
    {
        return "MyHorse{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", type=" + horseType + ", variant=" + variant + "}";
    }
}