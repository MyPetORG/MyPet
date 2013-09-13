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

import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.NBTStorage;
import org.spout.nbt.*;

import java.util.Collection;
import java.util.UUID;

public class InactiveMyPet implements IMyPet, NBTStorage {
    private final MyPetPlayer petOwner;
    private UUID uuid = null;
    private String petName = "";
    private String worldGroup = "";
    private double health = -1;
    private int hunger = 100;
    private int respawnTime = 0;
    private double exp = 0;
    protected long lastUsed = -1;
    private MyPetType petType = MyPetType.Wolf;
    private SkillTree skillTree = null;
    private CompoundTag NBTSkills;
    private CompoundTag NBTextendetInfo;

    public InactiveMyPet(MyPetPlayer petOwner) {
        this.petOwner = petOwner;
    }

    public double getExp() {
        return exp;
    }

    public void setExp(double Exp) {
        this.exp = Exp;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public int getHungerValue() {
        return hunger;
    }

    public void setHungerValue(int value) {
        if (value > 100) {
            hunger = 100;
        } else if (value < 1) {
            hunger = 1;
        } else {
            hunger = value;
        }
    }

    public CompoundTag getInfo() {
        if (NBTextendetInfo == null) {
            NBTextendetInfo = new CompoundTag("Info", new CompoundMap());
        }
        return NBTextendetInfo;
    }

    public void setInfo(CompoundTag info) {
        NBTextendetInfo = info;
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public MyPetType getPetType() {
        return petType;
    }

    public void setPetType(MyPetType petType) {
        this.petType = petType;
        if (respawnTime <= 0 && health == -1) {
            this.health = MyPet.getStartHP(petType.getMyPetClass());
        }

    }

    public int getRespawnTime() {
        return respawnTime;
    }

    public void setRespawnTime(int respawnTime) {
        this.respawnTime = respawnTime;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public void setSkillTree(SkillTree skillTree) {
        this.skillTree = skillTree;
    }

    public CompoundTag getSkills() {
        if (NBTSkills == null) {
            NBTSkills = new CompoundTag("Skills", new CompoundMap());
        }
        return NBTSkills;
    }

    public void setSkills(CompoundTag skills) {
        NBTSkills = skills;
    }

    public UUID getUUID() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }

        return this.uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getWorldGroup() {
        return worldGroup;
    }

    @Override
    public long getLastUsed() {
        return lastUsed;
    }

    public void setWorldGroup(String worldGroup) {
        if (worldGroup != null) {
            this.worldGroup = worldGroup;
        }
    }

    @Override
    public void load(CompoundTag myPetNBT) {
        if (myPetNBT.getValue().containsKey("UUID")) {
            uuid = UUID.fromString(((StringTag) myPetNBT.getValue().get("UUID")).getValue());
        }

        exp = ((DoubleTag) myPetNBT.getValue().get("Exp")).getValue();
        if (myPetNBT.getValue().get("Health").getType() == TagType.TAG_INT) {
            health = ((IntTag) myPetNBT.getValue().get("Health")).getValue();
        } else {
            health = ((DoubleTag) myPetNBT.getValue().get("Health")).getValue();
        }

        respawnTime = ((IntTag) myPetNBT.getValue().get("Respawntime")).getValue();
        petName = ((StringTag) myPetNBT.getValue().get("Name")).getValue();

        if (myPetNBT.getValue().containsKey("Type")) {
            petType = MyPetType.valueOf(((StringTag) myPetNBT.getValue().get("Type")).getValue());
        }

        if (myPetNBT.getValue().containsKey("LastUsed")) {
            lastUsed = ((LongTag) myPetNBT.getValue().get("LastUsed")).getValue();
        }

        if (myPetNBT.getValue().containsKey("Skilltree")) {
            String skillTreeName = ((StringTag) myPetNBT.getValue().get("Skilltree")).getValue();
            if (skillTreeName != null) {
                if (SkillTreeMobType.getMobTypeByPetType(petType) != null) {
                    SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByPetType(petType);

                    if (mobType.hasSkillTree(skillTreeName)) {
                        this.skillTree = mobType.getSkillTree(skillTreeName);
                    }
                }
            }
        }

        if (myPetNBT.getValue().containsKey("Hunger")) {
            hunger = ((IntTag) myPetNBT.getValue().get("Hunger")).getValue();
        }

        if (myPetNBT.getValue().containsKey("WorldGroup")) {
            worldGroup = ((StringTag) myPetNBT.getValue().get("WorldGroup")).getValue();
        }

        setSkills((CompoundTag) myPetNBT.getValue().get("Skills"));
        setInfo((CompoundTag) myPetNBT.getValue().get("Info"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag petNBT = new CompoundTag(null, new CompoundMap());

        petNBT.getValue().put("UUID", new StringTag("UUID", getUUID().toString()));
        petNBT.getValue().put("Type", new StringTag("Type", this.petType.getTypeName()));
        petNBT.getValue().put("Owner", new StringTag("Owner", this.petOwner.getName()));
        petNBT.getValue().put("Health", new DoubleTag("Health", this.health));
        petNBT.getValue().put("Respawntime", new IntTag("Respawntime", this.respawnTime));
        petNBT.getValue().put("Hunger", new IntTag("Hunger", this.hunger));
        petNBT.getValue().put("Name", new StringTag("Name", this.petName));
        petNBT.getValue().put("WorldGroup", new StringTag("WorldGroup", this.worldGroup));
        petNBT.getValue().put("Exp", new DoubleTag("Exp", this.exp));
        petNBT.getValue().put("LastUsed", new LongTag("LastUsed", this.lastUsed));
        petNBT.getValue().put("Info", getInfo());
        if (this.skillTree != null) {
            petNBT.getValue().put("Skilltree", new StringTag("Skilltree", skillTree.getName()));
        }
        petNBT.getValue().put("Skills", getSkills());

        return petNBT;
    }

    public void setSkills(Collection<ISkillInstance> skills) {
        if (NBTSkills == null) {
            NBTSkills = new CompoundTag("Skills", new CompoundMap());
        }
        for (ISkillInstance skill : skills) {
            if (skill instanceof ISkillStorage) {
                ISkillStorage storageSkill = (ISkillStorage) skill;
                CompoundTag s = storageSkill.save();
                if (s != null) {
                    this.NBTSkills.getValue().put(skill.getName(), s);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "InactiveMyPet{type=" + getPetType().getTypeName() + ", owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + getExp() + ", health=" + getHealth() + ", worldgroup=" + worldGroup + "}";
    }
}