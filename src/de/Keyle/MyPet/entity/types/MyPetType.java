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

import de.Keyle.MyPet.entity.types.cavespider.EntityMyCaveSpider;
import de.Keyle.MyPet.entity.types.cavespider.MyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.irongolem.EntityMyIronGolem;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.sheep.EntityMySheep;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.silverfish.EntityMySilverfish;
import de.Keyle.MyPet.entity.types.silverfish.MySilverfish;
import de.Keyle.MyPet.entity.types.villager.EntityMyVillager;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.World;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;

public enum MyPetType
{
    Wolf(EntityType.WOLF, "Wolf", EntityMyWolf.class, MyWolf.class),
    IronGolem(EntityType.IRON_GOLEM, "IronGolem", EntityMyIronGolem.class, MyIronGolem.class),
    Silverfish(EntityType.SILVERFISH, "Silverfish", EntityMySilverfish.class, MySilverfish.class),
    Chicken(EntityType.CHICKEN, "Chicken", EntityMyChicken.class, MyChicken.class),
    Cow(EntityType.COW, "Cow", EntityMyCow.class, MyCow.class),
    Mooshroom(EntityType.MUSHROOM_COW, "Mooshroom", EntityMyMooshroom.class, MyMooshroom.class),
    Pig(EntityType.PIG, "Pig", EntityMyPig.class, MyPig.class),
    Sheep(EntityType.SHEEP, "Sheep", EntityMySheep.class, MySheep.class),
    Villager(EntityType.VILLAGER, "Villager", EntityMyVillager.class, MyVillager.class),
    CaveSpider(EntityType.CAVE_SPIDER, "CaveSpider", EntityMyCaveSpider.class, MyCaveSpider.class),
    Ocelot(EntityType.OCELOT, "Ocelot", EntityMyOcelot.class, MyOcelot.class);

    private EntityType entityType;
    private String typeName;
    private Class<? extends EntityMyPet> entityClazz;
    private Class<? extends MyPet> myPetClazz;

    private MyPetType(EntityType type, String name, Class<? extends EntityMyPet> entityClazz, Class<? extends MyPet> myPetClazz)
    {
        this.entityType = type;
        this.typeName = name;
        this.entityClazz = entityClazz;
        this.myPetClazz = myPetClazz;
    }

    public static MyPetType getMyPetTypeByEntityType(EntityType type)
    {
        for (MyPetType MPT : MyPetType.values())
        {
            if (MPT.entityType == type)
            {
                return MPT;
            }
        }
        return null;
    }

    public static boolean isLeashableEntityType(EntityType type)
    {
        for (MyPetType MPT : MyPetType.values())
        {
            if (MPT.entityType == type)
            {
                return true;
            }
        }
        return false;
    }

    public EntityType getEntityType()
    {
        return entityType;
    }

    public Class<? extends EntityMyPet> getEntityClass()
    {
        return entityClazz;
    }

    public Class<? extends MyPet> getMyPetClass()
    {
        return myPetClazz;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public EntityMyPet getNewEntityInstance(World world, MyPet myPet)
    {
        EntityMyPet pet = null;

        try
        {
            Constructor ctor = entityClazz.getConstructor(World.class, MyPet.class);
            Object obj = ctor.newInstance(world, myPet);
            if (obj instanceof EntityMyPet)
            {
                pet = (EntityMyPet) obj;
            }
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().warning(entityClazz.getName() + " is no valid MyPet(Entity)!");
            MyPetUtil.getDebugLogger().warning(entityClazz.getName() + " is no valid MyPet(Entity)!");
            e.printStackTrace();
        }
        return pet;
    }

    public MyPet getNewMyPetInstance(MyPetPlayer owner)
    {
        MyPet pet = null;

        try
        {
            Constructor ctor = myPetClazz.getConstructor(MyPetPlayer.class);
            Object obj = ctor.newInstance(owner);
            if (obj instanceof MyPet)
            {
                pet = (MyPet) obj;
            }
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().warning(myPetClazz.getName() + "is no valid MyPet!");
            MyPetUtil.getDebugLogger().warning(myPetClazz.getName() + "is no valid MyPet!");
        }
        return pet;
    }
}