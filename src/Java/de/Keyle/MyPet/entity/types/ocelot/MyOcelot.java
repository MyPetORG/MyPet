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

package de.Keyle.MyPet.entity.types.ocelot;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.entity.Ocelot.Type;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.Tamed;
import static org.bukkit.Material.RAW_FISH;

@MyPetInfo(food = {RAW_FISH}, leashFlags = {Tamed})
public class MyOcelot extends MyPet
{
    protected boolean isSitting = false;
    protected boolean isBaby = false;
    protected Type catType = Type.WILD_OCELOT;

    public MyOcelot(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Ocelot";
    }

    public boolean isSitting()
    {
        return isSitting;
    }

    public void setSitting(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyOcelot) getCraftPet().getHandle()).setSitting(flag);
        }
        this.isSitting = flag;
    }

    public Type getCatType()
    {
        return catType;
    }

    public void setCatType(Type value)
    {
        if (status == PetState.Here)
        {
            ((EntityMyOcelot) getCraftPet().getHandle()).setCatType(value.getId());
        }
        this.catType = value;
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyOcelot) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("CatType", new IntTag("CatType", getCatType().getId()));
        info.getValue().put("Sitting", new ByteTag("Sitting", isSitting()));
        info.getValue().put("Baby", new ByteTag("Baby", isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("CatType"))
        {
            setCatType(Type.getType(((IntTag) info.getValue().get("CatType")).getValue()));
        }
        if (info.getValue().containsKey("Sitting"))
        {
            setSitting(((ByteTag) info.getValue().get("Sitting")).getBooleanValue());
        }
        if (info.getValue().containsKey("Baby"))
        {
            setBaby(((ByteTag) info.getValue().get("Baby")).getBooleanValue());
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Ocelot;
    }

    @Override
    public String toString()
    {
        return "MyOcelot{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", sitting=" + isSitting() + ", cattype=" + getCatType().name() + ", baby=" + isBaby() + "}";
    }
}