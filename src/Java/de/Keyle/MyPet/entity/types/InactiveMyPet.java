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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.skill.ISkillStorage;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.MyPetPlayer;
import org.bukkit.Location;
import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;

import java.util.Collection;
import java.util.UUID;

public class InactiveMyPet implements IMyPet
{
    private UUID uuid = null;
    private String petName = "";
    private final MyPetPlayer petOwner;
    private int health = -1;
    private int hunger = 100;
    private int respawnTime = 0;
    private Location location;
    private double exp = 0;
    private MyPetType petType = MyPetType.Wolf;
    private MyPetSkillTree skillTree = null;

    private CompoundTag NBTSkills;
    private CompoundTag NBTextendetInfo;

    public InactiveMyPet(MyPetPlayer petOwner)
    {
        this.petOwner = petOwner;
    }

    public void setSkills(Collection<ISkillInstance> skills)
    {
        if (NBTSkills == null)
        {
            NBTSkills = new CompoundTag("Skills", new CompoundMap());
        }
        for (ISkillInstance skill : skills)
        {
            if (skill instanceof ISkillStorage)
            {
                ISkillStorage storageSkill = (ISkillStorage) skill;
                CompoundTag s = storageSkill.save();
                if (s != null)
                {
                    this.NBTSkills.getValue().put(skill.getName(), s);
                }
            }
        }
    }

    public void setSkills(CompoundTag skills)
    {
        NBTSkills = skills;
    }

    public void setInfo(CompoundTag info)
    {
        NBTextendetInfo = info;
    }

    public CompoundTag getInfo()
    {
        if (NBTextendetInfo == null)
        {
            NBTextendetInfo = new CompoundTag("Info", new CompoundMap());
        }
        return NBTextendetInfo;
    }

    public CompoundTag getSkills()
    {
        if (NBTSkills == null)
        {
            NBTSkills = new CompoundTag("Skills", new CompoundMap());
        }
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

    public MyPetSkillTree getSkillTree()
    {
        return skillTree;
    }

    public void setSkillTree(MyPetSkillTree skillTree)
    {
        this.skillTree = skillTree;
    }

    public MyPetPlayer getOwner()
    {
        return petOwner;
    }

    @Override
    public String toString()
    {
        return "InactiveMyPet{type=" + getPetType().getTypeName() + ", owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + getExp() + ", health=" + getHealth() + "}";
    }

    public void setUUID(UUID uuid)
    {
        this.uuid = uuid;
    }

    public UUID getUUID()
    {
        if (this.uuid == null)
        {
            this.uuid = UUID.randomUUID();
        }

        return this.uuid;
    }

}