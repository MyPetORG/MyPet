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

package de.Keyle.MyPet.entity.types.blaze;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.MyPetPlayer;

public class MyBlaze extends MyPet
{
    protected boolean isOnFire = false;

    public MyBlaze(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Blaze";
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
    public MyPetType getPetType()
    {
        return MyPetType.Blaze;
    }

    @Override
    public String toString()
    {
        return "MyBlaze{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", isOnFire=" + isOnFire + "}";
    }
}