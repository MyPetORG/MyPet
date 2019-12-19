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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.compat.Compat;
import de.Keyle.MyPet.api.entity.types.*;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;

import java.util.LinkedList;
import java.util.List;

public enum MyPetType {
    Bat("BAT", "1.7.10", MyBat.class, new Compat<>()
            .v("1.7.10", 65)
            .v("1.13", "bat")
            .search()),
    Bee("BEE", "1.15", MyBee.class, new Compat<>()
            .v("1.13", "bee")
            .search()),
    Blaze("BLAZE", "1.7.10", MyBlaze.class, new Compat<>()
            .v("1.7.10", 61)
            .v("1.13", "blaze")
            .search()),
    Cat("CAT", "1.14", MyCat.class, new Compat<>()
            .v("1.14", "cat")
            .search()),
    CaveSpider("CAVE_SPIDER", "1.7.10", MyCaveSpider.class, new Compat<>()
            .v("1.7.10", 59)
            .v("1.13", "cave_spider")
            .search()),
    Chicken("CHICKEN", "1.7.10", MyChicken.class, new Compat<>()
            .v("1.7.10", 93)
            .v("1.13", "chicken")
            .search()),
    Cod("COD", "1.13", MyCod.class, new Compat<>()
            .v("1.13", "cod")
            .search()),
    Cow("COW", "1.7.10", MyCow.class, new Compat<>()
            .v("1.7.10", 92)
            .v("1.13", "cow")
            .search()),
    Creeper("CREEPER", "1.7.10", MyCreeper.class, new Compat<>()
            .v("1.7.10", 50)
            .v("1.13", "creeper")
            .search()),
    Dolphin("DOLPHIN", "1.13", MyDolphin.class, new Compat<>()
            .v("1.13", "dolphin")
            .search()),
    Donkey("DONKEY", "1.11", MyDonkey.class, new Compat<>()
            .v("1.7.10", 31)
            .v("1.13", "donkey")
            .search()),
    Drowned("DROWNED", "1.13", MyDrowned.class, new Compat<>()
            .v("1.13", "drowned")
            .search()),
    ElderGuardian("ELDER_GUARDIAN", "1.11", MyElderGuardian.class, new Compat<>()
            .v("1.7.10", 4)
            .v("1.13", "elder_guardian")
            .search()),
    EnderDragon("ENDER_DRAGON", "1.7.10", MyEnderDragon.class, new Compat<>()
            .v("1.7.10", 63)
            .v("1.13", "ender_dragon")
            .search()),
    Enderman("ENDERMAN", "1.7.10", MyEnderman.class, new Compat<>()
            .v("1.7.10", 58)
            .v("1.13", "enderman")
            .search()),
    Endermite("ENDERMITE", "1.8", MyEndermite.class, new Compat<>()
            .v("1.7.10", 67)
            .v("1.13", "endermite")
            .search()),
    Evoker("EVOKER", "1.11", MyEvoker.class, new Compat<>()
            .v("1.7.10", 34)
            .v("1.13", "evoker")
            .search()),
    Fox("FOX", "1.14", MyFox.class, new Compat<>()
            .v("1.14", "fox")
            .search()),
    Ghast("GHAST", "1.7.10", MyGhast.class, new Compat<>()
            .v("1.7.10", 56)
            .v("1.13", "ghast")
            .search()),
    Giant("GIANT", "1.7.10", MyGiant.class, new Compat<>()
            .v("1.7.10", 53)
            .v("1.13", "giant")
            .search()),
    Guardian("GUARDIAN", "1.8", MyGuardian.class, new Compat<>()
            .v("1.7.10", 68)
            .v("1.13", "guardian")
            .search()),
    Horse("HORSE", "1.7.10", MyHorse.class, new Compat<>()
            .v("1.7.10", 100)
            .v("1.13", "horse")
            .search()),
    Husk("HUSK", "1.11", MyHusk.class, new Compat<>()
            .v("1.7.10", 23)
            .v("1.13", "husk")
            .search()),
    Illusioner("ILLUSIONER", "1.12", MyIllusioner.class, new Compat<>()
            .v("1.7.10", 37)
            .v("1.13", "illusioner")
            .search()),
    IronGolem("IRON_GOLEM", "1.7.10", MyIronGolem.class, new Compat<>()
            .v("1.7.10", 99)
            .v("1.13", "iron_golem")
            .search()),
    Llama("LLAMA", "1.11", MyLlama.class, new Compat<>()
            .v("1.7.10", 103)
            .v("1.13", "llama")
            .search()),
    MagmaCube("MAGMA_CUBE", "1.7.10", MyMagmaCube.class, new Compat<>()
            .v("1.7.10", 62)
            .v("1.13", "magma_cube")
            .search()),
    Mooshroom("MUSHROOM_COW", "1.7.10", MyMooshroom.class, new Compat<>()
            .v("1.7.10", 96)
            .v("1.13", "mooshroom")
            .search()),
    Mule("MULE", "1.11", MyMule.class, new Compat<>()
            .v("1.7.10", 32)
            .v("1.13", "mule")
            .search()),
    Ocelot("OCELOT", "1.7.10", MyOcelot.class, new Compat<>()
            .v("1.7.10", 98)
            .v("1.13", "ocelot")
            .search()),
    Panda("PANDA", "1.14", MyPanda.class, new Compat<>()
            .v("1.14", "panda")
            .search()),
    Parrot("PARROT", "1.12", MyParrot.class, new Compat<>()
            .v("1.7.10", 105)
            .v("1.13", "parrot")
            .search()),
    Phantom("PHANTOM", "1.13", MyPhantom.class, new Compat<>()
            .v("1.13", "phantom")
            .search()),
    Pig("PIG", "1.7.10", MyPig.class, new Compat<>()
            .v("1.7.10", 90)
            .v("1.13", "pig")
            .search()),
    PigZombie("PIG_ZOMBIE", "1.7.10", MyPigZombie.class, new Compat<>()
            .v("1.7.10", 57)
            .v("1.13", "zombie_pigman")
            .search()),
    Pillager("PILLAGER", "1.14", MyPillager.class, new Compat<>()
            .v("1.14", "pillager")
            .search()),
    PolarBear("POLAR_BEAR", "1.10", MyPolarBear.class, new Compat<>()
            .v("1.7.10", 102)
            .v("1.13", "polar_bear")
            .search()),
    Pufferfish("PUFFERFISH", "1.13", MyPufferfish.class, new Compat<>()
            .v("1.13", "pufferfish")
            .search()),
    Rabbit("RABBIT", "1.8", MyRabbit.class, new Compat<>()
            .v("1.7.10", 101)
            .v("1.13", "rabbit")
            .search()),
    Ravager("RAVAGER", "1.14", MyRavager.class, new Compat<>()
            .v("1.14", "ravager")
            .search()),
    Salmon("SALMON", "1.13", MySalmon.class, new Compat<>()
            .v("1.13", "salmon")
            .search()),
    Sheep("SHEEP", "1.7.10", MySheep.class, new Compat<>()
            .v("1.7.10", 91)
            .v("1.13", "sheep")
            .search()),
    Silverfish("SILVERFISH", "1.7.10", MySilverfish.class, new Compat<>()
            .v("1.7.10", 60)
            .v("1.13", "silverfish")
            .search()),
    Skeleton("SKELETON", "1.7.10", MySkeleton.class, new Compat<>()
            .v("1.7.10", 51)
            .v("1.13", "skeleton")
            .search()),
    SkeletonHorse("SKELETON_HORSE", "1.11", MySkeletonHorse.class, new Compat<>()
            .v("1.7.10", 28)
            .v("1.13", "skeleton_horse")
            .search()),
    Slime("SLIME", "1.7.10", MySlime.class, new Compat<>()
            .v("1.7.10", 55)
            .v("1.13", "slime")
            .search()),
    Snowman("SNOWMAN", "1.7.10", MySnowman.class, new Compat<>()
            .v("1.7.10", 97)
            .v("1.13", "snow_golem")
            .search()),
    Spider("SPIDER", "1.7.10", MySpider.class, new Compat<>()
            .v("1.7.10", 52)
            .v("1.13", "spider")
            .search()),
    Squid("SQUID", "1.7.10", MySquid.class, new Compat<>()
            .v("1.7.10", 94)
            .v("1.13", "squid")
            .search()),
    Stray("STRAY", "1.11", MyStray.class, new Compat<>()
            .v("1.7.10", 6)
            .v("1.13", "stray")
            .search()),
    TraderLlama("TRADER_LLAMA", "1.14", MyTraderLlama.class, new Compat<>()
            .v("1.14", "trader_llama")
            .search()),
    TropicalFish("TROPICAL_FISH", "1.13", MyTropicalFish.class, new Compat<>()
            .v("1.13", "tropical_fish")
            .search()),
    Turtle("TURTLE", "1.13", MyTurtle.class, new Compat<>()
            .v("1.13", "turtle")
            .search()),
    Vex("VEX", "1.11", MyVex.class, new Compat<>()
            .v("1.7.10", 35)
            .v("1.13", "vex")
            .search()),
    Villager("VILLAGER", "1.7.10", MyVillager.class, new Compat<>()
            .v("1.7.10", 120)
            .v("1.13", "villager")
            .search()),
    Vindicator("VINDICATOR", "1.11", MyVindicator.class, new Compat<>()
            .v("1.7.10", 36)
            .v("1.13", "vindicator")
            .search()),
    WanderingTrader("WANDERING_TRADER", "1.14", MyWanderingTrader.class, new Compat<>()
            .v("1.14", "wandering_trader")
            .search()),
    Witch("WITCH", "1.7.10", MyWitch.class, new Compat<>()
            .v("1.7.10", 66)
            .v("1.13", "witch")
            .search()),
    Wither("WITHER", "1.7.10", MyWither.class, new Compat<>()
            .v("1.7.10", 64)
            .v("1.13", "wither")
            .search()),
    WitherSkeleton("WITHER_SKELETON", "1.11", MyWitherSkeleton.class, new Compat<>()
            .v("1.7.10", 5)
            .v("1.13", "wither_skeleton")
            .search()),
    Wolf("WOLF", "1.7.10", MyWolf.class, new Compat<>()
            .v("1.7.10", 95)
            .v("1.13", "wolf")
            .search()),
    Zombie("ZOMBIE", "1.7.10", MyZombie.class, new Compat<>()
            .v("1.7.10", 54)
            .v("1.13", "zombie")
            .search()),
    ZombieHorse("ZOMBIE_HORSE", "1.11", MyZombieHorse.class, new Compat<>()
            .v("1.7.10", 29)
            .v("1.13", "zombie_horse")
            .search()),
    ZombieVillager("ZOMBIE_VILLAGER", "1.11", MyZombieVillager.class, new Compat<>()
            .v("1.7.10", 27)
            .v("1.13", "zombie_villager")
            .search());

