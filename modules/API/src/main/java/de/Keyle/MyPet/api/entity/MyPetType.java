/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.entity.types.MyBat;

public enum MyPetType {
    Bat("BAT", 65, MyBat.class),
    Blaze("BLAZE", 61, MyBat.class),
    CaveSpider("CAVE_SPIDER", 59, MyBat.class),
    Chicken("CHICKEN", 93, MyBat.class),
    Cow("COW", 92, MyBat.class),
    Creeper("CREEPER", 50, MyBat.class),
    EnderDragon("ENDER_DRAGON", 63, MyBat.class),
    Enderman("ENDERMAN", 58, MyBat.class),
    Endermite("ENDERMITE", 67, MyBat.class),
    Ghast("GHAST", 56, MyBat.class),
    Giant("GIANT", 53, MyBat.class),
    Guardian("GUARDIAN", 68, MyBat.class),
    Horse("HORSE", 100, MyBat.class, "EntityHorse"),
    IronGolem("IRON_GOLEM", 99, MyBat.class, "VillagerGolem"),
    MagmaCube("MAGMA_CUBE", 62, MyBat.class, "LavaSlime"),
    Mooshroom("MUSHROOM_COW", 96, MyBat.class, "MushroomCow"),
    Ocelot("OCELOT", 98, MyBat.class, "Ozelot"),
    Pig("PIG", 90, MyBat.class),
    PigZombie("PIG_ZOMBIE", 57, MyBat.class),
    Rabbit("RABBIT", 101, MyBat.class),
    Sheep("SHEEP", 91, MyBat.class),
    Silverfish("SILVERFISH", 60, MyBat.class),
    Skeleton("SKELETON", 51, MyBat.class),
    Slime("SLIME", 55, MyBat.class),
    Snowman("SNOWMAN", 97, MyBat.class, "SnowMan"),
    Spider("SPIDER", 52, MyBat.class),
    Squid("SQUID", 94, MyBat.class),
    Witch("WITCH", 66, MyBat.class),
    Wither("WITHER", 64, MyBat.class, "WitherBoss"),
    Wolf("WOLF", 95, MyBat.class),
    Villager("VILLAGER", 120, MyBat.class),
    Zombie("ZOMBIE", 54, MyBat.class);

    private String bukkitName;
    private String minecraftName;
    private int typeID;
    private Class<? extends ActiveMyPet> mypetClass;

    MyPetType(String typeName, int typeID, Class<? extends ActiveMyPet> mypetClass) {
        this.bukkitName = typeName;
        this.typeID = typeID;
        this.minecraftName = name();
        this.mypetClass = mypetClass;
    }

    MyPetType(String bukkitName, int typeID, Class<? extends ActiveMyPet> mypetClass, String minecraftName) {
        this.bukkitName = bukkitName;
        this.minecraftName = minecraftName;
        this.typeID = typeID;
        this.mypetClass = mypetClass;
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

    public Class<? extends ActiveMyPet> getMyPetClass() {
        return mypetClass;
    }

    public static MyPetType byEntityTypeName(String name) {
        for (MyPetType t : values()) {
            if (t.getBukkitName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public static MyPetType byName(String name) {
        MyPetType type = valueOf(name);
        if (type == null) {
            for (MyPetType t : values()) {
                if (t.name().equalsIgnoreCase(name)) {
                    return t;
                }
            }
        }
        return type;
    }
}