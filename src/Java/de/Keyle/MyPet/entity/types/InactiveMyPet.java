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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.skill.skills.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetPlayer;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.Location;

import java.util.Collection;

public class InactiveMyPet
{
    private String petName = "";
    private final MyPetPlayer petOwner;
    private int health = -1;
    private int hunger = 100;
    private int respawnTime = 0;
    private Location location;
    private double exp = 0;
    private MyPetType petType = MyPetType.Wolf;

    private NBTTagCompound NBTSkills = new NBTTagCompound("Skills");
    private NBTTagCompound NBTextendetInfo;

    public InactiveMyPet(MyPetPlayer petOwner)
    {
        this.petOwner = petOwner;
    }

    public void setSkills(Collection<MyPetGenericSkill> skills)
    {
        if (skills.size() > 0)
        {
            for (MyPetGenericSkill skill : skills)
            {
                NBTTagCompound s = skill.save();
                if (s != null)
                {
                    this.NBTSkills.set(skill.getName(), s);
                }
            }
        }
    }

    public void setSkills(NBTTagCompound skills)
    {
        NBTSkills = skills;
    }

    public void setInfo(NBTTagCompound info)
    {
        NBTextendetInfo = info;
    }

    public NBTTagCompound getInfo()
    {
        return NBTextendetInfo;
    }

    public NBTTagCompound getSkills()
    {
        return NBTSkills;
    }

    public void setPetType(MyPetType petType)
    {
        this.petType = petType;
        if (respawnTime <= 0 && health == -1)
        {
            this.health = MyPet.getStartHP(petType.getMyPetClass());
        }

    }

    public MyPetType getPetType()
    {
        return petType;
    }

    public int getHungerValue()
    {
        return hunger;
    }

    public void setHungerValue(int value)
    {
        if (value > 100)
        {
            hunger = 100;
        }
        else if (value < 1)
        {
            hunger = 1;
        }
        else
        {
            hunger = value;
        }
    }

    public void setHealth(int health)
    {
        this.health = health;
    }

    public int getHealth()
    {
        return health;
    }

    public void setExp(double Exp)
    {
        this.exp = Exp;
    }

    public double getExp()
    {
        return exp;
    }

    public void setPetName(String petName)
    {
        this.petName = petName;
    }

    public String getPetName()
    {
        return petName;
    }

    public void setRespawnTime(int respawnTime)
    {
        this.respawnTime = respawnTime;
    }

    public int getRespawnTime()
    {
        return respawnTime;
    }

    public Location getLocation()
    {
        return location;
    }

    public void setLocation(Location Location)
    {
        this.location = Location;
    }

    public MyPetPlayer getPetOwner()
    {
        return petOwner;
    }

    @Override
    public String toString()
    {
        return "InactiveMyPet{type=" + getPetType().getTypeName() + ", owner=" + getPetOwner().getName() + ", name=" + petName + ", exp=" + getExp() + ", health=" + getHealth() + "}";
    }
}