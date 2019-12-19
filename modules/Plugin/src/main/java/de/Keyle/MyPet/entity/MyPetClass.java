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

package de.Keyle.MyPet.entity;


import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.types.*;

import java.lang.reflect.Constructor;

public enum MyPetClass {
    Bat(MyBat.class),
    Bee(MyBee.class),
    Blaze(MyBlaze.class),
    Cat(MyCat.class),
    CaveSpider(MyCaveSpider.class),
    Chicken(MyChicken.class),
    Cod(MyCod.class),
    Cow(MyCow.class),
    Creeper(MyCreeper.class),
    Dolphin(MyDolphin.class),
    Donkey(MyDonkey.class),
    Drowned(MyDrowned.class),
    ElderGuardian(MyElderGuardian.class),
    EnderDragon(MyEnderDragon.class),
    Enderman(MyEnderman.class),
    Endermite(MyEndermite.class),
    Evoker(MyEvoker.class),
    Fox(MyFox.class),
    Ghast(MyGhast.class),
    Giant(MyGiant.class),
    Guardian(MyGuardian.class),
    Horse(MyHorse.class),
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
    PigZombie(MyPigZombie.class),
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
    Snowman(MySnowman.class),
    Spider(MySpider.class),
    Squid(MySquid.class),
    Stray(MyStray.class),
    TraderLlama(MyTraderLlama.class),
    TropicalFish(MyTropicalFish.class),
    Turtle(MyTurtle.class),
    WanderingTrader(MyWanderingTrader.class),
    Witch(MyWitch.class),
    Wither(MyWither.class),
    WitherSkeleton(MyWitherSkeleton.class),
    Wolf(MyWolf.class),
    Vex(MyVex.class),
    Villager(MyVillager.class),
    Vindicator(MyVindicator.class),
    Zombie(MyZombie.class),
    ZombieHorse(MyZombieHorse.class),
    ZombieVillager(MyZombieVillager.class);

    private Class<? extends MyPet> clazz;

    MyPetClass(Class<? extends MyPet> clazz) {
        this.clazz = clazz;
    }

    public static MyPetClass getByMyPetType(MyPetType type) {
        return valueOf(type.name());
    }

    public Class<? extends MyPet> getMyPetClass() {
        return clazz;
    }

    public MyPet getNewMyPetInstance(MyPetPlayer owner) {
        try {
            Constructor<?> ctor = clazz.getConstructor(MyPetPlayer.class);
            Object obj = ctor.newInstance(owner);
            if (obj instanceof MyPet) {
                return (MyPet) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}