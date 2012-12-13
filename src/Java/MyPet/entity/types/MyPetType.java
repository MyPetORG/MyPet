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

import Java.MyPet.entity.types.bat.EntityMyBat;
import Java.MyPet.entity.types.bat.MyBat;
import Java.MyPet.entity.types.chicken.EntityMyChicken;
import Java.MyPet.entity.types.chicken.MyChicken;
import Java.MyPet.entity.types.cow.EntityMyCow;
import Java.MyPet.entity.types.cow.MyCow;
import Java.MyPet.entity.types.creeper.EntityMyCreeper;
import Java.MyPet.entity.types.enderman.EntityMyEnderman;
import Java.MyPet.entity.types.enderman.MyEnderman;
import Java.MyPet.entity.types.irongolem.EntityMyIronGolem;
import Java.MyPet.entity.types.irongolem.MyIronGolem;
import Java.MyPet.entity.types.magmacube.EntityMyMagmaCube;
import Java.MyPet.entity.types.magmacube.MyMagmaCube;
import Java.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import Java.MyPet.entity.types.mooshroom.MyMooshroom;
import Java.MyPet.entity.types.pig.EntityMyPig;
import Java.MyPet.entity.types.pigzombie.EntityMyPigZombie;
import Java.MyPet.entity.types.sheep.EntityMySheep;
import Java.MyPet.entity.types.sheep.MySheep;
import Java.MyPet.entity.types.silverfish.EntityMySilverfish;
import Java.MyPet.entity.types.silverfish.MySilverfish;
import Java.MyPet.entity.types.skeleton.EntityMySkeleton;
import Java.MyPet.entity.types.skeleton.MySkeleton;
import Java.MyPet.entity.types.slime.EntityMySlime;
import Java.MyPet.entity.types.slime.MySlime;
import Java.MyPet.entity.types.spider.EntityMySpider;
import Java.MyPet.entity.types.villager.EntityMyVillager;
import Java.MyPet.entity.types.villager.MyVillager;
import Java.MyPet.entity.types.wolf.MyWolf;
import Java.MyPet.entity.types.zombie.EntityMyZombie;
import Java.MyPet.entity.types.zombie.MyZombie;
import Java.MyPet.util.MyPetPlayer;
import Java.MyPet.util.MyPetUtil;
import Java.MyPet.entity.types.cavespider.EntityMyCaveSpider;
import Java.MyPet.entity.types.cavespider.MyCaveSpider;
import Java.MyPet.entity.types.creeper.MyCreeper;
import Java.MyPet.entity.types.ocelot.EntityMyOcelot;
import Java.MyPet.entity.types.ocelot.MyOcelot;
import Java.MyPet.entity.types.pig.MyPig;
import Java.MyPet.entity.types.pigzombie.MyPigZombie;
import Java.MyPet.entity.types.spider.MySpider;
import Java.MyPet.entity.types.wolf.EntityMyWolf;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.World;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;

public enum MyPetType
{
    Bat(EntityType.BAT, "Bat", EntityMyBat.class, MyBat.class),
    CaveSpider(EntityType.CAVE_SPIDER, "CaveSpider", EntityMyCaveSpider.class, MyCaveSpider.class),
    Chicken(EntityType.CHICKEN, "Chicken", EntityMyChicken.class, MyChicken.class),
    Cow(EntityType.COW, "Cow", EntityMyCow.class, MyCow.class),
    Creeper(EntityType.CREEPER, "Creeper", EntityMyCreeper.class, MyCreeper.class),
    Enderman(EntityType.ENDERMAN, "Enderman", EntityMyEnderman.class, MyEnderman.class),
    IronGolem(EntityType.IRON_GOLEM, "IronGolem", EntityMyIronGolem.class, MyIronGolem.class),
    MagmaCube(EntityType.MAGMA_CUBE, "MagmaCube", EntityMyMagmaCube.class, MyMagmaCube.class),
    Mooshroom(EntityType.MUSHROOM_COW, "Mooshroom", EntityMyMooshroom.class, MyMooshroom.class),
    Ocelot(EntityType.OCELOT, "Ocelot", EntityMyOcelot.class, MyOcelot.class),
    Pig(EntityType.PIG, "Pig", EntityMyPig.class, MyPig.class),
    PigZombie(EntityType.PIG_ZOMBIE, "PigZombie", EntityMyPigZombie.class, MyPigZombie.class),
    Sheep(EntityType.SHEEP, "Sheep", EntityMySheep.class, MySheep.class),
    Silverfish(EntityType.SILVERFISH, "Silverfish", EntityMySilverfish.class, MySilverfish.class),
    Skeleton(EntityType.SKELETON, "Skeleton", EntityMySkeleton.class, MySkeleton.class),
    Slime(EntityType.SLIME, "Slime", EntityMySlime.class, MySlime.class),
    Spider(EntityType.SPIDER, "Spider", EntityMySpider.class, MySpider.class),
    Wolf(EntityType.WOLF, "Wolf", EntityMyWolf.class, MyWolf.class),
    Villager(EntityType.VILLAGER, "Villager", EntityMyVillager.class, MyVillager.class),
    Zombie(EntityType.ZOMBIE, "Zombie", EntityMyZombie.class, MyZombie.class);