    private String bukkitName;
    private String minecraftVersion;
    private Object typeID;
    private Class<? extends MyPet> mypetClass;

    MyPetType(String bukkitName, String minecraftVersion, Class<? extends MyPet> mypetClass, Compat id) {
        this.bukkitName = bukkitName;
        this.typeID = id.get();
        this.mypetClass = mypetClass;
        this.minecraftVersion = minecraftVersion;
    }

    public String getBukkitName() {
        return bukkitName;
    }

    public Object getTypeID() {
        return typeID;
    }

    public Class<? extends MyPet> getMyPetClass() {
        return mypetClass;
    }

    public boolean checkMinecraftVersion() {
        return minecraftVersion == null || MyPetApi.getCompatUtil().compareWithMinecraftVersion(this.minecraftVersion) >= 0;
    }

    public static List<MyPetType> all() {
        List<MyPetType> all = new LinkedList<>();
        for (MyPetType t : values()) {
            if (t.checkMinecraftVersion()) {
                all.add(t);
            }
        }
        return all;
    }

    public static MyPetType byEntityTypeName(String name) {
        return byEntityTypeName(name, true);
    }

    public static MyPetType byEntityTypeName(String name, boolean versionCheck) {
        for (MyPetType t : values()) {
            if (t.getBukkitName().equalsIgnoreCase(name)) {
                if (!versionCheck || t.checkMinecraftVersion()) {
                    return t;
                }
                break;
            }
        }
        throw new MyPetTypeNotFoundException(name);
    }

    public static MyPetType byName(String name) {
        return byName(name, true);
    }

    public static MyPetType byName(String name, boolean versionCheck) {
        for (MyPetType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                if (!versionCheck || t.checkMinecraftVersion()) {
                    return t;
                }
                break;
            }
        }
        throw new MyPetTypeNotFoundException(name);
    }
}