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

package de.Keyle.MyPet.entity.types.chicken;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;

import static org.bukkit.Material.SEEDS;

@MyPetInfo(food = {SEEDS})
public class MyChicken extends MyPet implements IMyPetBaby
{
    protected boolean isBaby = false;

    public MyChicken(MyPetPlayer petOwner)
    {
        super(petOwner);
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Baby", new ByteTag("Baby", isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Baby"))
        {
            setBaby(((ByteTag) info.getValue().get("Baby")).getBooleanValue());
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Chicken;
    }

    public boolean isBaby()
    {
        return isBaby;
    }

    public void setBaby(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyChicken) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    @Override
    public String toString()
    {
        return "MyChicken{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", baby=" + isBaby() + "}";
    }
}