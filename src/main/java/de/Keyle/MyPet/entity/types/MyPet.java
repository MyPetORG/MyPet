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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.event.MyPetLevelUpEvent;
import de.Keyle.MyPet.api.event.MyPetSpoutEvent;
import de.Keyle.MyPet.api.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.skill.*;
import de.Keyle.MyPet.skill.skills.implementation.Damage;
import de.Keyle.MyPet.skill.skills.implementation.HP;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skills.implementation.Ranged;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import de.Keyle.MyPet.util.support.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_6_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.spout.nbt.*;

import java.util.*;

import static org.bukkit.Bukkit.getPluginManager;
import static org.bukkit.Bukkit.getServer;

public abstract class MyPet implements IMyPet, NBTStorage
{
    private static Map<Class<? extends MyPet>, Double> startHP = new HashMap<Class<? extends MyPet>, Double>();
    private static Map<Class<? extends MyPet>, Double> startSpeed = new HashMap<Class<? extends MyPet>, Double>();
    private static Map<Class<? extends MyPet>, List<Material>> food = new HashMap<Class<? extends MyPet>, List<Material>>();
    private static Map<Class<? extends MyPet>, List<LeashFlag>> leashFlags = new HashMap<Class<? extends MyPet>, List<LeashFlag>>();
    private static Map<Class<? extends MyPet>, Integer> customRespawnTimeFactor = new HashMap<Class<? extends MyPet>, Integer>();
    private static Map<Class<? extends MyPet>, Integer> customRespawnTimeFixed = new HashMap<Class<? extends MyPet>, Integer>();

    static
    {
        for (MyPetType petType : MyPetType.values())
        {
            startHP.put(petType.getMyPetClass(), 20D);
        }
    }

    public static enum LeashFlag
    {
        Baby, Adult, LowHp, Tamed, UserCreated, Wild, CanBreed, Angry, None, Impossible;

        public static LeashFlag getLeashFlagByName(String name)
        {
            for (LeashFlag leashFlags : LeashFlag.values())
            {
                if (leashFlags.name().equalsIgnoreCase(name))
                {
                    return leashFlags;
                }
            }
            return null;
        }
    }

    public static enum SpawnFlags
    {
        Success, NoSpace, AlreadyHere, Dead, Canceled, NotAllowed
    }

    public static enum PetState
    {
        Dead, Despawned, Here
    }

    protected CraftMyPet craftMyPet;
    protected String petName = "Pet";
    protected final MyPetPlayer petOwner;
    protected double health;
    protected int respawnTime = 0;
    protected int hungerTime = 0;
    protected int hunger = 100;
    protected UUID uuid = null;
    protected String worldGroup = "";
    protected PetState status = PetState.Despawned;
    protected boolean wantToRespawn = false;
    protected Location petLocation;
    protected MyPetSkillTree skillTree = null;
    protected MyPetSkills skills;
    protected MyPetExperience experience;

    public MyPet(MyPetPlayer Owner)
    {
        this.petOwner = Owner;
        skills = new MyPetSkills(this);
        experience = new MyPetExperience(this);
        hungerTime = MyPetConfiguration.HUNGER_SYSTEM_TIME;
        autoAssignSkilltree();
        petName = MyPetLocales.getString("Name." + getPetType().getTypeName(), petOwner.getLanguage());
    }

