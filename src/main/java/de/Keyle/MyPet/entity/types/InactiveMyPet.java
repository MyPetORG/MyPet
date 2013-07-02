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
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.NBTStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.spout.nbt.*;

import java.util.Collection;
import java.util.UUID;

public class InactiveMyPet implements IMyPet, NBTStorage
{
    private UUID uuid = null;
    private String petName = "";
    private String worldGroup = "";
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

    @Override
    public String getWorldGroup()
    {
        return worldGroup;
    }

    public void setWorldGroup(String worldGroup)
    {
        if (worldGroup != null)
        {
            this.worldGroup = worldGroup;
        }
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

    @Override
    public CompoundTag save()
    {
        CompoundTag petNBT = new CompoundTag(null, new CompoundMap());

        CompoundTag locationNBT = new CompoundTag("Location", new CompoundMap());
        locationNBT.getValue().put("X", new DoubleTag("X", this.location.getX()));
        locationNBT.getValue().put("Y", new DoubleTag("Y", this.location.getY()));
        locationNBT.getValue().put("Z", new DoubleTag("Z", this.location.getY()));
        locationNBT.getValue().put("Yaw", new FloatTag("Yaw", this.location.getYaw()));
        locationNBT.getValue().put("Pitch", new FloatTag("Pitch", this.location.getPitch()));
        locationNBT.getValue().put("World", new StringTag("World", this.location.getWorld().getName()));

        petNBT.getValue().put("UUID", new StringTag("UUID", getUUID().toString()));
        petNBT.getValue().put("Type", new StringTag("Type", this.petType.getTypeName()));
        petNBT.getValue().put("Owner", new StringTag("Owner", this.petOwner.getName()));
        petNBT.getValue().put("Location", locationNBT);
        petNBT.getValue().put("Health", new IntTag("Health", this.health));
        petNBT.getValue().put("Respawntime", new IntTag("Respawntime", this.respawnTime));
        petNBT.getValue().put("Hunger", new IntTag("Hunger", this.hunger));
        petNBT.getValue().put("Name", new StringTag("Name", this.petName));
        petNBT.getValue().put("WorldGroup", new StringTag("WorldGroup", this.worldGroup));
        petNBT.getValue().put("Exp", new DoubleTag("Exp", this.exp));
        petNBT.getValue().put("Info", getInfo());
        if (this.skillTree != null)
        {
            petNBT.getValue().put("Skilltree", new StringTag("Skilltree", skillTree.getName()));
        }
        petNBT.getValue().put("Skills", getSkills());

        return petNBT;
    }

    @Override
    public void load(CompoundTag myPetNBT)
    {
        if (myPetNBT.getValue().containsKey("UUID"))
        {
            uuid = UUID.fromString(((StringTag) myPetNBT.getValue().get("UUID")).getValue());
        }

        CompoundTag locationNBT = (CompoundTag) myPetNBT.getValue().get("Location");
        double petX = ((DoubleTag) locationNBT.getValue().get("X")).getValue();
        double petY = ((DoubleTag) locationNBT.getValue().get("Y")).getValue();
        double petZ = ((DoubleTag) locationNBT.getValue().get("Z")).getValue();
        float petYaw = 1F;
        if (locationNBT.getValue().containsKey("Yaw"))
        {
            petYaw = ((FloatTag) locationNBT.getValue().get("Yaw")).getValue();
        }
        float petPitch = 1F;
        if (locationNBT.getValue().containsKey("Pitch"))
        {
            petPitch = ((FloatTag) locationNBT.getValue().get("Pitch")).getValue();
        }
        String petWorld = ((StringTag) locationNBT.getValue().get("World")).getValue();
        if (Bukkit.getServer().getWorld(petWorld) != null)
        {
            this.location = new Location(Bukkit.getServer().getWorld(petWorld), petX, petY, petZ, petYaw, petPitch);
        }
        else
        {
            this.location = new Location(Bukkit.getServer().getWorlds().get(0), petX, petY, petZ, petYaw, petPitch);
        }

        exp = ((DoubleTag) myPetNBT.getValue().get("Exp")).getValue();
        health = ((IntTag) myPetNBT.getValue().get("Health")).getValue();
        respawnTime = ((IntTag) myPetNBT.getValue().get("Respawntime")).getValue();
        petName = ((StringTag) myPetNBT.getValue().get("Name")).getValue();

        if (myPetNBT.getValue().containsKey("Type"))
        {
            petType = MyPetType.valueOf(((StringTag) myPetNBT.getValue().get("Type")).getValue());
        }

        if (myPetNBT.getValue().containsKey("Skilltree"))
        {
            String skillTreeName = ((StringTag) myPetNBT.getValue().get("Skilltree")).getValue();
            if (skillTreeName != null)
            {
                if (MyPetSkillTreeMobType.getMobTypeByPetType(petType) != null)
                {
                    MyPetSkillTreeMobType mobType = MyPetSkillTreeMobType.getMobTypeByPetType(petType);

                    if (mobType.hasSkillTree(skillTreeName))
                    {
                        this.skillTree = mobType.getSkillTree(skillTreeName);
                    }
                }
            }
        }

        if (myPetNBT.getValue().containsKey("Hunger"))
        {
            hunger = ((IntTag) myPetNBT.getValue().get("Hunger")).getValue();
        }

        if (myPetNBT.getValue().containsKey("WorldGroup"))
        {
            worldGroup = ((StringTag) myPetNBT.getValue().get("WorldGroup")).getValue();
        }

        setSkills((CompoundTag) myPetNBT.getValue().get("Skills"));
        setInfo((CompoundTag) myPetNBT.getValue().get("Info"));

    }

    @Override
    public String toString()
    {
        return "InactiveMyPet{type=" + getPetType().getTypeName() + ", owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + getExp() + ", health=" + getHealth() + "}";
    }
}