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

package Java.MyPet.entity.types;

import Java.MyPet.entity.types.bat.MyBat;
import Java.MyPet.entity.types.cavespider.MyCaveSpider;
import Java.MyPet.entity.types.chicken.MyChicken;
import Java.MyPet.entity.types.cow.MyCow;
import Java.MyPet.entity.types.creeper.MyCreeper;
import Java.MyPet.entity.types.enderman.MyEnderman;
import Java.MyPet.entity.types.irongolem.MyIronGolem;
import Java.MyPet.entity.types.magmacube.MyMagmaCube;
import Java.MyPet.entity.types.mooshroom.MyMooshroom;
import Java.MyPet.entity.types.sheep.MySheep;
import Java.MyPet.entity.types.silverfish.MySilverfish;
import Java.MyPet.entity.types.skeleton.MySkeleton;
import Java.MyPet.entity.types.slime.MySlime;
import Java.MyPet.entity.types.spider.MySpider;
import Java.MyPet.entity.types.villager.MyVillager;
import Java.MyPet.entity.types.wolf.MyWolf;
import Java.MyPet.entity.types.zombie.MyZombie;
import Java.MyPet.event.MyPetSpoutEvent;
import Java.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import Java.MyPet.skill.MyPetExperience;
import Java.MyPet.skill.MyPetSkillSystem;
import Java.MyPet.skill.skills.MyPetGenericSkill;
import Java.MyPet.util.*;
import Java.MyPet.entity.types.ocelot.MyOcelot;
import Java.MyPet.entity.types.pig.MyPig;
import Java.MyPet.entity.types.pigzombie.MyPigZombie;
import Java.MyPet.skill.MyPetSkillTree;
import net.minecraft.server.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MyPet
{
    private static Map<Class<? extends MyPet>, Integer> startHP = new HashMap<Class<? extends MyPet>, Integer>();
    private static Map<Class<? extends MyPet>, Integer> startDamage = new HashMap<Class<? extends MyPet>, Integer>();
    private static Map<Class<? extends MyPet>, List<Material>> food = new HashMap<Class<? extends MyPet>, List<Material>>();
    private static Map<Class<? extends MyPet>, List<LeashFlag>> leashFlags = new HashMap<Class<? extends MyPet>, List<LeashFlag>>();
    private static Map<Class<? extends MyPet>, Float[]> entitySizes = new HashMap<Class<? extends MyPet>, Float[]>();

    static
    {
        for (MyPetType petType : MyPetType.values())
        {
            startHP.put(petType.getMyPetClass(), 20);
            startDamage.put(petType.getMyPetClass(), 4);
        }

        entitySizes.put(MyBat.class, new Float[]{0.5F, 0.9F});
        entitySizes.put(MyCaveSpider.class, new Float[]{0.7F, 0.5F});
        entitySizes.put(MyChicken.class, new Float[]{0.3F, 0.7F});
        entitySizes.put(MyCow.class, new Float[]{0.9F, 1.3F});
        entitySizes.put(MyCreeper.class, new Float[]{0.9F, 0.9F});
        entitySizes.put(MyEnderman.class, new Float[]{0.6F, 2.9F});
        entitySizes.put(MyIronGolem.class, new Float[]{1.4F, 2.9F});
        entitySizes.put(MyMagmaCube.class, new Float[]{0.6F, 0.6F});
        entitySizes.put(MyMooshroom.class, new Float[]{0.9F, 1.3F});
        entitySizes.put(MyOcelot.class, new Float[]{0.6F, 0.8F});
        entitySizes.put(MyPig.class, new Float[]{0.9F, 0.9F});
        entitySizes.put(MyPigZombie.class, new Float[]{0.9F, 0.9F});
        entitySizes.put(MySheep.class, new Float[]{0.9F, 1.3F});
        entitySizes.put(MySilverfish.class, new Float[]{0.3F, 0.7F});
        entitySizes.put(MySkeleton.class, new Float[]{0.6F, 0.6F});
        entitySizes.put(MySlime.class, new Float[]{0.6F, 0.6F});
        entitySizes.put(MySpider.class, new Float[]{1.4F, 0.9F});
        entitySizes.put(MyVillager.class, new Float[]{0.6F, 0.8F});
        entitySizes.put(MyWolf.class, new Float[]{0.6F, 0.8F});
        entitySizes.put(MyZombie.class, new Float[]{0.9F, 0.9F});
    }

    public static enum LeashFlag
    {
        Baby, Adult, LowHp, Tamed, UserCreated, None;

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

    public static enum PetState
    {
        Dead, Despawned, Here
    }

    protected CraftMyPet craftMyPet;
    public String petName = "Pet";
    protected final MyPetPlayer petOwner;
    protected int health;
    public int respawnTime = 0;
    public int hungerTime = 0;
    protected int hunger = 100;

    public PetState status = PetState.Despawned;

    protected Location petLocation;

    protected MyPetSkillTree skillTree = null;
    protected MyPetSkillSystem skillSystem;
    protected MyPetExperience experience;

    public MyPet(MyPetPlayer Owner)
    {
        this.petOwner = Owner;
        if (MyPetSkillTreeConfigLoader.getSkillTreeNames(this.getPetType()).size() > 0)
        {
            for (String skillTreeName : MyPetSkillTreeConfigLoader.getSkillTreeNames(this.getPetType()))
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyPet.custom.skilltree." + skillTreeName))
                {
                    this.skillTree = MyPetSkillTreeConfigLoader.getMobType(this.getPetType().getTypeName()).getSkillTree(skillTreeName);
                    break;
                }
            }
        }
        if (this.skillTree == null)
        {
            for (String skillTreeName : MyPetSkillTreeConfigLoader.getSkillTreeNames("default"))
            {
                if (MyPetPermissions.has(Owner.getPlayer(), "MyPet.custom.skilltree." + skillTreeName))
                {
                    this.skillTree = MyPetSkillTreeConfigLoader.getMobType("default").getSkillTree(skillTreeName);
                    break;
                }
            }
        }
        if (this.skillTree == null)
        {
            this.skillTree = new MyPetSkillTree("%+-%NoNe%-+%");
        }
        skillSystem = new MyPetSkillSystem(this);
        experience = new MyPetExperience(this);
        hungerTime = MyPetConfig.hungerSystemTime;
    }

    public void setPetName(String newName)
    {
        this.petName = newName;
        MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(this, MyPetSpoutEventReason.Name));
    }

    public void removePet()
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
            craftMyPet.remove();
        }
    }

    protected void respawnPet()
    {
        if (status != PetState.Here && getOwner().isOnline())
        {
            petLocation = getOwner().getPlayer().getLocation();
            sendMessageToOwner(MyPetUtil.setColors(MyPetLanguage.getString("Msg_OnRespawn")).replace("%petname%", petName));
            createPet();
            respawnTime = 0;
            if (MyPetConfig.hungerSystem)
            {
                setHealth((int) Math.ceil(getMaxHealth() / 100. * (hunger + 1 - (hunger % 10))));
            }
            else
            {
                setHealth(getMaxHealth());
            }
        }
    }

    public boolean createPet()
    {
        if (status != PetState.Here && getOwner().isOnline())
        {
            if (respawnTime <= 0)
            {
                net.minecraft.server.World mcWorld = ((CraftWorld) petLocation.getWorld()).getHandle();
                EntityMyPet petEntity = getPetType().getNewEntityInstance(mcWorld, this);
                petEntity.setLocation(petLocation);
                if (!MyPetUtil.canSpawn(petLocation, petEntity))
                {
                    return false;
                }
                if (!petLocation.getChunk().isLoaded())
                {
                    petLocation.getChunk().load();
                }
                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    status = PetState.Despawned;
                    return false;
                }
                craftMyPet = (CraftMyPet) petEntity.getBukkitEntity();
                status = PetState.Here;
                return true;
            }
        }
        return false;
    }

    public CraftMyPet getCraftPet()
    {
        return craftMyPet;
    }

    public void setHealth(int d)
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

    public int getHealth()
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

    public int getMaxHealth()
    {
        return getStartHP(this.getClass()) + (skillSystem.hasSkill("HP") ? skillSystem.getSkill("HP").getLevel() : 0);
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
        hungerTime = MyPetConfig.hungerSystemTime;
    }

    public int getDamage()
    {
        return MyPet.getStartDamage(this.getClass()) + (getSkillSystem().hasSkill("Damage") ? getSkillSystem().getSkillLevel("Damage") : 0);
    }

    public MyPetSkillSystem getSkillSystem()
    {
        return skillSystem;
    }

    public MyPetExperience getExperience()
    {
        return experience;
    }

    public MyPetSkillTree getSkillTree()
    {
        return skillTree;
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
        if (status == PetState.Here && MyPetUtil.canSpawn(loc, this.craftMyPet.getHandle()))
        {
            craftMyPet.teleport(loc);
        }
    }

    public void scheduleTask()
    {
        if (status != PetState.Despawned && getOwner().isOnline())
        {
            if (skillSystem.getSkills().size() > 0)
            {
                for (MyPetGenericSkill skill : skillSystem.getSkills())
                {
                    skill.schedule();
                }
            }
            if (status == PetState.Dead)
            {
                respawnTime--;
                if (respawnTime <= 0)
                {
                    respawnPet();
                }
            }
            if (MyPetConfig.hungerSystem && hunger > 1 && hungerTime-- <= 0)
            {
                hunger--;
                hungerTime = MyPetConfig.hungerSystemTime;
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

    public static int getStartHP(Class<? extends MyPet> myPetClass)
    {
        if (startHP.containsKey(myPetClass))
        {
            return startHP.get(myPetClass);
        }
        return 1;
    }

    public static void setStartHP(Class<? extends MyPet> myPetClass, int hp)
    {
        startHP.put(myPetClass, hp);
    }

    public static int getStartDamage(Class<? extends MyPet> myPetClass)
    {
        if (startDamage.containsKey(myPetClass))
        {
            return startDamage.get(myPetClass);
        }
        return 1;
    }

    public static void setStartDamage(Class<? extends MyPet> myPetClass, int damage)
    {
        startDamage.put(myPetClass, damage);
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

    public static Float[] getEntitySize(Class<? extends MyPet> myPetClass)
    {
        if (entitySizes.containsKey(myPetClass))
        {
            return entitySizes.get(myPetClass);
        }
        return null;
    }

    public boolean isPassiv()
    {
        return getDamage() == 0;
    }

    public abstract MyPetType getPetType();

    public NBTTagCompound getExtendedInfo()
    {
        return new NBTTagCompound("Info");
    }

    public void setExtendedInfo(NBTTagCompound info)
    {
    }

    @Override
    public String toString()
    {
        return "MyPet{owner=" + getOwner().getName() + ", name=" + petName + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + skillTree.getName() + "}";
    }
}
