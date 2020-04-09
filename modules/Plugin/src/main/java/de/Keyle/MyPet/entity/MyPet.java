/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.event.MyPetLevelEvent;
import de.Keyle.MyPet.api.event.MyPetNameEvent;
import de.Keyle.MyPet.api.event.MyPetStatusEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.NameFilter;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.DamageImpl;
import de.Keyle.MyPet.skill.skills.LifeImpl;
import de.Keyle.MyPet.skill.skills.RangedImpl;
import de.Keyle.MyPet.util.hooks.VaultHook;
import de.Keyle.MyPet.util.hooks.WorldGuardHook;
import de.keyle.knbt.*;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public abstract class MyPet implements de.Keyle.MyPet.api.entity.MyPet, NBTStorage {

    protected final MyPetPlayer petOwner;
    protected MyPetBukkitEntity bukkitEntity;
    protected String petName;
    protected double health;
    protected int respawnTime = 0;
    protected int hungerTime;
    protected double saturation = 100;
    protected UUID uuid = null;
    protected String worldGroup = "";
    protected TagCompound storage = new TagCompound();
    protected PetState status = PetState.Despawned;
    protected boolean wantsToRespawn = false;
    protected Skilltree skilltree = null;
    protected Skills skills;
    protected MyPetExperience experience;
    protected long lastUsed = -1;

    @Override
    public void setExp(double exp) {
        getExperience().setExp(exp);
    }

    @Override
    public TagCompound getInfo() {
        TagCompound tag = writeExtendedInfo();

        // TODO replace with proper storage
        storage.put("level", new TagInt(getExperience().getLevel()));
        tag.put("storage", storage);

        return tag;
    }

    @Override
    public void setInfo(TagCompound info) {
        readExtendedInfo(info);

        // TODO replace with proper storage
        if (info.containsKey("storage")) {
            TagCompound storage = info.getAs("storage", TagCompound.class).clone();
            for (String key : storage.getCompoundData().keySet()) {
                this.storage.put(key, storage.get(key));
            }
        }
    }

    @Override
    public void setOwner(MyPetPlayer owner) {
        throw new UnsupportedOperationException("You can't change the owner for an active MyPet!");
    }

    @Override
    public void setPetType(MyPetType petType) {
        throw new UnsupportedOperationException("You can't change the type for an active MyPet!");
    }

    @Override
    public void setSkills(TagCompound skills) {
    }

    protected MyPet(MyPetPlayer petOwner) {
        if (petOwner == null) {
            throw new IllegalArgumentException("Owner must not be null.");
        }
        this.petOwner = petOwner;
        skills = new Skills(this);
        experience = new MyPetExperience(this);
        hungerTime = Configuration.HungerSystem.HUNGER_SYSTEM_TIME;
        petName = Translation.getString("Name." + getPetType().name(), petOwner);
    }

    public java.util.Optional<MyPetBukkitEntity> getEntity() {
        if (getStatus() == PetState.Here) {
            return java.util.Optional.of(bukkitEntity);
        }
        return java.util.Optional.empty();
    }

    public double getYSpawnOffset() {
        return 0;
    }

    public java.util.Optional<Location> getLocation() {
        if (status == PetState.Here) {
            return java.util.Optional.of(bukkitEntity.getLocation());
        } else if (petOwner.isOnline()) {
            return java.util.Optional.of(petOwner.getPlayer().getLocation());
        } else {
            return java.util.Optional.empty();
        }
    }

    public void setLocation(Location loc) {
        if (status == PetState.Here && MyPetApi.getPlatformHelper().canSpawn(loc, this.bukkitEntity.getHandle())) {
            bukkitEntity.teleport(loc);
        }
    }

    public double getDamage() {
        return getSkills().has(DamageImpl.class) ? getSkills().get(DamageImpl.class).getDamage().getValue().doubleValue() : 0;
    }

    public double getRangedDamage() {
        return getSkills().has(RangedImpl.class) ? getSkills().get(RangedImpl.class).getDamage().getValue().doubleValue() : 0;
    }

    public boolean isPassiv() {
        return getDamage() == 0 && getRangedDamage() == 0;
    }

    public boolean hasTarget() {
        return this.getStatus() == PetState.Here && bukkitEntity.getHandle().hasTarget();
    }

    public double getExp() {
        return getExperience().getExp();
    }

    public MyPetExperience getExperience() {
        return experience;
    }

    public TagCompound writeExtendedInfo() {
        TagCompound newTag = new TagCompound();
        newTag.put("Version", new TagShort(RepositoryMyPetConverterService.Version.valueOf(MyPetApi.getCompatUtil().getInternalVersion()).ordinal()));
        return newTag;
    }

    public void readExtendedInfo(TagCompound info) {
    }

    public double getMaxHealth() {
        return MyPetApi.getMyPetInfo().getStartHP(getPetType()) + (skills.isActive(LifeImpl.class) ? skills.get(LifeImpl.class).getLife().getValue().doubleValue() : 0);
    }

    public double getHealth() {
        double health;
        if (status == PetState.Here) {
            health = bukkitEntity.getHealth();
        } else {
            health = this.health;
        }
        if (health > getMaxHealth()) {
            this.setHealth(Double.MAX_VALUE);
            health = getMaxHealth();
        }
        return health;
    }

    public void setHealth(double health) {
        double maxHealth = getMaxHealth();
        health = Math.min(health, maxHealth);
        if (status == PetState.Here) {
            bukkitEntity.setHealth(health);
        } else {
            this.health = health;
        }
    }

    public double getSaturation() {
        //TODO remove when interaction is fixed
        switch (getPetType()) {
            case Giant:
            case Ghast:
                return 100;
        }
        if (Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
            return saturation;
        } else {
            return 100;
        }
    }

    public void setSaturation(double value) {
        //TODO remove when interaction is fixed
        switch (getPetType()) {
            case Giant:
            case Ghast:
                saturation = 100;
                return;
        }
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            saturation = Math.max(1, Math.min(100, value));
            hungerTime = Configuration.HungerSystem.HUNGER_SYSTEM_TIME;
        } else {
            MyPetApi.getLogger().warning("Saturation was set to an invalid number!\n" + Util.stackTraceToString());
        }
    }

    public void decreaseSaturation(double value) {
        //TODO remove when interaction is fixed
        switch (getPetType()) {
            case Giant:
            case Ghast:
                saturation = 100;
                return;
        }
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            saturation = Math.max(1, Math.min(100, saturation - value));
        } else {
            MyPetApi.getLogger().warning("Saturation was decreased by an invalid number!\n" + Util.stackTraceToString());
        }
    }

    public String getPetName() {
        return this.petName;
    }

    public void setPetName(String newName) {
        if (!NameFilter.isClean(newName)) {
            newName = Translation.getString("Name." + getPetType().name(), getOwner().getLanguage());
        }
        if (!this.petName.equals(newName)) {
            MyPetNameEvent event = new MyPetNameEvent(this, newName);
            Bukkit.getPluginManager().callEvent(event);
            newName = event.getNewName();
        }
        this.petName = newName;
        if (status == PetState.Here) {
            if (Configuration.Name.Tag.SHOW) {
                getEntity().ifPresent(entity -> entity.getHandle().updateNameTag());
            }
        }
    }

    public abstract MyPetType getPetType();

    public int getRespawnTime() {
        return respawnTime;
    }

    public void setRespawnTime(int time) {
        respawnTime = Math.max(time, 0);

        if (respawnTime > 0) {
            updateStatus(PetState.Dead);
        }
    }

    public boolean autoAssignSkilltree() {
        if (skilltree == null && this.petOwner.isOnline()) {
            if (Configuration.Skilltree.RANDOM_SKILLTREE_ASSIGNMENT) {
                return setSkilltree(MyPetApi.getSkilltreeManager().getRandomSkilltree(this));
            } else if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT) {
                List<Skilltree> skilltrees = new ArrayList<>(MyPetApi.getSkilltreeManager().getOrderedSkilltrees());

                for (Skilltree skilltree : skilltrees) {
                    if (skilltree.getMobTypes().contains(getPetType()) && skilltree.checkRequirements(this)) {
                        return setSkilltree(skilltree);
                    }
                }
                return false;
            }
            getOwner().sendMessage(Util.formatText(Translation.getString("Message.Skilltree.SelectionPrompt", getOwner()), getPetName()), 120000);
        }
        return true;
    }

    public Skilltree getSkilltree() {
        return skilltree;
    }

    public TagCompound getSkillInfo() {
        TagCompound skillsNBT = new TagCompound();
        Collection<Skill> skillList = this.getSkills().all();
        if (skillList.size() > 0) {
            for (Skill skill : skillList) {
                if (skill instanceof NBTStorage) {
                    NBTStorage storageSkill = (NBTStorage) skill;
                    TagCompound s = storageSkill.save();
                    if (s != null) {
                        skillsNBT.getCompoundData().put(skill.getName(), s);
                    }
                }
            }
        }
        return skillsNBT;
    }

    public Skills getSkills() {
        return skills;
    }

    public PetState getStatus() {
        if (status == PetState.Here) {
            if (bukkitEntity == null || bukkitEntity.getHandle() == null) {
                updateStatus(PetState.Despawned);
            } else if (bukkitEntity.getHealth() <= 0 || bukkitEntity.isDead()) {
                updateStatus(PetState.Dead);
            }
        }
        return status;
    }

    public void setStatus(PetState status) {
        if (status == PetState.Here) {
            if (this.status == PetState.Dead) {
                respawnPet();
            } else if (this.status == PetState.Despawned) {
                createEntity();
            }
        } else if (status == PetState.Dead) {
            updateStatus(PetState.Dead);
        } else {
            if (this.status == PetState.Here) {
                removePet();
            }
        }
    }

    protected void updateStatus(PetState status) {
        if (this.status != status) {
            this.status = status;
            Bukkit.getPluginManager().callEvent(new MyPetStatusEvent(this, status));
        }
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

    public void setLastUsed(long date) {
        this.lastUsed = date;
    }

    @Override
    public long getLastUsed() {
        return lastUsed;
    }

    @Override
    public String getWorldGroup() {
        return this.worldGroup;
    }

    public void setWorldGroup(String worldGroup) {
        if (worldGroup == null) {
            return;
        }
        if (WorldGroup.getGroupByName(worldGroup) == null) {
            worldGroup = "default";
        }
        this.worldGroup = worldGroup;
        experience.setMaxLevel(Configuration.LevelSystem.Experience.LEVEL_CAP);
    }

    public SpawnFlags createEntity() {
        lastUsed = System.currentTimeMillis();
        if (status != PetState.Here && getOwner().isOnline()) {
            Player owner = getOwner().getPlayer();
            if (owner.isDead()) {
                updateStatus(PetState.Despawned);
                return SpawnFlags.OwnerDead;
            }
            if (owner.getGameMode().name().equals("SPECTATOR")) {
                return SpawnFlags.Spectator;
            }

            if (respawnTime <= 0) {
                Location loc = petOwner.getPlayer().getLocation();

                if (!WorldGroup.getGroupByWorld(loc.getWorld().getName()).getName().equals(getWorldGroup())) {
                    return SpawnFlags.WrongWorldGroup;
                }

                int ownerX = owner.getLocation().getChunk().getX();
                int ownerZ = owner.getLocation().getChunk().getZ();
                if (!owner.getWorld().isChunkLoaded(ownerX, ownerZ)) {
                    return SpawnFlags.InvalidPosition;
                }

                if (owner.isFlying()) {
                    boolean groundFound = false;
                    for (int i = 10; i >= 0; i--) {
                        Block b = loc.getBlock();
                        if (b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                            groundFound = true;
                            break;
                        }
                        loc = loc.subtract(0, 1, 0);
                    }

                    if (!groundFound) {
                        return SpawnFlags.Flying;
                    }
                }

                MyPetCallEvent event = new MyPetCallEvent(this);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return SpawnFlags.NotAllowed;
                }

                if (!MyPetApi.getHookHelper().isPetAllowed(getOwner())) {
                    return SpawnFlags.NotAllowed;
                }

                MyPetMinecraftEntity minecraftEntity = MyPetApi.getEntityRegistry().createMinecraftEntity(this, loc.getWorld());

                if (minecraftEntity == null) {
                    updateStatus(PetState.Despawned);
                    return SpawnFlags.Canceled;
                }
                bukkitEntity = minecraftEntity.getBukkitEntity();

                bukkitEntity.setMetadata("MyPet", new FixedMetadataValue(MyPetApi.getPlugin(), true));

                if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.10") >= 0) {
                    Random r = new Random(petOwner.getInternalUUID().toString().hashCode());
                    String random = RandomStringUtils.random(10, 0, 0, true, true, null, r);

                    Team t;
                    if (owner.getScoreboard().getTeam("MyPet-" + random) != null) {
                        t = owner.getScoreboard().getTeam("MyPet-" + random);
                    } else {
                        t = owner.getScoreboard().registerNewTeam("MyPet-" + random);
                        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                    }

                    for (String entry : t.getEntries()) {
                        try {
                            t.removeEntry(entry);
                        } catch (IllegalStateException ignored) {
                        }
                    }
                    t.addEntry(minecraftEntity.getUniqueID().toString());
                }

                if (getYSpawnOffset() > 0) {
                    loc = loc.add(0, getYSpawnOffset(), 0);
                }
                loc.setPitch(0);
                loc.setYaw(0);

                Location origin = loc.clone();
                boolean positionFound = false;

                loc.subtract(1, 0, 1);
                for (double x = 0; x <= 2; x += 0.5) {
                    for (double z = 0; z <= 2; z += 0.5) {
                        if (x != 1 && z != 1) {
                            minecraftEntity.setLocation(loc);
                            if (MyPetApi.getPlatformHelper().canSpawn(loc, minecraftEntity)) {
                                Block b = loc.getBlock();
                                if (b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                                    positionFound = true;
                                    break;
                                }
                            }
                        }
                        loc.add(0, 0, 0.5);
                    }
                    if (positionFound) {
                        break;
                    }
                    loc.subtract(0, 0, 2);
                    loc.add(0.5, 0, 0);
                }
                if (!positionFound) {
                    minecraftEntity.setLocation(origin);
                    if (!MyPetApi.getPlatformHelper().canSpawn(origin, minecraftEntity)) {
                        updateStatus(PetState.Despawned);
                        return SpawnFlags.NoSpace;
                    }
                }

                WorldGuardHook wgHook = MyPetApi.getPluginHookManager().getHook(WorldGuardHook.class);
                if (wgHook != null) {
                    wgHook.fixMissingEntityType(loc.getWorld(), true);
                }
                if (MyPetApi.getEntityRegistry().spawnMinecraftEntity(minecraftEntity, loc.getWorld())) {
                    if (wgHook != null) {
                        wgHook.fixMissingEntityType(loc.getWorld(), false);
                    }

                    updateStatus(PetState.Here);

                    if (worldGroup == null || worldGroup.equals("")) {
                        setWorldGroup(WorldGroup.getGroupByWorld(loc.getWorld().getName()).getName());
                    }

                    autoAssignSkilltree();

                    wantsToRespawn = false;

                    return SpawnFlags.Success;
                }
                if (wgHook != null) {
                    wgHook.fixMissingEntityType(loc.getWorld(), false);
                }
                return SpawnFlags.Canceled;
            }
        }
        if (status == PetState.Dead) {
            return SpawnFlags.Dead;
        } else {
            return SpawnFlags.AlreadyHere;
        }
    }

    public void removePet() {
        if (status == PetState.Here) {
            health = bukkitEntity.getHealth();
            updateStatus(PetState.Despawned);
            bukkitEntity.removeEntity();
            bukkitEntity = null;

            getSkills().get(BackpackImpl.class).closeInventory();
        }
    }

    public void removePet(boolean wantToRespawn) {
        this.wantsToRespawn = wantToRespawn;
        removePet();
    }

    public void respawnPet() {
        if (status != PetState.Here && getOwner().isOnline()) {
            updateStatus(PetState.Despawned);
            respawnTime = 0;
            switch (createEntity()) {
                case Success:
                    getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Respawn", petOwner), petName));
                    break;
                case Canceled:
                    getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", petOwner), petName));
                    break;
                case NoSpace:
                    getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", petOwner), petName));
                    break;
                case Flying:
                    getOwner().sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", petOwner), petName));
                    break;
            }
            if (Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
                setHealth((int) Math.ceil(getMaxHealth() / 100. * (saturation + 1 - (saturation % 10))));
            } else {
                setHealth(getMaxHealth());
            }
        }
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    public void setWantsToRespawn(boolean wantsToRespawn) {
        this.wantsToRespawn = wantsToRespawn;
    }

    public boolean wantsToRespawn() {
        return wantsToRespawn;
    }

    public void schedule() {
        if (status != PetState.Despawned && getOwner().isOnline()) {
            if (status == PetState.Dead) {
                if (!Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                    respawnTime--;
                }
                if (respawnTime <= 0) {
                    respawnPet();
                } else if (MyPetApi.getPluginHookManager().isHookActive(VaultHook.class) && getOwner().hasAutoRespawnEnabled() && respawnTime >= getOwner().getAutoRespawnMin() && Permissions.has(getOwner().getPlayer(), "MyPet.user.respawn")) {
                    double cost = respawnTime * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
                    VaultHook vaultHook = MyPetApi.getPluginHookManager().getHook(VaultHook.class);
                    if (vaultHook.canPay(getOwner().getPlayer(), cost)) {
                        vaultHook.pay(getOwner().getPlayer(), cost);
                        getOwner().sendMessage(Util.formatText(Translation.getString("Message.Command.Respawn.Paid", petOwner.getLanguage()), petName, cost + " " + vaultHook.currencyNameSingular()));
                        respawnTime = 0;
                    }
                }
            }
            if (status == PetState.Here) {
                for (Skill skill : skills.all()) {
                    if (skill instanceof Scheduler) {
                        ((Scheduler) skill).schedule();
                    }
                }
                if (Configuration.HungerSystem.USE_HUNGER_SYSTEM) {
                    if (saturation > 1 && --hungerTime <= 0) {
                        saturation--;
                        hungerTime = Configuration.HungerSystem.HUNGER_SYSTEM_TIME;
                        if (saturation == 66) {
                            getOwner().sendMessage(Util.formatText(Translation.getString("Message.Hunger.Rumbling", getOwner()), getPetName()));
                        } else if (saturation == 33) {
                            getOwner().sendMessage(Util.formatText(Translation.getString("Message.Hunger.Hungry", getOwner()), getPetName()));
                        } else if (saturation == 1) {
                            getOwner().sendMessage(Util.formatText(Translation.getString("Message.Hunger.Starving", getOwner()), getPetName()));
                        }
                    }
                    if (saturation == 1 && getHealth() >= 2) {
                        getEntity().ifPresent(entity -> entity.damage(1.));
                    }
                }
            }
        }
    }

    @Override
    public void load(TagCompound myPetNBT) {
    }

    @Override
    public TagCompound save() {
        TagCompound petNBT = new TagCompound();

        petNBT.getCompoundData().put("UUID", new TagString(getUUID().toString()));
        petNBT.getCompoundData().put("Type", new TagString(this.getPetType().name()));
        petNBT.getCompoundData().put("Health", new TagDouble(this.getHealth()));
        petNBT.getCompoundData().put("Respawntime", new TagInt(this.respawnTime));
        petNBT.getCompoundData().put("Hunger", new TagDouble(this.saturation));
        petNBT.getCompoundData().put("Name", new TagString(this.petName));
        petNBT.getCompoundData().put("WorldGroup", new TagString(this.worldGroup));
        petNBT.getCompoundData().put("Exp", new TagDouble(this.getExp()));
        petNBT.getCompoundData().put("LastUsed", new TagLong(this.lastUsed));
        petNBT.getCompoundData().put("Info", writeExtendedInfo());
        petNBT.getCompoundData().put("Internal-Owner-UUID", new TagString(this.petOwner.getInternalUUID().toString()));
        petNBT.getCompoundData().put("Wants-To-Respawn", new TagByte(wantsToRespawn));
        if (this.skilltree != null) {
            petNBT.getCompoundData().put("Skilltree", new TagString(skilltree.getName()));
        }
        TagCompound skillsNBT = new TagCompound();
        Collection<Skill> skillList = this.getSkills().all();
        if (skillList.size() > 0) {
            for (Skill skill : skillList) {
                if (skill instanceof NBTStorage) {
                    NBTStorage storageSkill = (NBTStorage) skill;
                    TagCompound s = storageSkill.save();
                    if (s != null) {
                        skillsNBT.getCompoundData().put(skill.getName(), s);
                    }
                }
            }
        }
        petNBT.getCompoundData().put("Skills", skillsNBT);

        return petNBT;
    }

    @Override
    public String toString() {
        return "MyPet{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skilltree.getName() + ", worldgroup=" + worldGroup + "}";
    }

    public static float[] getEntitySize(Class<? extends MyPetMinecraftEntity> entityMyPetClass) {
        EntitySize es = entityMyPetClass.getAnnotation(EntitySize.class);
        if (es != null) {
            return new float[]{es.height(), es.width()};
        }
        return new float[]{0, 0};
    }


    public boolean setSkilltree(Skilltree skilltree) {
        if (skilltree == null || this.skilltree == skilltree) {
            return false;
        }
        if (skilltree.getRequiredLevel() > 1 && getExperience().getLevel() < skilltree.getRequiredLevel()) {
            return false;
        }
        this.skilltree = skilltree;
        getServer().getPluginManager().callEvent(new MyPetLevelEvent(this, experience.getLevel()));
        return true;
    }
}