/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
    Blaze(MyBlaze.class),
    CaveSpider(MyCaveSpider.class),
    Chicken(MyChicken.class),
    Cow(MyCow.class),
    Creeper(MyCreeper.class),
    EnderDragon(MyEnderDragon.class),
    Enderman(MyEnderman.class),
    Endermite(MyEndermite.class),
    Ghast(MyGhast.class),
    Giant(MyGiant.class),
    Guardian(MyGuardian.class),
    Horse(MyHorse.class),
    IronGolem(MyIronGolem.class),
    MagmaCube(MyMagmaCube.class),
    Mooshroom(MyMooshroom.class),
    Ocelot(MyOcelot.class),
    Pig(MyPig.class),
    PigZombie(MyPigZombie.class),
    PolarBear(MyPolarBear.class),
    Rabbit(MyRabbit.class),
    Sheep(MySheep.class),
    Silverfish(MySilverfish.class),
    Skeleton(MySkeleton.class),
    Slime(MySlime.class),
    Snowman(MySnowman.class),
    Spider(MySpider.class),
    Squid(MySquid.class),
    Witch(MyWitch.class),
    Wither(MyWither.class),
    Wolf(MyWolf.class),
    Villager(MyVillager.class),
    Zombie(MyZombie.class);

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