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

    Type getCatType();

    void setCatType(Type value);

    /**
     * Get the plugin-owned ordinal for this cat's type for NMS conversion
     */
    int getCatTypeOrdinal();

    DyeColor getCollarColor();

    void setCollarColor(DyeColor value);

    boolean isTamed();

    void setTamed(boolean flag);

    /**
     * Map Bukkit Cat.Type to a stable plugin-owned ordinal used for NMS conversion and persistence.
     * This mirrors the ordering used by the plugin implementation to ensure consistency across versions.
     */
    static int getOwnTypeOrdinal(Type type) {
        if (type == null) {
            return 0; // default to TABBY
        }
        switch (type) {
            case TABBY:
                return 0;
            case BLACK:
                return 1;
            case RED:
                return 2;
            case SIAMESE:
                return 3;
            case BRITISH_SHORTHAIR:
                return 4;
            case CALICO:
                return 5;
            case PERSIAN:
                return 6;
            case RAGDOLL:
                return 7;
            case WHITE:
                return 8;
            case JELLIE:
                return 9;
            case ALL_BLACK:
                return 10;
            default:
                return 0; // fallback
        }
    }
}