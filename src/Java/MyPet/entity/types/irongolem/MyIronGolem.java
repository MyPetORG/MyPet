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

package Java.MyPet.entity.types.irongolem;

import Java.MyPet.util.MyPetPlayer;
import Java.MyPet.entity.types.MyPet;
import Java.MyPet.entity.types.MyPetType;

public class MyIronGolem extends MyPet
{
    public MyIronGolem(MyPetPlayer petOwner)
    {
        super(petOwner);
        this.petName = "Iron Golem";
    }

    @Override
    public MyPetType getPetType()
    {
        return MyPetType.IronGolem;
    }

    @Override
    public String toString()
    {
        return "MyIronGolem{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}