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
import org.bukkit.DyeColor;
import org.bukkit.entity.Cat.Type;

@DefaultInfo(food = {"cod"}, leashFlags = {"Tamed"})
public interface MyCat extends MyPet, MyPetBaby {

    /**
     * Stable cat type mapping that won't change across Minecraft versions.
     * Bukkit's Cat.Type enum ordering has changed in some still-supported versions,
     * so we maintain our own stable ordinal mapping for persistence until versions
     * using integer IDs are dropped.
     */
    enum OwnCatType {
        TABBY(Type.TABBY),
        BLACK(Type.BLACK),
        RED(Type.RED),
        SIAMESE(Type.SIAMESE),
        BRITISH_SHORTHAIR(Type.BRITISH_SHORTHAIR),
        CALICO(Type.CALICO),
        PERSIAN(Type.PERSIAN),
        RAGDOLL(Type.RAGDOLL),
        WHITE(Type.WHITE),
        JELLIE(Type.JELLIE),
        ALL_BLACK(Type.ALL_BLACK);

        private final Type bukkitType;

        OwnCatType(Type type) {
            this.bukkitType = type;
        }

        public Type getBukkitType() {
            return bukkitType;
        }
    }

    /**
     * Returns a stable ordinal for the given cat type that won't change across Minecraft versions.
     *
     * @param type the Bukkit Cat.Type to convert
     * @return a stable ordinal value (0-10) that can be safely persisted
     */
    static int getOwnTypeOrdinal(Type type) {
        if (type == null) return 0;
        for (OwnCatType ownType : OwnCatType.values()) {
            if (ownType.getBukkitType() == type) {
                return ownType.ordinal();
            }
        }
        return 0; // fallback to TABBY
    }

    Type getCatType();

    void setCatType(Type value);

    DyeColor getCollarColor();

    void setCollarColor(DyeColor value);

    boolean isTamed();

    void setTamed(boolean flag);
}