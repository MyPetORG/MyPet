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

package de.Keyle.MyPet.api.entity.types;

import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBaby;
import org.bukkit.entity.Rabbit;

@DefaultInfo(food = {"carrot"})
public interface MyRabbit extends MyPet, MyPetBaby {
    enum RabbitType {
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

    RabbitType getVariant();

    void setVariant(RabbitType variant);
}