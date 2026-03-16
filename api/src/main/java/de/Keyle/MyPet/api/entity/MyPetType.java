/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

import de.Keyle.MyPet.api.entity.types.*;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import org.bukkit.entity.EntityType;

import java.util.LinkedList;
import java.util.List;

public enum MyPetType {
    Axolotl(MyAxolotl.class),
    Allay(MyAllay.class),
    Armadillo(MyArmadillo.class),
    Bat(MyBat.class),
    Bee(MyBee.class),
    Blaze(MyBlaze.class),
    Bogged(MyBogged.class),
    Breeze(MyBreeze.class),
    Camel(MyCamel.class),
    Cat(MyCat.class),
    CaveSpider(MyCaveSpider.class),
    Chicken(MyChicken.class),
    Cod(MyCod.class),
    CopperGolem(MyCopperGolem.class),
    Cow(MyCow.class),
    Creeper(MyCreeper.class),
    Creaking(MyCreaking.class),
    Dolphin(MyDolphin.class),
    Donkey(MyDonkey.class),
    Drowned(MyDrowned.class),
    ElderGuardian(MyElderGuardian.class),
    EnderDragon(MyEnderDragon.class),
    Enderman(MyEnderman.class),
    Endermite(MyEndermite.class),
    Evoker(MyEvoker.class),
    Fox(MyFox.class),
    Frog(MyFrog.class),
    Ghast(MyGhast.class),
    Goat(MyGoat.class),
    Giant(MyGiant.class),
    GlowSquid(MyGlowSquid.class),
    Guardian(MyGuardian.class),
    Horse(MyHorse.class),
    Hoglin(MyHoglin.class),
    Husk(MyHusk.class),
    Illusioner(MyIllusioner.class),
    IronGolem(MyIronGolem.class),
    Llama(MyLlama.class),
    MagmaCube(MyMagmaCube.class),
    Mooshroom(MyMooshroom.class),
    Mule(MyMule.class),
    Ocelot(MyOcelot.class),
    Panda(MyPanda.class),
    Parrot(MyParrot.class),
    Phantom(MyPhantom.class),
    Pig(MyPig.class),
    Piglin(MyPiglin.class),
    PiglinBrute(MyPiglin.class),
    Pillager(MyPillager.class),
    PolarBear(MyPolarBear.class),
    Pufferfish(MyPufferfish.class),
    Rabbit(MyRabbit.class),
    Ravager(MyRavager.class),
    Salmon(MySalmon.class),
    Sheep(MySheep.class),
    Silverfish(MySilverfish.class),
    Skeleton(MySkeleton.class),
    SkeletonHorse(MySkeletonHorse.class),
    Slime(MySlime.class),
    Sniffer(MySniffer.class),
    SnowGolem(MySnowGolem.class),
    Spider(MySpider.class),
    Squid(MySquid.class),
    Stray(MyStray.class),
    Strider(MyStray.class),
    Tadpole(MyTadpole.class),
    TraderLlama(MyTraderLlama.class),
    TropicalFish(MyTropicalFish.class),
    Turtle(MyTurtle.class),
    Vex(MyVex.class),
    Villager(MyVillager.class),
    Vindicator(MyVindicator.class),
    Warden(MyWarden.class),
    WanderingTrader(MyWanderingTrader.class),
    Witch(MyWitch.class),
    Wither(MyWither.class),
    WitherSkeleton(MyWitherSkeleton.class),
    Wolf(MyWolf.class),
    Zoglin(MyZoglin.class),
    Zombie(MyZombie.class),
    ZombieHorse(MyZombieHorse.class),
    ZombifiedPiglin(MyZombifiedPiglin.class),
    ZombieVillager(MyZombieVillager.class);

    private final Class<? extends MyPet> mypetClass;

    MyPetType(Class<? extends MyPet> mypetClass) {
        this.mypetClass = mypetClass;
    }

    private static String camelToSnake(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
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

    public String getBukkitName() {
        return camelToSnake(name()).toUpperCase();
    }

    public String getTypeID() {
        return camelToSnake(name());
    }

    public Class<? extends MyPet> getMyPetClass() {
        return mypetClass;
    }

    public boolean checkMinecraftVersion() {
        try {
            EntityType.valueOf(getBukkitName());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns a MyPetType by name, or null if not found.
     * This method does NOT check Minecraft version compatibility.
     * Use this when you want to gracefully handle invalid/unknown pet types.
     *
     * @param name the name to look up (case-insensitive)
     * @return the MyPetType, or null if not found
     */
    public static MyPetType byNameOrNull(String name) {
        for (MyPetType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}
