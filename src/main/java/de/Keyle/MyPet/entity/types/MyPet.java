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

import com.google.common.collect.ArrayListMultimap;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.event.MyPetCallEvent;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import de.Keyle.MyPet.skill.Experience;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.Skills;
import de.Keyle.MyPet.skill.skills.implementation.Damage;
import de.Keyle.MyPet.skill.skills.implementation.HP;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skills.implementation.Ranged;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.hooks.Economy;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.hooks.arenas.*;
import de.Keyle.MyPet.util.locale.Translation;
import de.keyle.knbt.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public abstract class MyPet implements de.Keyle.MyPet.api.entity.MyPet, NBTStorage {
    private static Map<Class<? extends MyPet>, Double> startHP = new HashMap<>();
    private static Map<Class<? extends MyPet>, Double> startSpeed = new HashMap<>();
    private static ArrayListMultimap<Class<? extends MyPet>, ConfigItem> food = ArrayListMultimap.create();
    private static ArrayListMultimap<Class<? extends MyPet>, LeashFlag> leashFlags = ArrayListMultimap.create();
    private static Map<Class<? extends MyPet>, Integer> customRespawnTimeFactor = new HashMap<>();
    private static Map<Class<? extends MyPet>, Integer> customRespawnTimeFixed = new HashMap<>();
    private static Map<Class<? extends MyPet>, ConfigItem> leashItem = new HashMap<>();
    protected final MyPetPlayer petOwner;
    protected CraftMyPet craftMyPet;
    protected String petName = "Pet";
    protected double health;
    protected int respawnTime = 0;
    protected int hungerTime = 0;
    protected int hunger = 100;
    protected UUID uuid = null;
    protected String worldGroup = "";
    protected PetState status = PetState.Despawned;
    protected boolean wantsToRespawn = false;
    protected SkillTree skillTree = null;
    protected Skills skills;
    protected Experience experience;
    protected long lastUsed = -1;

    public enum LeashFlag {
        Baby, Adult, LowHp, Tamed, UserCreated, Wild, CanBreed, Angry, None, Impossible;

        public static LeashFlag getLeashFlagByName(String name) {
            for (LeashFlag leashFlags : LeashFlag.values()) {
                if (leashFlags.name().equalsIgnoreCase(name)) {
                    return leashFlags;
                }
            }
            return null;
        }
    }

    public enum SpawnFlags {
        Success, NoSpace, AlreadyHere, Dead, Canceled, OwnerDead, Flying, NotAllowed
    }

    public enum PetState {
        Dead, Despawned, Here
    }

    protected MyPet(MyPetPlayer petOwner) {
        if (petOwner == null) {
            throw new IllegalArgumentException("Owner must not be null.");
        }
        this.petOwner = petOwner;
        skills = new Skills(this);
        experience = new Experience(this);
        hungerTime = Configuration.HUNGER_SYSTEM_TIME;
        petName = Translation.getString("Name." + getPetType().getTypeName(), this.petOwner);
    }

    public CraftMyPet getCraftPet() {
        getStatus();
        return craftMyPet;
    }

    public double getYSpawnOffset() {
        return 0;
    }

    public Location getLocation() {
        if (status == PetState.Here) {
            return craftMyPet.getLocation();
        } else if (petOwner.isOnline()) {
            return petOwner.getPlayer().getLocation();
        } else {
            return null;
        }
    }

    public void setLocation(Location loc) {
        if (status == PetState.Here && BukkitUtil.canSpawn(loc, this.craftMyPet.getHandle())) {
            craftMyPet.teleport(loc);
        }
    }

    public double getDamage() {
        return getSkills().hasSkill(Damage.class) ? getSkills().getSkill(Damage.class).getDamage() : 0;
    }

    public double getRangedDamage() {
        return getSkills().hasSkill(Ranged.class) ? getSkills().getSkill(Ranged.class).getDamage() : 0;
    }

    public boolean isPassiv() {
        return getDamage() == 0 && getRangedDamage() == 0;
    }

    public boolean hasTarget() {
        return this.getStatus() == PetState.Here && craftMyPet.getHandle().getGoalTarget() != null && craftMyPet.getHandle().getGoalTarget().isAlive();
    }

    public double getExp() {
        return getExperience().getExp();
    }

    public Experience getExperience() {
        return experience;
    }

    public TagCompound writeExtendedInfo() {
        return new TagCompound();
    }

    public void readExtendedInfo(TagCompound info) {
    }

    public double getMaxHealth() {
        return getStartHP(this.getClass()) + (skills.isSkillActive(HP.class) ? skills.getSkill(HP.class).getHpIncrease() : 0);
    }

    public double getHealth() {
        if (status == PetState.Here) {
            return craftMyPet.getHealth();
        } else {
            return health;
        }
    }

    public void setHealth(double d) {
        if (d > getMaxHealth()) {
            health = getMaxHealth();
        } else {
            health = d;
        }
        if (status == PetState.Here) {
            craftMyPet.setHealth(health);
        }
    }

    public int getHungerValue() {
        if (Configuration.USE_HUNGER_SYSTEM) {
            return hunger;
        } else {
            return 100;
        }
    }

    public void setHungerValue(int value) {
        if (value > 100) {
            hunger = 100;
        } else if (value < 1) {
            hunger = 1;
        } else {
            hunger = value;
        }
        hungerTime = Configuration.HUNGER_SYSTEM_TIME;
    }

    public String getPetName() {
        return this.petName;
    }

    public void setPetName(String newName) {
        this.petName = NameFilter.clean(newName);
        if (status == PetState.Here) {
            if (Configuration.PET_INFO_OVERHEAD_NAME) {
                getCraftPet().getHandle().setCustomNameVisible(true);
                getCraftPet().getHandle().setCustomName(Util.cutString(Configuration.PET_INFO_OVERHEAD_PREFIX + petName + Configuration.PET_INFO_OVERHEAD_SUFFIX, 64));
            }
        }
    }

    public abstract MyPetType getPetType();

    public int getRespawnTime() {
        return respawnTime;
    }

    public void setRespawnTime(int time) {
        respawnTime = time > 0 ? time : 0;

        if (respawnTime > 0) {
            status = PetState.Dead;
        }
    }

    public boolean autoAssignSkilltree() {
        if (skillTree == null && this.petOwner.isOnline()) {
            if (Configuration.AUTOMATIC_SKILLTREE_ASSIGNMENT) {
                if (SkillTreeMobType.getSkillTreeNames(this.getPetType()).size() > 0) {
                    List<SkillTree> skilltrees = SkillTreeMobType.getSkillTrees(this.getPetType());
                    if (Configuration.RANDOM_SKILLTREE_ASSIGNMENT) {
                        Collections.shuffle(skilltrees);
                    }
                    for (SkillTree skillTree : skilltrees) {
                        if (Permissions.has(this.petOwner.getPlayer(), "MyPet.custom.skilltree." + skillTree.getPermission())) {
                            return setSkilltree(skillTree);
                        }
                    }
                }
            } else {
                for (SkillTree skillTree : SkillTreeMobType.getSkillTrees(this.getPetType())) {
                    if (Permissions.has(this.petOwner.getPlayer(), "MyPet.custom.skilltree." + skillTree.getPermission())) {
                        sendMessageToOwner(Util.formatText(Translation.getString("Message.Skilltree.SelectionPrompt", getOwner()), getPetName()));
                        break;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public Skills getSkills() {
        return skills;
    }

    public PetState getStatus() {
        if (status == PetState.Here) {
            if (craftMyPet == null || craftMyPet.getHandle() == null) {
                status = PetState.Despawned;
            } else if (craftMyPet.getHealth() <= 0 || craftMyPet.isDead()) {
                status = PetState.Dead;
            }
        }
        return status;
    }

    public void setStatus(PetState status) {
        if (status == PetState.Here) {
            if (this.status == PetState.Dead) {
                respawnPet();
            } else if (this.status == PetState.Despawned) {
                createPet();
            }
        } else if (status == PetState.Dead) {
            this.status = PetState.Dead;
        } else {
            if (this.status == PetState.Here) {
                removePet();
            }
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
    }

    public SpawnFlags createPet() {
        lastUsed = System.currentTimeMillis();
        if (status != PetState.Here && getOwner().isOnline()) {
            if (getOwner().getPlayer().isDead()) {
                status = PetState.Despawned;
                return SpawnFlags.OwnerDead;
            }

            if (respawnTime <= 0) {
                Location loc = petOwner.getPlayer().getLocation();
                if (getOwner().getPlayer().isFlying()) {
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

                EntityMyPet petEntity = getPetType().getNewEntityInstance(BukkitUtil.getWorldNMS(loc.getWorld()), this);
                craftMyPet = petEntity.getBukkitEntity();
                if (getYSpawnOffset() > 0) {
                    loc = loc.add(0, getYSpawnOffset(), 0);
                }
                loc.setPitch(0);
                loc.setYaw(0);
                petEntity.setLocation(loc);

                MyPetCallEvent event = new MyPetCallEvent(this);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return SpawnFlags.Canceled;
                }

                if (!BukkitUtil.canSpawn(loc, petEntity)) {
                    status = PetState.Despawned;
                    return SpawnFlags.NoSpace;
                }

                if (Minigames.DISABLE_PETS_IN_MINIGAMES && Minigames.isInMinigame(getOwner())) {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (PvPArena.DISABLE_PETS_IN_ARENA && PvPArena.isInPvPArena(getOwner())) {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (MobArena.DISABLE_PETS_IN_ARENA && MobArena.isInMobArena(getOwner())) {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (BattleArena.DISABLE_PETS_IN_ARENA && BattleArena.isInBattleArena(getOwner())) {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (SurvivalGames.DISABLE_PETS_IN_SURVIVAL_GAMES && SurvivalGames.isInSurvivalGames(getOwner())) {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (SurvivalGames.DISABLE_PETS_IN_SURVIVAL_GAMES && UltimateSurvivalGames.isInSurvivalGames(getOwner())) {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }

                if (!BukkitUtil.getWorldNMS(loc.getWorld()).addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
                    status = PetState.Despawned;
                    return SpawnFlags.Canceled;
                }
                craftMyPet.setMetadata("MyPet", new FixedMetadataValue(MyPetPlugin.getPlugin(), this));
                status = PetState.Here;

                if (worldGroup == null || worldGroup.equals("")) {
                    setWorldGroup(WorldGroup.getGroupByWorld(craftMyPet.getWorld().getName()).getName());
                }

                autoAssignSkilltree();
                wantsToRespawn = true;

                return SpawnFlags.Success;
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
            health = craftMyPet.getHealth();
            status = PetState.Despawned;
            craftMyPet.getHandle().dead = true;
            craftMyPet = null;
        }
    }

    public void removePet(boolean wantToRespawn) {
        this.wantsToRespawn = wantToRespawn;
        removePet();
    }

    public void respawnPet() {
        if (status != PetState.Here && getOwner().isOnline()) {
            respawnTime = 0;
            switch (createPet()) {
                case Success:
                    sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Respawn", petOwner), petName));
                    break;
                case Canceled:
                    sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Prevent", petOwner), petName));
                    break;
                case NoSpace:
                    sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.NoSpace", petOwner), petName));
                    break;
                case Flying:
                    sendMessageToOwner(Util.formatText(Translation.getString("Message.Spawn.Flying", petOwner), petName));
                    break;
            }
            if (Configuration.USE_HUNGER_SYSTEM) {
                setHealth((int) Math.ceil(getMaxHealth() / 100. * (hunger + 1 - (hunger % 10))));
            } else {
                setHealth(getMaxHealth());
            }
        }
    }

    public MyPetPlayer getOwner() {
        return petOwner;
    }

    public void sendMessageToOwner(String text) {
        if (petOwner.isOnline()) {
            petOwner.getPlayer().sendMessage(text);
        }
    }

    public void setWantsToRespawn(boolean wantsToRespawn) {
        this.wantsToRespawn = wantsToRespawn;
    }

    public boolean wantToRespawn() {
        return wantsToRespawn;
    }

    public void scheduleTask() {
        if (status != PetState.Despawned && getOwner().isOnline()) {
            for (ISkillInstance skill : skills.getSkills()) {
                if (skill instanceof Scheduler) {
                    ((Scheduler) skill).schedule();
                }
            }
            if (status == PetState.Dead) {
                respawnTime--;
                if (Economy.canUseEconomy() && getOwner().hasAutoRespawnEnabled() && respawnTime >= getOwner().getAutoRespawnMin() && Permissions.has(getOwner().getPlayer(), "MyPet.user.respawn")) {
                    double cost = respawnTime * Configuration.RESPAWN_COSTS_FACTOR + Configuration.RESPAWN_COSTS_FIXED;
                    if (Economy.canPay(getOwner(), cost)) {
                        Economy.pay(getOwner(), cost);
                        sendMessageToOwner(Util.formatText(Translation.getString("Message.Command.Respawn.Paid", petOwner.getLanguage()), petName, cost + " " + Economy.getEconomy().currencyNameSingular()));
                        respawnTime = 1;
                    }
                }
                if (respawnTime <= 0) {
                    status = PetState.Despawned;
                    respawnPet();
                }
            }
            if (status == PetState.Here) {
                if (Configuration.USE_HUNGER_SYSTEM) {
                    if (hunger > 1 && --hungerTime <= 0) {
                        hunger--;
                        hungerTime = Configuration.HUNGER_SYSTEM_TIME;
                        if (hunger == 66) {
                            sendMessageToOwner(Util.formatText(Translation.getString("Message.Hunger.Rumbling", getOwner()), getPetName()));
                        } else if (hunger == 33) {
                            sendMessageToOwner(Util.formatText(Translation.getString("Message.Hunger.Hungry", getOwner()), getPetName()));
                        } else if (hunger == 1) {
                            sendMessageToOwner(Util.formatText(Translation.getString("Message.Hunger.Starving", getOwner()), getPetName()));
                        }
                    }
                    if (hunger == 1 && getHealth() >= 2) {
                        getCraftPet().damage(1.);
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
        petNBT.getCompoundData().put("Type", new TagString(this.getPetType().getTypeName()));
        petNBT.getCompoundData().put("Health", new TagDouble(this.health));
        petNBT.getCompoundData().put("Respawntime", new TagInt(this.respawnTime));
        petNBT.getCompoundData().put("Hunger", new TagInt(this.hunger));
        petNBT.getCompoundData().put("Name", new TagString(this.petName));
        petNBT.getCompoundData().put("WorldGroup", new TagString(this.worldGroup));
        petNBT.getCompoundData().put("Exp", new TagDouble(this.getExp()));
        petNBT.getCompoundData().put("LastUsed", new TagLong(this.lastUsed));
        petNBT.getCompoundData().put("Info", writeExtendedInfo());
        petNBT.getCompoundData().put("Internal-Owner-UUID", new TagString(this.petOwner.getInternalUUID().toString()));
        petNBT.getCompoundData().put("Wants-To-Respawn", new TagByte(wantsToRespawn));
        if (this.skillTree != null) {
            petNBT.getCompoundData().put("Skilltree", new TagString(skillTree.getName()));
        }
        TagCompound skillsNBT = new TagCompound();
        Collection<ISkillInstance> skillList = this.getSkills().getSkills();
        if (skillList.size() > 0) {
            for (ISkillInstance skill : skillList) {
                if (skill instanceof ISkillStorage) {
                    ISkillStorage storageSkill = (ISkillStorage) skill;
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
        return "MyPet{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + ", worldgroup=" + worldGroup + "}";
    }

    public static float[] getEntitySize(Class<? extends EntityMyPet> entityMyPetClass) {
        EntitySize es = entityMyPetClass.getAnnotation(EntitySize.class);
        if (es != null) {
            return new float[]{es.height(), es.width()};
        }
        return new float[]{0, 0};
    }

    public static int getCustomRespawnTimeFactor(Class<? extends MyPet> myPetClass) {
        if (customRespawnTimeFactor.containsKey(myPetClass)) {
            return customRespawnTimeFactor.get(myPetClass);
        }
        return 0;
    }

    public static void setCustomRespawnTimeFactor(Class<? extends MyPet> myPetClass, int factor) {
        customRespawnTimeFactor.put(myPetClass, factor);
    }

    public static int getCustomRespawnTimeFixed(Class<? extends MyPet> myPetClass) {
        if (customRespawnTimeFixed.containsKey(myPetClass)) {
            return customRespawnTimeFixed.get(myPetClass);
        }
        return 0;
    }

    public static void setCustomRespawnTimeFixed(Class<? extends MyPet> myPetClass, int factor) {
        customRespawnTimeFixed.put(myPetClass, factor);
    }

    public static List<ConfigItem> getFood(Class<? extends MyPet> myPetClass) {
        return food.get(myPetClass);
    }

    public static void setFood(Class<? extends MyPet> myPetClass, ConfigItem foodToAdd) {
        for (ConfigItem configItem : food.get(myPetClass)) {
            if (configItem.compare(foodToAdd.getItem())) {
                return;
            }
        }
        food.put(myPetClass, foodToAdd);
    }

    public static boolean hasLeashFlag(Class<? extends MyPet> myPetClass, LeashFlag flag) {
        return leashFlags.get(myPetClass).contains(flag);
    }

    public static List<LeashFlag> getLeashFlags(Class<? extends MyPet> myPetClass) {
        return leashFlags.get(myPetClass);
    }

    public static void setLeashFlags(Class<? extends MyPet> myPetClass, LeashFlag leashFlagToAdd) {
        if (!leashFlags.get(myPetClass).contains(leashFlagToAdd)) {
            leashFlags.put(myPetClass, leashFlagToAdd);
        }
    }

    public boolean setSkilltree(SkillTree skillTree) {
        if (skillTree == null || this.skillTree == skillTree) {
            return false;
        }
        if (skillTree.getRequiredLevel() > 1 && getExperience().getLevel() < skillTree.getRequiredLevel()) {
            return false;
        }
        skills.reset();
        this.skillTree = skillTree;
        getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(this, experience.getLevel(), 0, true));
        return true;
    }

    public static double getStartHP(Class<? extends MyPet> myPetClass) {
        if (startHP.containsKey(myPetClass)) {
            return startHP.get(myPetClass);
        }
        return 20;
    }

    public static void setStartHP(Class<? extends MyPet> myPetClass, double hp) {
        startHP.put(myPetClass, hp);
    }

    public static ConfigItem getLeashItem(Class<? extends MyPet> myPetClass) {
        return leashItem.get(myPetClass);
    }

    public static void setLeashItem(Class<? extends MyPet> myPetClass, ConfigItem configItem) {
        leashItem.put(myPetClass, configItem);
    }

    public static double getStartSpeed(MyPetType myPetType) {
        if (myPetType != null) {
            return getStartSpeed(myPetType.getMyPetClass());
        }
        return 0.3F;
    }

    public static double getStartSpeed(Class<? extends MyPet> myPetClass) {
        if (startSpeed.containsKey(myPetClass)) {
            return startSpeed.get(myPetClass);
        }
        return 0.3F;
    }

    public static void setStartSpeed(Class<? extends MyPet> myPetClass, double speed) {
        startSpeed.put(myPetClass, speed);
    }

    public static void resetOptions() {
        customRespawnTimeFactor.clear();
        customRespawnTimeFixed.clear();
        leashFlags.clear();
        food.clear();
        startSpeed.clear();
        startHP.clear();
    }
}