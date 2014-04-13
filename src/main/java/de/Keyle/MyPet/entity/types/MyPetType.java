/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.entity.types.bat.EntityMyBat;
import de.Keyle.MyPet.entity.types.bat.MyBat;
import de.Keyle.MyPet.entity.types.blaze.EntityMyBlaze;
import de.Keyle.MyPet.entity.types.blaze.MyBlaze;
import de.Keyle.MyPet.entity.types.cavespider.EntityMyCaveSpider;
import de.Keyle.MyPet.entity.types.cavespider.MyCaveSpider;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.creeper.EntityMyCreeper;
import de.Keyle.MyPet.entity.types.creeper.MyCreeper;
import de.Keyle.MyPet.entity.types.enderman.EntityMyEnderman;
import de.Keyle.MyPet.entity.types.enderman.MyEnderman;
import de.Keyle.MyPet.entity.types.ghast.EntityMyGhast;
import de.Keyle.MyPet.entity.types.ghast.MyGhast;
import de.Keyle.MyPet.entity.types.giant.EntityMyGiant;
import de.Keyle.MyPet.entity.types.giant.MyGiant;
import de.Keyle.MyPet.entity.types.horse.EntityMyHorse;
import de.Keyle.MyPet.entity.types.horse.MyHorse;
import de.Keyle.MyPet.entity.types.irongolem.EntityMyIronGolem;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.magmacube.EntityMyMagmaCube;
import de.Keyle.MyPet.entity.types.magmacube.MyMagmaCube;
import de.Keyle.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.pigzombie.EntityMyPigZombie;
import de.Keyle.MyPet.entity.types.pigzombie.MyPigZombie;
import de.Keyle.MyPet.entity.types.sheep.EntityMySheep;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.silverfish.EntityMySilverfish;
import de.Keyle.MyPet.entity.types.silverfish.MySilverfish;
import de.Keyle.MyPet.entity.types.skeleton.EntityMySkeleton;
import de.Keyle.MyPet.entity.types.skeleton.MySkeleton;
import de.Keyle.MyPet.entity.types.slime.EntityMySlime;
import de.Keyle.MyPet.entity.types.slime.MySlime;
import de.Keyle.MyPet.entity.types.snowman.EntityMySnowman;
import de.Keyle.MyPet.entity.types.snowman.MySnowman;
import de.Keyle.MyPet.entity.types.spider.EntityMySpider;
import de.Keyle.MyPet.entity.types.spider.MySpider;
import de.Keyle.MyPet.entity.types.squid.EntityMySquid;
import de.Keyle.MyPet.entity.types.squid.MySquid;
import de.Keyle.MyPet.entity.types.villager.EntityMyVillager;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.entity.types.witch.EntityMyWitch;
import de.Keyle.MyPet.entity.types.witch.MyWitch;
import de.Keyle.MyPet.entity.types.wither.EntityMyWither;
import de.Keyle.MyPet.entity.types.wither.MyWither;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.entity.types.zombie.EntityMyZombie;
import de.Keyle.MyPet.entity.types.zombie.MyZombie;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import net.minecraft.server.v1_7_R2.EntityCreature;
import net.minecraft.server.v1_7_R2.World;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;