    public void setPetName(String newName)
    {
        this.petName = newName;
        if (status == PetState.Here)
        {
            if (MyPetConfiguration.PET_INFO_OVERHEAD_NAME)
            {
                getCraftPet().getHandle().setCustomNameVisible(true);
                getCraftPet().getHandle().setCustomName(MyPetUtil.cutString(MyPetConfiguration.PET_INFO_OVERHEAD_PREFIX + petName + MyPetConfiguration.PET_INFO_OVERHEAD_SUFFIX, 64));
            }
        }
        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            getPluginManager().callEvent(new MyPetSpoutEvent(this, MyPetSpoutEventReason.Name));
        }
    }

    public String getPetName()
    {
        return this.petName;
    }

    public MyPetSkillTree getSkillTree()
    {
        return skillTree;
    }

    public boolean setSkilltree(MyPetSkillTree skillTree)
    {
        if (skillTree == null || this.skillTree == skillTree)
        {
            return false;
        }
        skills.reset();
        this.skillTree = skillTree;
        for (int i = 1 ; i <= experience.getLevel() ; i++)
        {
            getServer().getPluginManager().callEvent(new MyPetLevelUpEvent(this, i, true));
        }
        return true;
    }

    public boolean autoAssignSkilltree()
    {
        if (MyPetConfiguration.AUTOMATIC_SKILLTREE_ASSIGNMENT && skillTree == null && this.petOwner.isOnline())
        {
            if (MyPetSkillTreeMobType.getSkillTreeNames(this.getPetType()).size() > 0)
            {
                for (MyPetSkillTree skillTree : MyPetSkillTreeMobType.getSkillTrees(this.getPetType()))
                {
                    if (MyPetPermissions.has(this.petOwner.getPlayer(), "MyPet.custom.skilltree." + skillTree.getPermission()))
                    {
                        return setSkilltree(skillTree);
                    }
                }
            }
        }
        return false;
    }

    public void removePet()
    {
        removePet(false);
    }

    public void removePet(boolean wantToRespawn)
    {
        if (status == PetState.Here)
        {
            health = craftMyPet.getHealth();
            petLocation = craftMyPet.getLocation();
            if (petLocation == null && getOwner().isOnline())
            {
                petLocation = getOwner().getPlayer().getLocation();
            }
            status = PetState.Despawned;
            this.wantToRespawn = wantToRespawn;
            craftMyPet.remove();
            craftMyPet = null;
        }
    }

    public void respawnPet()
    {
        if (status != PetState.Here && getOwner().isOnline())
        {
            petLocation = getOwner().getPlayer().getLocation();
            respawnTime = 0;
            switch (createPet())
            {
                case Success:
                    sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.OnRespawn", petOwner.getLanguage())).replace("%petname%", petName));
                    break;
                case Canceled:
                    sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnPrevent", petOwner.getLanguage())).replace("%petname%", petName));
                    break;
                case NoSpace:
                    sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.SpawnNoSpace", petOwner.getLanguage())).replace("%petname%", petName));
                    break;
            }
            if (MyPetConfiguration.USE_HUNGER_SYSTEM)
            {
                setHealth((int) Math.ceil(getMaxHealth() / 100. * (hunger + 1 - (hunger % 10))));
            }
            else
            {
                setHealth(getMaxHealth());
            }
        }
    }

    public SpawnFlags createPet()
    {
        if (status != PetState.Here && getOwner().isOnline())
        {
            if (respawnTime <= 0)
            {
                net.minecraft.server.v1_6_R1.World mcWorld = ((CraftWorld) petLocation.getWorld()).getHandle();
                EntityMyPet petEntity = getPetType().getNewEntityInstance(mcWorld, this);
                craftMyPet = (CraftMyPet) petEntity.getBukkitEntity();
                petEntity.setLocation(petLocation);
                if (!MyPetBukkitUtil.canSpawn(petLocation, petEntity))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NoSpace;
                }

                if (Minigames.DISABLE_PETS_IN_MINIGAMES && Minigames.isInMinigame(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (PvPArena.DISABLE_PETS_IN_ARENA && PvPArena.isInPvPArena(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (MobArena.DISABLE_PETS_IN_ARENA && MobArena.isInMobArena(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (BattleArena.DISABLE_PETS_IN_ARENA && BattleArena.isInBattleArena(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }
                if (SurvivalGames.DISABLE_PETS_IN_SURVIVAL_GAMES && SurvivalGames.isInSurvivalGames(getOwner()))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.NotAllowed;
                }

                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    status = PetState.Despawned;
                    return SpawnFlags.Canceled;
                }
                craftMyPet.setMetadata("MyPet", new FixedMetadataValue(MyPetPlugin.getPlugin(), this));
                status = PetState.Here;

                if (worldGroup == null || worldGroup.equals(""))
                {
                    setWorldGroup(MyPetWorldGroup.getGroup(craftMyPet.getWorld().getName()).getName());
                }

                autoAssignSkilltree();

                return SpawnFlags.Success;
            }
        }
        if (status == PetState.Dead)
        {
            return SpawnFlags.Dead;
        }
        else
        {
            return SpawnFlags.AlreadyHere;
        }
    }

    public CraftMyPet getCraftPet()
    {
        getStatus();
        return craftMyPet;
    }

    public PetState getStatus()
    {
        if (status == PetState.Here)
        {
            if (craftMyPet == null || craftMyPet.getHandle() == null)
            {
                status = PetState.Despawned;
            }
            else if (craftMyPet.getHealth() <= 0 || craftMyPet.isDead())
            {
                status = PetState.Dead;
            }
        }
        return status;
    }

    public boolean wantToRespawn()
    {
        return wantToRespawn;
    }

    public void setStatus(PetState status)
    {
        if (status == PetState.Here)
        {
            if (this.status == PetState.Dead)
            {
                respawnPet();
            }
            else if (this.status == PetState.Despawned)
            {
                createPet();
            }
        }
        else if (status == PetState.Dead)
        {
            this.status = PetState.Dead;
        }
        else
        {
            if (this.status == PetState.Here)
            {
                removePet();
            }
        }
    }

    public void setHealth(double d)
    {
        if (d > getMaxHealth())
        {
            health = getMaxHealth();
        }
        else
        {
            health = d;
        }
        if (status == PetState.Here)
        {
            craftMyPet.setHealth(health);
        }
    }

    public double getHealth()
    {
        if (status == PetState.Here)
        {
            return craftMyPet.getHealth();
        }
        else
        {
            return health;
        }
    }

    public double getMaxHealth()
    {
        return getStartHP(this.getClass()) + (skills.isSkillActive("HP") ? ((HP) skills.getSkill("HP")).getHpIncrease() : 0);
    }

    public int getRespawnTime()
    {
        return respawnTime;
    }

    public void setRespawnTime(int time)
    {
        respawnTime = time > 0 ? time : 0;
    }

    public int getHungerValue()
    {
        if (MyPetConfiguration.USE_HUNGER_SYSTEM)
        {
            return hunger;
        }
        else
        {
            return 100;
        }
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
        hungerTime = MyPetConfiguration.HUNGER_SYSTEM_TIME;

        if (MyPetConfiguration.ENABLE_EVENTS)
        {
            MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(this, MyPetSpoutEventReason.HungerChange);
            getServer().getPluginManager().callEvent(spoutEvent);
        }
    }

    public double getDamage()
    {
        return (getSkills().hasSkill("Damage") ? ((Damage) getSkills().getSkill("Damage")).getDamage() : 0);
    }

    public double getRangedDamage()
    {
        return (getSkills().hasSkill("Ranged") ? ((Ranged) getSkills().getSkill("Ranged")).getDamage() : 0);
    }

    public MyPetSkills getSkills()
    {
        return skills;
    }

    public MyPetExperience getExperience()
    {
        return experience;
    }

    public double getExp()
    {
        return getExperience().getExp();
    }

    public Location getLocation()
    {
        if (status == PetState.Here)
        {
            return craftMyPet.getLocation();
        }
        else
        {
            return petLocation;
        }
    }

    public void setLocation(Location loc)
    {
        this.petLocation = loc;
        if (status == PetState.Here && MyPetBukkitUtil.canSpawn(loc, this.craftMyPet.getHandle()))
        {
            craftMyPet.teleport(loc);
        }
    }

    @Override
    public String getWorldGroup()
    {
        return this.worldGroup;
    }

    public void setWorldGroup(String worldGroup)
    {
        if (worldGroup != null)
        {
            this.worldGroup = worldGroup;
        }
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

    public void scheduleTask()
    {
        if (status != PetState.Despawned && getOwner().isOnline())
        {
            for (ISkillInstance skill : skills.getSkills())
            {
                if (skill instanceof IScheduler)
                {
                    ((IScheduler) skill).schedule();
                }
            }
            if (status == PetState.Dead)
            {
                respawnTime--;
                if (MyPetEconomy.canUseEconomy() && getOwner().hasAutoRespawnEnabled() && respawnTime >= getOwner().getAutoRespawnMin() && MyPetPermissions.has(getOwner().getPlayer(), "MyPet.user.respawn"))
                {
                    double cost = respawnTime * MyPetConfiguration.RESPAWN_COSTS_FACTOR + MyPetConfiguration.RESPAWN_COSTS_FIXED;
                    if (MyPetEconomy.canPay(getOwner(), cost))
                    {
                        MyPetEconomy.pay(getOwner(), cost);
                        sendMessageToOwner(MyPetBukkitUtil.setColors(MyPetLocales.getString("Message.RespawnPaid", petOwner.getLanguage()).replace("%cost%", cost + " " + MyPetEconomy.getEconomy().currencyNameSingular()).replace("%petname%", petName)));
                        respawnTime = 1;
                    }
                }
                if (respawnTime <= 0)
                {
                    respawnPet();
                }
            }
            if (MyPetConfiguration.USE_HUNGER_SYSTEM && hunger > 1 && --hungerTime <= 0)
            {
                hunger--;
                hungerTime = MyPetConfiguration.HUNGER_SYSTEM_TIME;

                if (MyPetConfiguration.ENABLE_EVENTS)
                {
                    MyPetSpoutEvent spoutEvent = new MyPetSpoutEvent(this, MyPetSpoutEventReason.HungerChange);
                    getServer().getPluginManager().callEvent(spoutEvent);
                }
            }
        }
    }

    public MyPetPlayer getOwner()
    {
        return petOwner;
    }

    public void sendMessageToOwner(String text)
    {
        if (petOwner.isOnline())
        {
            getOwner().getPlayer().sendMessage(text);
        }
    }

    public static double getStartHP(Class<? extends MyPet> myPetClass)
    {
        if (startHP.containsKey(myPetClass))
        {
            return startHP.get(myPetClass);
        }
        return 1;
    }

    public static void setStartHP(Class<? extends MyPet> myPetClass, double hp)
    {
        startHP.put(myPetClass, hp);
    }

    public static double getStartSpeed(Class<? extends MyPet> myPetClass)
    {
        if (startSpeed.containsKey(myPetClass))
        {
            return startSpeed.get(myPetClass);
        }
        return 0.3F;
    }

    public static void setStartSpeed(Class<? extends MyPet> myPetClass, double speed)
    {
        startSpeed.put(myPetClass, speed);
    }

    public static List<Material> getFood(Class<? extends MyPet> myPetClass)
    {
        List<Material> foodList = new ArrayList<Material>();
        if (food.containsKey(myPetClass))
        {
            foodList.addAll(food.get(myPetClass));
        }
        return foodList;
    }

    public static void setFood(Class<? extends MyPet> myPetClass, Material foodToAdd)
    {
        if (food.containsKey(myPetClass))
        {
            List<Material> foodList = food.get(myPetClass);
            if (!foodList.contains(foodToAdd))
            {
                foodList.add(foodToAdd);
            }
        }
        else
        {
            List<Material> foodList = new ArrayList<Material>();
            foodList.add(foodToAdd);
            food.put(myPetClass, foodList);
        }
    }

    public static List<LeashFlag> getLeashFlags(Class<? extends MyPet> myPetClass)
    {
        List<LeashFlag> leashFlagList = new ArrayList<LeashFlag>();
        if (leashFlags.containsKey(myPetClass))
        {
            leashFlagList.addAll(leashFlags.get(myPetClass));
        }
        return leashFlagList;
    }

    public static boolean hasLeashFlag(Class<? extends MyPet> myPetClass, LeashFlag flag)
    {
        if (leashFlags.containsKey(myPetClass))
        {
            return leashFlags.get(myPetClass).contains(flag);
        }
        return false;
    }

    public static void setLeashFlags(Class<? extends MyPet> myPetClass, LeashFlag leashFlagToAdd)
    {
        if (leashFlags.containsKey(myPetClass))
        {
            List<LeashFlag> leashFlagList = leashFlags.get(myPetClass);
            if (!leashFlagList.contains(leashFlagToAdd))
            {
                leashFlagList.add(leashFlagToAdd);
            }
        }
        else
        {
            List<LeashFlag> leashFlagList = new ArrayList<LeashFlag>();
            leashFlagList.add(leashFlagToAdd);
            leashFlags.put(myPetClass, leashFlagList);
        }
    }

    public static float[] getEntitySize(Class<? extends EntityMyPet> entityMyPetClass)
    {
        EntitySize es = entityMyPetClass.getAnnotation(EntitySize.class);
        if (es != null)
        {
            return new float[]{es.height(), es.width()};
        }
        return new float[]{0, 0};
    }

    public boolean isPassiv()
    {
        return getDamage() == 0;
    }

    public boolean hasTarget()
    {
        return this.getStatus() == PetState.Here && craftMyPet.getHandle().getGoalTarget() != null && craftMyPet.getHandle().getGoalTarget().isAlive();
    }

    public abstract MyPetType getPetType();

    public CompoundTag getExtendedInfo()
    {
        return new CompoundTag("Info", new CompoundMap());
    }

    public void setExtendedInfo(CompoundTag info)
    {
    }

    @Override
    public CompoundTag save()
    {
        CompoundTag petNBT = new CompoundTag(null, new CompoundMap());

        CompoundTag locationNBT = new CompoundTag("Location", new CompoundMap());
        locationNBT.getValue().put("X", new DoubleTag("X", this.getLocation().getX()));
        locationNBT.getValue().put("Y", new DoubleTag("Y", this.getLocation().getY()));
        locationNBT.getValue().put("Z", new DoubleTag("Z", this.getLocation().getY()));
        locationNBT.getValue().put("Yaw", new FloatTag("Yaw", this.getLocation().getYaw()));
        locationNBT.getValue().put("Pitch", new FloatTag("Pitch", this.getLocation().getPitch()));
        locationNBT.getValue().put("World", new StringTag("World", this.getLocation().getWorld().getName()));

        petNBT.getValue().put("UUID", new StringTag("UUID", getUUID().toString()));
        petNBT.getValue().put("Type", new StringTag("Type", this.getPetType().getTypeName()));
        petNBT.getValue().put("Owner", new StringTag("Owner", this.petOwner.getName()));
        petNBT.getValue().put("Location", locationNBT);
        petNBT.getValue().put("Health", new DoubleTag("Health", this.health));
        petNBT.getValue().put("Respawntime", new IntTag("Respawntime", this.respawnTime));
        petNBT.getValue().put("Hunger", new IntTag("Hunger", this.hunger));
        petNBT.getValue().put("Name", new StringTag("Name", this.petName));
        petNBT.getValue().put("WorldGroup", new StringTag("WorldGroup", this.worldGroup));
        petNBT.getValue().put("Exp", new DoubleTag("Exp", this.getExp()));
        petNBT.getValue().put("Info", getExtendedInfo());
        if (this.skillTree != null)
        {
            petNBT.getValue().put("Skilltree", new StringTag("Skilltree", skillTree.getName()));
        }
        CompoundTag skillsNBT = new CompoundTag("Skills", new CompoundMap());
        Collection<ISkillInstance> skillList = this.getSkills().getSkills();
        if (skillList.size() > 0)
        {
            for (ISkillInstance skill : skillList)
            {
                if (skill instanceof ISkillStorage)
                {
                    ISkillStorage storageSkill = (ISkillStorage) skill;
                    CompoundTag s = storageSkill.save();
                    if (s != null)
                    {
                        skillsNBT.getValue().put(skill.getName(), s);
                    }
                }
            }
        }
        petNBT.getValue().put("Skills", skillsNBT);

        return petNBT;
    }

    @Override
    public void load(CompoundTag myPetNBT)
    {
    }

    public static int getCustomRespawnTimeFactor(Class<? extends MyPet> myPetClass)
    {
        if (customRespawnTimeFactor.containsKey(myPetClass))
        {
            return customRespawnTimeFactor.get(myPetClass);
        }
        return 0;
    }

    public static void setCustomRespawnTimeFactor(Class<? extends MyPet> myPetClass, int factor)
    {
        customRespawnTimeFactor.put(myPetClass, factor);
    }


    public static int getCustomRespawnTimeFixed(Class<? extends MyPet> myPetClass)
    {
        if (customRespawnTimeFixed.containsKey(myPetClass))
        {
            return customRespawnTimeFixed.get(myPetClass);
        }
        return 0;
    }

    public static void setCustomRespawnTimeFixed(Class<? extends MyPet> myPetClass, int factor)
    {
        customRespawnTimeFixed.put(myPetClass, factor);
    }

    public static void resetOptions()
    {
        customRespawnTimeFactor.clear();
        customRespawnTimeFixed.clear();
        leashFlags.clear();
        food.clear();
        startSpeed.clear();
        for (MyPetType petType : MyPetType.values())
        {
            startHP.put(petType.getMyPetClass(), 20D);
        }
    }

    @Override
    public String toString()
    {
        return "MyPet{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}
