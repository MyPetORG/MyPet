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
import de.Keyle.MyPet.api.entity.types.*;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;

import java.util.LinkedList;
import java.util.List;

public enum MyPetType {
    Bat("BAT", 65, MyBat.class),
    Blaze("BLAZE", 61, MyBlaze.class),
    Cat("CAT", 0, "1.14", MyCat.class),
    CaveSpider("CAVE_SPIDER", 59, MyCaveSpider.class),
    Chicken("CHICKEN", 93, MyChicken.class),
    Cod("COD", 0, "1.13", MyCod.class),
    Cow("COW", 92, MyCow.class),
    Creeper("CREEPER", 50, MyCreeper.class),
    Dolphin("DOLPHIN", 0, "1.13", MyDolphin.class),
    Donkey("DONKEY", 31, "1.11", MyDonkey.class),
    Drowned("DROWNED", 0, "1.13", MyDrowned.class),
    ElderGuardian("ELDER_GUARDIAN", 4, "1.11", MyElderGuardian.class),
    EnderDragon("ENDER_DRAGON", 63, MyEnderDragon.class),
    Enderman("ENDERMAN", 58, MyEnderman.class),
    Endermite("ENDERMITE", 67, "1.8", MyEndermite.class),
    Evoker("EVOKER", 34, "1.11", MyEvoker.class, "EvocationIllager"),
    Fox("FOX", 0, "1.14", MyFox.class),
    Ghast("GHAST", 56, MyGhast.class),
    Giant("GIANT", 53, MyGiant.class),
    Guardian("GUARDIAN", 68, "1.8", MyGuardian.class),
    Horse("HORSE", 100, MyHorse.class, "EntityHorse"),
    Husk("HUSK", 23, "1.11", MyHusk.class),
    Illusioner("ILLUSIONER", 37, "1.12", MyIllusioner.class, "IllusionIllager"),
    IronGolem("IRON_GOLEM", 99, MyIronGolem.class, "VillagerGolem"),
    Llama("LLAMA", 103, "1.11", MyLlama.class),
    MagmaCube("MAGMA_CUBE", 62, MyMagmaCube.class, "LavaSlime"),
    Mooshroom("MUSHROOM_COW", 96, MyMooshroom.class, "MushroomCow"),
    Mule("MULE", 32, "1.11", MyMule.class),
    Ocelot("OCELOT", 98, MyOcelot.class, "Ozelot"),
    Panda("PANDA", 0, "1.14", MyPanda.class),
    Parrot("PARROT", 105, "1.12", MyParrot.class),
    Phantom("PHANTOM", 0, "1.13", MyPhantom.class),
    Pig("PIG", 90, MyPig.class),
    PigZombie("PIG_ZOMBIE", 57, MyPigZombie.class),
    Pillager("PILLAGER", 0, "1.14", MyPillager.class),
    PolarBear("POLAR_BEAR", 102, "1.10", MyPolarBear.class),
    Pufferfish("PUFFERFISH", 0, "1.13", MyPufferfish.class),
    Rabbit("RABBIT", 101, "1.8", MyRabbit.class),
    Ravager("RAVAGER", 0, "1.14", MyRavager.class),
    Salmon("SALMON", 0, "1.13", MySalmon.class),
    Sheep("SHEEP", 91, MySheep.class),
    Silverfish("SILVERFISH", 60, MySilverfish.class),
    Skeleton("SKELETON", 51, MySkeleton.class),
    SkeletonHorse("SKELETON_HORSE", 28, "1.11", MySkeletonHorse.class),
    Slime("SLIME", 55, MySlime.class),
    Snowman("SNOWMAN", 97, MySnowman.class, "SnowMan"),
    Spider("SPIDER", 52, MySpider.class),
    Squid("SQUID", 94, MySquid.class),
    Stray("STRAY", 6, "1.11", MyStray.class),
    TraderLlama("TRADER_LLAMA", 0, "1.14", MyTraderLlama.class),
    TropicalFish("TROPICAL_FISH", 0, "1.13", MyTropicalFish.class),
    Turtle("TURTLE", 0, "1.13", MyTurtle.class),
    WanderingTrader("WANDERING_TRADER", 0, "1.14", MyWanderingTrader.class),
    Witch("WITCH", 66, MyWitch.class),
    Wither("WITHER", 64, MyWither.class, "WitherBoss"),
    WitherSkeleton("WITHER_SKELETON", 5, "1.11", MyWitherSkeleton.class),
    Wolf("WOLF", 95, MyWolf.class),
    Vex("VEX", 35, "1.11", MyVex.class),
    Villager("VILLAGER", 120, MyVillager.class),
    Vindicator("VINDICATOR", 36, "1.11", MyVindicator.class, "VindicationIllager"),
    Zombie("ZOMBIE", 54, MyZombie.class),
    ZombieHorse("ZOMBIE_HORSE", 29, "1.11", MyZombieHorse.class),
    ZombieVillager("ZOMBIE_VILLAGER", 27, "1.11", MyZombieVillager.class);

    private String bukkitName;
    private String minecraftName;
    private String minecraftVersion = null;
    private int typeID;
    private Class<? extends MyPet> mypetClass;

    MyPetType(String typeName, int typeID, Class<? extends MyPet> mypetClass) {
        this.bukkitName = typeName;
        this.typeID = typeID;
        this.minecraftName = name();
        this.mypetClass = mypetClass;
    }

    MyPetType(String typeName, int typeID, String minecraftVersion, Class<? extends MyPet> mypetClass) {
        this(typeName, typeID, mypetClass);
        this.minecraftVersion = minecraftVersion;
    }

    MyPetType(String bukkitName, int typeID, Class<? extends MyPet> mypetClass, String minecraftName) {
        this(bukkitName, typeID, mypetClass);
        this.minecraftName = minecraftName;
    }

    MyPetType(String bukkitName, int typeID, String minecraftVersion, Class<? extends MyPet> mypetClass, String minecraftName) {
        this(bukkitName, typeID, mypetClass);
        this.minecraftName = minecraftName;
        this.minecraftVersion = minecraftVersion;
    }

    public String getBukkitName() {
        return bukkitName;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public int getTypeID() {
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