    private EntityType bukkitType;
    private String name;
    private Class<? extends EntityMyPet> entityClass;
    private Class<? extends MyPet> myPetClass;

    private MyPetType(EntityType bukkitType, String typeName, Class<? extends EntityMyPet> entityClass, Class<? extends MyPet> myPetClass)
    {
        this.bukkitType = bukkitType;
        this.name = typeName;
        this.entityClass = entityClass;
        this.myPetClass = myPetClass;
    }

    public static MyPetType getMyPetTypeByEntityType(EntityType type)
    {
        for (MyPetType myPetType : MyPetType.values())
        {
            if (myPetType.bukkitType == type)
            {
                return myPetType;
            }
        }
        return null;
    }

    public static MyPetType getMyPetTypeByEntityClass(Class<? extends EntityCreature> entityClass)
    {
        for (MyPetType myPetType : MyPetType.values())
        {
            if (myPetType.entityClass == entityClass)
            {
                return myPetType;
            }
        }
        return null;
    }

    public static MyPetType getMyPetTypeByName(String name)
    {
        for (MyPetType myPetType : MyPetType.values())
        {
            if (myPetType.name.equalsIgnoreCase(name))
            {
                return myPetType;
            }
        }
        return null;
    }

    public static boolean isLeashableEntityType(EntityType type)
    {
        for (MyPetType myPetType : MyPetType.values())
        {
            if (myPetType.bukkitType == type)
            {
                return true;
            }
        }
        return false;
    }

    public EntityType getEntityType()
    {
        return bukkitType;
    }

    public Class<? extends EntityMyPet> getEntityClass()
    {
        return entityClass;
    }

    public Class<? extends MyPet> getMyPetClass()
    {
        return myPetClass;
    }

    public String getTypeName()
    {
        return name;
    }

    public EntityMyPet getNewEntityInstance(World world, MyPet myPet)
    {
        EntityMyPet petEntity = null;

        try
        {
            Constructor<?> ctor = entityClass.getConstructor(World.class, MyPet.class);
            Object obj = ctor.newInstance(world, myPet);
            if (obj instanceof EntityMyPet)
            {
                petEntity = (EntityMyPet) obj;
            }
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().warning(entityClass.getName() + " is no valid MyPet(Entity)!");
            MyPetUtil.getDebugLogger().warning(entityClass.getName() + " is no valid MyPet(Entity)!");
            e.printStackTrace();
        }
        return petEntity;
    }

    public MyPet getNewMyPetInstance(MyPetPlayer owner)
    {
        MyPet pet = null;

        try
        {
            Constructor<?> ctor = myPetClass.getConstructor(MyPetPlayer.class);
            Object obj = ctor.newInstance(owner);
            if (obj instanceof MyPet)
            {
                pet = (MyPet) obj;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MyPetUtil.getLogger().warning(myPetClass.getName() + " is no valid MyPet!");
            MyPetUtil.getDebugLogger().warning(myPetClass.getName() + " is no valid MyPet!");
        }
        return pet;
    }
}