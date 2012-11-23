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

import de.Keyle.MyPet.entity.types.cavespider.MyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.pigzombie.MyPigZombie;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.silverfish.MySilverfish;
import de.Keyle.MyPet.entity.types.slime.MySlime;
import de.Keyle.MyPet.entity.types.spider.MySpider;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.entity.types.zombie.MyZombie;
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.skill.MyPetSkillSystem;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.util.*;
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

    static
    {
        startHP.put(MyCaveSpider.class, 20);
        startHP.put(MyChicken.class, 20);
        startHP.put(MyCow.class, 20);
        startHP.put(MyIronGolem.class, 20);
        startHP.put(MyMooshroom.class, 20);
        startHP.put(MyOcelot.class, 20);
        startHP.put(MyPig.class, 20);
        startHP.put(MyPigZombie.class, 20);
        startHP.put(MySheep.class, 20);
        startHP.put(MySilverfish.class, 20);
        startHP.put(MySlime.class, 20);
        startHP.put(MySpider.class, 20);
        startHP.put(MyVillager.class, 20);
        startHP.put(MyWolf.class, 20);
        startHP.put(MyZombie.class, 20);

        startDamage.put(MyCaveSpider.class, 4);
        startDamage.put(MyChicken.class, 4);
        startDamage.put(MyCow.class, 4);
        startDamage.put(MyIronGolem.class, 4);
        startDamage.put(MyMooshroom.class, 4);
        startDamage.put(MyOcelot.class, 4);
        startDamage.put(MyPig.class, 4);
        startDamage.put(MyPigZombie.class, 4);
        startDamage.put(MySheep.class, 4);
        startDamage.put(MySilverfish.class, 4);
        startDamage.put(MySlime.class, 4);
        startDamage.put(MySpider.class, 4);
        startDamage.put(MyVillager.class, 4);
        startDamage.put(MyWolf.class, 4);
        startDamage.put(MyZombie.class, 4);
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
                if (!petLocation.getChunk().isLoaded())
                {
                    petLocation.getChunk().load();
                }
                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
                    return false;
                }
                craftMyPet = (CraftMyPet) petEntity.getBukkitEntity();
                status = PetState.Here;
                return true;
            }
        }
        return false;
    }

    public boolean createPet(Location loc)
    {
        if (status != PetState.Here && getOwner().isOnline())
        {
            if (respawnTime <= 0)
            {
                this.petLocation = loc;
                net.minecraft.server.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
                EntityMyPet petEntity = getPetType().getNewEntityInstance(mcWorld, this);
                petEntity.setLocation(loc);
                if (!petLocation.getChunk().isLoaded())
                {
                    petLocation.getChunk().load();
                }
                if (!mcWorld.addEntity(petEntity, CreatureSpawnEvent.SpawnReason.CUSTOM))
                {
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
        if (status == PetState.Here)
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
        if (startHP.containsKey(myPetClass))
        {
            startHP.put(myPetClass, hp);
        }
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
        if (startDamage.containsKey(myPetClass))
        {
            startDamage.put(myPetClass, damage);
        }
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

    public boolean isPassiv()
    {
        return getStartDamage(this.getClass()) == 0;
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