public enum MyPetType {
    Bat(EntityType.BAT, "Bat", EntityMyBat.class, MyBat.class),
    Blaze(EntityType.BLAZE, "Blaze", EntityMyBlaze.class, MyBlaze.class),
    CaveSpider(EntityType.CAVE_SPIDER, "CaveSpider", EntityMyCaveSpider.class, MyCaveSpider.class),
    Chicken(EntityType.CHICKEN, "Chicken", EntityMyChicken.class, MyChicken.class),
    Cow(EntityType.COW, "Cow", EntityMyCow.class, MyCow.class),
    Creeper(EntityType.CREEPER, "Creeper", EntityMyCreeper.class, MyCreeper.class),
    Enderman(EntityType.ENDERMAN, "Enderman", EntityMyEnderman.class, MyEnderman.class),
    Ghast(EntityType.GHAST, "Ghast", EntityMyGhast.class, MyGhast.class),
    Giant(EntityType.GIANT, "Giant", EntityMyGiant.class, MyGiant.class),
    Horse(EntityType.HORSE, "Horse", EntityMyHorse.class, MyHorse.class),
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
    Snowman(EntityType.SNOWMAN, "Snowman", EntityMySnowman.class, MySnowman.class),
    Spider(EntityType.SPIDER, "Spider", EntityMySpider.class, MySpider.class),
    Squid(EntityType.SQUID, "Squid", EntityMySquid.class, MySquid.class),
    Witch(EntityType.WITCH, "Witch", EntityMyWitch.class, MyWitch.class),
    Wither(EntityType.WITHER, "Wither", EntityMyWither.class, MyWither.class),
    Wolf(EntityType.WOLF, "Wolf", EntityMyWolf.class, MyWolf.class),
    Villager(EntityType.VILLAGER, "Villager", EntityMyVillager.class, MyVillager.class),
    Zombie(EntityType.ZOMBIE, "Zombie", EntityMyZombie.class, MyZombie.class);

    private EntityType bukkitType;
    private String name;
    private Class<? extends EntityMyPet> entityClass;
    private Class<? extends MyPet> myPetClass;

    private MyPetType(EntityType bukkitType, String typeName, Class<? extends EntityMyPet> entityClass, Class<? extends MyPet> myPetClass) {
        this.bukkitType = bukkitType;
        this.name = typeName;
        this.entityClass = entityClass;
        this.myPetClass = myPetClass;
    }

    public Class<? extends EntityMyPet> getEntityClass() {
        return entityClass;
    }

    public EntityType getEntityType() {
        return bukkitType;
    }

    public Class<? extends MyPet> getMyPetClass() {
        return myPetClass;
    }

    public static MyPetType getMyPetTypeByEntityClass(Class<? extends EntityCreature> entityClass) {
        for (MyPetType myPetType : MyPetType.values()) {
            if (myPetType.entityClass == entityClass) {
                return myPetType;
            }
        }
        return null;
    }

    public static MyPetType getMyPetTypeByEntityType(EntityType type) {
        for (MyPetType myPetType : MyPetType.values()) {
            if (myPetType.bukkitType == type) {
                return myPetType;
            }
        }
        return null;
    }

    public static MyPetType getMyPetTypeByName(String name) {
        for (MyPetType myPetType : MyPetType.values()) {
            if (myPetType.name.equalsIgnoreCase(name)) {
                return myPetType;
            }
        }
        return null;
    }

    public EntityMyPet getNewEntityInstance(World world, MyPet myPet) {
        EntityMyPet petEntity = null;

        try {
            Constructor<?> ctor = entityClass.getConstructor(World.class, MyPet.class);
            Object obj = ctor.newInstance(world, myPet);
            if (obj instanceof EntityMyPet) {
                petEntity = (EntityMyPet) obj;
            }
        } catch (Exception e) {
            MyPetLogger.write(ChatColor.RED + entityClass.getName() + " is no valid MyPet(Entity)!");
            DebugLogger.warning(entityClass.getName() + " is no valid MyPet(Entity)!");
            e.printStackTrace();
        }
        return petEntity;
    }

    public MyPet getNewMyPetInstance(MyPetPlayer owner) {
        MyPet pet = null;

        try {
            Constructor<?> ctor = myPetClass.getConstructor(MyPetPlayer.class);
            Object obj = ctor.newInstance(owner);
            if (obj instanceof MyPet) {
                pet = (MyPet) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
            MyPetLogger.write(ChatColor.RED + myPetClass.getName() + " is no valid MyPet!");
            DebugLogger.warning(myPetClass.getName() + " is no valid MyPet!");
        }
        return pet;
    }

    public String getTypeName() {
        return name;
    }

    public static boolean isLeashableEntityType(EntityType type) {
        for (MyPetType myPetType : MyPetType.values()) {
            if (myPetType.bukkitType == type) {
                return true;
            }
        }
        return false;
    }
}