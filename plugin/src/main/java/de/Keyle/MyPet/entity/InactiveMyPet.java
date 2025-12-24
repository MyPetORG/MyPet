/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.NBTStorage;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.UUID;

public class InactiveMyPet implements StoredMyPet, NBTStorage {
    public boolean wantsToRespawn = false;
    protected long lastUsed = -1;
    private MyPetPlayer petOwner;
    private UUID uuid = null;
    private String petName = "";
    private String worldGroup = "";
    private double health = -1;
    private double saturation = 100;
    private int respawnTime = 0;
    private double exp = 0;
    private MyPetType petType = MyPetType.Wolf;
    private Skilltree skilltree = null;
    private CompoundBinaryTag NBTSkills;
    private CompoundBinaryTag NBTextendetInfo;

    public InactiveMyPet(MyPetPlayer petOwner) throws IllegalArgumentException {
        if (petOwner == null) {
            throw new IllegalArgumentException("Owner must not be null.");
        }
        this.petOwner = petOwner;
    }

    public double getExp() {
        return exp;
    }

    public void setExp(double exp) {
        this.exp = exp;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    @Override
    public double getSaturation() {
        return saturation;
    }

    @Override
    public void setSaturation(double value) {
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            saturation = Math.max(1, Math.min(100, value));
        } else {
            MyPetApi.getLogger().warning("Saturation was set to an invalid number!\n" + Util.stackTraceToString());
        }
    }

    public double getHungerValue() {
        return getSaturation();
    }

    public CompoundBinaryTag getInfo() {
        if (NBTextendetInfo == null) {
            NBTextendetInfo = CompoundBinaryTag.empty();
        }
        return NBTextendetInfo;
    }

    public void setInfo(CompoundBinaryTag info) {
        NBTextendetInfo = info;
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    public void setOwner(MyPetPlayer owner) {
        petOwner = owner;
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
            this.health = MyPetApi.getMyPetInfo().getStartHP(petType);
        }

    }

    public boolean wantsToRespawn() {
        return wantsToRespawn;
    }

    public void setWantsToRespawn(boolean wantsToRespawn) {
        this.wantsToRespawn = wantsToRespawn;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    public void setRespawnTime(int respawnTime) {
        this.respawnTime = respawnTime;
    }

    public Skilltree getSkilltree() {
        return skilltree;
    }

    public boolean setSkilltree(Skilltree skilltree) {
        this.skilltree = skilltree;
        return true;
    }

    public boolean setSkilltree(Skilltree skilltree, MyPetSelectSkilltreeEvent.Source source) {
        this.skilltree = skilltree;
        MyPetSelectSkilltreeEvent selectEvent = new MyPetSelectSkilltreeEvent(this, skilltree, source);
        Bukkit.getServer().getPluginManager().callEvent(selectEvent);
        return true;
    }

    public CompoundBinaryTag getSkillInfo() {
        if (NBTSkills == null) {
            NBTSkills = CompoundBinaryTag.empty();
        }
        return NBTSkills;
    }

    public void setSkills(CompoundBinaryTag skills) {
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

    public void setWorldGroup(String worldGroup) {
        if (worldGroup != null) {
            this.worldGroup = worldGroup;
        }
    }

    @Override
    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    @Override
    public void load(CompoundBinaryTag myPetNBT) {
        if (myPetNBT.keySet().contains("UUID")) {
            uuid = UUID.fromString(myPetNBT.getString("UUID"));
        }

        exp = myPetNBT.getDouble("Exp");

        // Health can be stored as int (legacy) or double
        BinaryTag healthTag = myPetNBT.get("Health");
        if (healthTag != null) {
            if (healthTag.type() == BinaryTagTypes.INT) {
                health = ((IntBinaryTag) healthTag).value();
            } else if (healthTag.type() == BinaryTagTypes.DOUBLE) {
                health = ((DoubleBinaryTag) healthTag).value();
            }
        }

        respawnTime = myPetNBT.getInt("Respawntime");
        petName = myPetNBT.getString("Name");

        if (myPetNBT.keySet().contains("Type")) {
            petType = MyPetType.valueOf(myPetNBT.getString("Type"));
        }

        if (myPetNBT.keySet().contains("LastUsed")) {
            lastUsed = myPetNBT.getLong("LastUsed");
        }

        if (myPetNBT.keySet().contains("Skilltree")) {
            String skillTreeName = myPetNBT.getString("Skilltree");
            if (!skillTreeName.isEmpty()) {
                Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skillTreeName);
                if (skilltree != null && skilltree.getMobTypes().contains(getPetType())) {
                    this.skilltree = skilltree;
                }
            }
        }

        // Hunger/saturation can be stored as int (legacy) or double
        BinaryTag hungerTag = myPetNBT.get("Hunger");
        if (hungerTag != null) {
            if (hungerTag.type() == BinaryTagTypes.INT) {
                saturation = ((IntBinaryTag) hungerTag).value();
            } else if (hungerTag.type() == BinaryTagTypes.DOUBLE) {
                saturation = ((DoubleBinaryTag) hungerTag).value();
            }
        }

        if (myPetNBT.keySet().contains("WorldGroup")) {
            worldGroup = myPetNBT.getString("WorldGroup");
        }

        if (myPetNBT.keySet().contains("Wants-To-Respawn")) {
            wantsToRespawn = myPetNBT.getBoolean("Wants-To-Respawn");
        }

        setSkills(myPetNBT.getCompound("Skills"));
        setInfo(myPetNBT.getCompound("Info"));
    }

    @Override
    public CompoundBinaryTag save() {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
                .putString("UUID", getUUID().toString())
                .putString("Type", this.petType.name())
                .putDouble("Health", this.health)
                .putInt("Respawntime", this.respawnTime)
                .putDouble("Hunger", this.saturation)
                .putString("Name", this.petName)
                .putString("WorldGroup", this.worldGroup)
                .putDouble("Exp", this.exp)
                .putLong("LastUsed", this.lastUsed)
                .put("Info", getInfo())
                .putString("Owner-UUID", this.petOwner.getUniqueId().toString())
                .putBoolean("Wants-To-Respawn", wantsToRespawn);

        if (this.skilltree != null) {
            builder.putString("Skilltree", skilltree.getName());
        }

        builder.put("Skills", getSkillInfo());

        return builder.build();
    }

    public void setSkills(Collection<Skill> skills) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
        if (NBTSkills != null) {
            // Preserve existing entries
            for (String key : NBTSkills.keySet()) {
                builder.put(key, NBTSkills.get(key));
            }
        }
        for (Skill skill : skills) {
            if (skill instanceof NBTStorage storageSkill) {
                CompoundBinaryTag s = storageSkill.save();
                if (s != null) {
                    builder.put(skill.getName(), s);
                }
            }
        }
        this.NBTSkills = builder.build();
    }

    @Override
    public String toString() {
        return "InactiveMyPet{type=" + getPetType().name() + ", owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + getExp() + ", health=" + getHealth() + ", worldgroup=" + worldGroup + (skilltree != null ? ", skilltree=" + skilltree.getName() : "") + "}";
    }
}