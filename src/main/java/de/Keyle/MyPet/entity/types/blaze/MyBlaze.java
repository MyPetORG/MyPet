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

package de.Keyle.MyPet.entity.types.blaze;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.ChatColor;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;

import static org.bukkit.Material.SULPHUR;

@MyPetInfo(food = {SULPHUR})
public class MyBlaze extends MyPet
{
    protected boolean isOnFire = false;

    public MyBlaze(MyPetPlayer petOwner)
    {
        super(petOwner);
    }

    public boolean isOnFire()
    {
        return isOnFire;
    }

    public void setOnFire(boolean flag)
    {
        if (status == PetState.Here)
        {
            ((EntityMyBlaze) getCraftPet().getHandle()).setOnFire(flag);
        }
        isOnFire = flag;
    }

    @Override
    public CompoundTag getExtendedInfo()
    {
        CompoundTag info = super.getExtendedInfo();
        info.getValue().put("Fire", new ByteTag("Fire", isOnFire()));
        return info;
    }

    @Override
    public void setExtendedInfo(CompoundTag info)
    {
        if (info.getValue().containsKey("Fire"))
        {
            setOnFire(((ByteTag) info.getValue().get("Fire")).getBooleanValue());
        }
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.Blaze;
    }

    @Override
    public String toString()
    {
        return "MyBlaze{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", isOnFire=" + isOnFire + "}";
    }
}