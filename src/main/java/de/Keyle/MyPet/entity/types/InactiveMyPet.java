/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.keyle.knbt.*;

import java.util.Collection;
import java.util.UUID;

public class InactiveMyPet implements MyPet, NBTStorage {
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
    private TagCompound NBTSkills;
    private TagCompound NBTextendetInfo;
    public boolean wantsToRespawn = false;

    public InactiveMyPet(MyPetPlayer petOwner) throws IllegalArgumentException {
        if (petOwner == null) {
            throw new IllegalArgumentException("Owner must not be null.");
        }
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

    public TagCompound getInfo() {
        if (NBTextendetInfo == null) {
            NBTextendetInfo = new TagCompound();
        }
        return NBTextendetInfo;
    }

    public void setInfo(TagCompound info) {
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
            this.health = de.Keyle.MyPet.entity.types.MyPet.getStartHP(petType.getMyPetClass());
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

    public TagCompound getSkills() {
        if (NBTSkills == null) {
            NBTSkills = new TagCompound();
        }
        return NBTSkills;
    }

    public void setSkills(TagCompound skills) {
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

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void setWorldGroup(String worldGroup) {
        if (worldGroup != null) {
            this.worldGroup = worldGroup;
        }
    }

    @Override
    public void load(TagCompound myPetNBT) {
        if (myPetNBT.getCompoundData().containsKey("UUID")) {
            uuid = UUID.fromString(myPetNBT.getAs("UUID", TagString.class).getStringData());
        }

        exp = myPetNBT.getAs("Exp", TagDouble.class).getDoubleData();
        if (myPetNBT.containsKeyAs("Health", TagInt.class)) {
            health = myPetNBT.getAs("Health", TagInt.class).getIntData();
        } else if (myPetNBT.containsKeyAs("Health", TagDouble.class)) {
            health = myPetNBT.getAs("Health", TagDouble.class).getDoubleData();
        }

        respawnTime = myPetNBT.getAs("Respawntime", TagInt.class).getIntData();
        petName = myPetNBT.getAs("Name", TagString.class).getStringData();

        if (myPetNBT.getCompoundData().containsKey("Type")) {
            petType = MyPetType.valueOf(myPetNBT.getAs("Type", TagString.class).getStringData());
        }

        if (myPetNBT.getCompoundData().containsKey("LastUsed")) {
            lastUsed = myPetNBT.getAs("LastUsed", TagLong.class).getLongData();
        }

        if (myPetNBT.getCompoundData().containsKey("Skilltree")) {
            String skillTreeName = myPetNBT.getAs("Skilltree", TagString.class).getStringData();
            if (skillTreeName != null) {
                if (SkillTreeMobType.getMobTypeByPetType(petType) != null) {
                    SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByPetType(petType);

                    if (mobType.hasSkillTree(skillTreeName)) {
                        this.skillTree = mobType.getSkillTree(skillTreeName);
                    }
                }
            }
        }

        if (myPetNBT.getCompoundData().containsKey("Hunger")) {
            hunger = myPetNBT.getAs("Hunger", TagInt.class).getIntData();
        }

        if (myPetNBT.getCompoundData().containsKey("WorldGroup")) {
            worldGroup = myPetNBT.getAs("WorldGroup", TagString.class).getStringData();
        }

        if (myPetNBT.getCompoundData().containsKey("Wants-To-Respawn")) {
            wantsToRespawn = myPetNBT.getAs("Wants-To-Respawn", TagByte.class).getBooleanData();
        }

        setSkills(myPetNBT.getAs("Skills", TagCompound.class));
        setInfo(myPetNBT.getAs("Info", TagCompound.class));
    }

    @Override
    public TagCompound save() {
        TagCompound petNBT = new TagCompound();

        petNBT.getCompoundData().put("UUID", new TagString(getUUID().toString()));
        petNBT.getCompoundData().put("Type", new TagString(this.petType.getTypeName()));
        petNBT.getCompoundData().put("Health", new TagDouble(this.health));
        petNBT.getCompoundData().put("Respawntime", new TagInt(this.respawnTime));
        petNBT.getCompoundData().put("Hunger", new TagInt(this.hunger));
        petNBT.getCompoundData().put("Name", new TagString(this.petName));
        petNBT.getCompoundData().put("WorldGroup", new TagString(this.worldGroup));
        petNBT.getCompoundData().put("Exp", new TagDouble(this.exp));
        petNBT.getCompoundData().put("LastUsed", new TagLong(this.lastUsed));
        petNBT.getCompoundData().put("Info", getInfo());
        petNBT.getCompoundData().put("Internal-Owner-UUID", new TagString(this.petOwner.getInternalUUID().toString()));
        petNBT.getCompoundData().put("Wants-To-Respawn", new TagByte(wantsToRespawn));
        if (this.skillTree != null) {
            petNBT.getCompoundData().put("Skilltree", new TagString(skillTree.getName()));
        }
        petNBT.getCompoundData().put("Skills", getSkills());

        return petNBT;
    }

    public void setSkills(Collection<ISkillInstance> skills) {
        if (NBTSkills == null) {
            NBTSkills = new TagCompound();
        }
        for (ISkillInstance skill : skills) {
            if (skill instanceof ISkillStorage) {
                ISkillStorage storageSkill = (ISkillStorage) skill;
                TagCompound s = storageSkill.save();
                if (s != null) {
                    this.NBTSkills.getCompoundData().put(skill.getName(), s);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "InactiveMyPet{type=" + getPetType().getTypeName() + ", owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + getExp() + ", health=" + getHealth() + ", worldgroup=" + worldGroup + (skillTree != null ? ", skilltree=" + skillTree.getName() : "") + "}";
    }
}