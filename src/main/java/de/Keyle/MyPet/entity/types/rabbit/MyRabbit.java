/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2014 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.types.rabbit;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.util.ConfigItem;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;
import org.bukkit.entity.Rabbit;

import static org.bukkit.Material.CARROT;
import static org.bukkit.Material.RED_ROSE;

@MyPetInfo(food = {CARROT, RED_ROSE})
public class MyRabbit extends MyPet implements IMyPetBaby {
    public static ConfigItem GROW_UP_ITEM;

    protected boolean isBaby = false;
    protected RabbitType variant = RabbitType.BROWN;

    public static enum RabbitType {
        BROWN(Rabbit.Type.BROWN, (byte) 0),
        WHITE(Rabbit.Type.WHITE, (byte) 1),
        BLACK(Rabbit.Type.BLACK, (byte) 2),
        BLACK_AND_WHITE(Rabbit.Type.BLACK_AND_WHITE, (byte) 3),
        GOLD(Rabbit.Type.GOLD, (byte) 4),
        SALT_AND_PEPPER(Rabbit.Type.SALT_AND_PEPPER, (byte) 5),
        THE_KILLER_BUNNY(Rabbit.Type.THE_KILLER_BUNNY, (byte) 99);

        Rabbit.Type type;
        byte id;

        RabbitType(Rabbit.Type type, byte id) {
            this.type = type;
            this.id = id;
        }

        public static RabbitType getTypeByID(byte id) {
            for (RabbitType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return BROWN;
        }

        public static RabbitType getTypeByBukkitEnum(Rabbit.Type bukkitType) {
            for (RabbitType type : values()) {
                if (type.type == bukkitType) {
                    return type;
                }
            }
            return BROWN;
        }

        public Rabbit.Type getBukkitType() {
            return type;
        }

        public byte getId() {
            return id;
        }
    }

    public MyRabbit(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Rabbit;
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        info.getCompoundData().put("Variant", new TagByte(variant.getId()));
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.containsKeyAs("Variant", TagInt.class)) {
            setVariant(RabbitType.getTypeByID(info.getAs("Variant", TagByte.class).getByteData()));
        }
        if (info.getCompoundData().containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyPig) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public RabbitType getVariant() {
        return variant;
    }

    public void setVariant(RabbitType variant) {
        this.variant = variant;

        if (status == PetState.Here) {
            ((EntityMyRabbit) getCraftPet().getHandle()).setVariant(variant.getId());
        }
    }

    @Override
    public String toString() {
        return "MyRabbit{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", baby=" + isBaby() + ", variant=" + variant + "}";
    }
}