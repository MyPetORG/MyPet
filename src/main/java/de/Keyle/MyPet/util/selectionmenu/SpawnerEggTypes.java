/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util.selectionmenu;

import de.Keyle.MyPet.entity.types.MyPetType;

public enum SpawnerEggTypes {
    Bat(MyPetType.Bat, 65),
    Blaze(MyPetType.Blaze, 61),
    CaveSpider(MyPetType.CaveSpider, 59),
    Chicken(MyPetType.Chicken, 93),
    Cow(MyPetType.Cow, 92),
    Creeper(MyPetType.Creeper, 50),
    Enderman(MyPetType.Enderman, 58),
    Endermite(MyPetType.Endermite, 67),
    Ghast(MyPetType.Ghast, 56),
    Giant(MyPetType.Giant, 54, true),
    Guardian(MyPetType.Guardian, 68, true),
    Horse(MyPetType.Horse, 100),
    IronGolem(MyPetType.IronGolem, 60, true),
    MagmaCube(MyPetType.MagmaCube, 62),
    Mooshroom(MyPetType.Mooshroom, 96),
    Ocelot(MyPetType.Ocelot, 98),
    Pig(MyPetType.Pig, 90),
    PigZombie(MyPetType.PigZombie, 57),
    Rabbit(MyPetType.Rabbit, 101),
    Sheep(MyPetType.Sheep, 91),
    Silverfish(MyPetType.Silverfish, 60),
    Skeleton(MyPetType.Skeleton, 51),
    Slime(MyPetType.Slime, 55),
    Snowman(MyPetType.Snowman, 97),
    Spider(MyPetType.Spider, 52),
    Squid(MyPetType.Squid, 94),
    Witch(MyPetType.Witch, 66),
    Wither(MyPetType.Wither, 58, true),
    Wolf(MyPetType.Wolf, 95),
    Villager(MyPetType.Villager, 120),
    Zombie(MyPetType.Zombie, 54);

    MyPetType type;
    short color;
    boolean glowing;

    SpawnerEggTypes(MyPetType type, int color) {
        this(type, color, false);
    }

    SpawnerEggTypes(MyPetType type, int color, boolean glowing) {
        this.type = type;
        this.color = (short) color;
        this.glowing = glowing;
    }

    public static SpawnerEggTypes getEggType(MyPetType type) {
        for (SpawnerEggTypes eggType : values()) {
            if (eggType.type == type) {
                return eggType;
            }
        }
        return Zombie;
    }

    public short getColor() {
        return this.color;
    }

    public boolean isGlowing() {
        return glowing;
    }
}