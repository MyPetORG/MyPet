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
import de.keyle.knbt.TagCompound;
import lombok.Getter;


@DefaultInfo(food = {"apple"})
public interface MyVillager extends MyPet, MyPetBaby {

    enum Type {
        Desert("desert"),
        Jungle("jungle"),
        Plains("plains"),
        Savanna("savanna"),
        Snow("snow"),
        Swamp("swamp"),
        Taiga("taiga");

        @Getter String key;

        Type(String key) {
            this.key = key;
        }
    }

    enum Profession {
        NONE("none"),
        ARMORER("armorer"),
        BUTCHER("butcher"),
        CARTOGRAPHER("cartographer"),
        CLERIC("cleric"),
        FARMER("farmer"),
        FISHERMAN("fisherman"),
        FLETCHER("fletcher"),
        LEATHERWORKER("leatherworker"),
        LIBRARIAN("librarian"),
        MASON("mason"),
        NITWIT("nitwit"),
        SHEPHERD("shepherd"),
        TOOLSMITH("toolsmith"),
        WEAPONSMITH("weaponsmith");

        @Getter String key;

        Profession(String key) {
            this.key = key;
        }
    }

    int getProfession();

    void setProfession(int value);

    Type getType();

    void setType(Type value);

    int getVillagerLevel();

    void setVillagerLevel(int level);

    void setOriginalData(TagCompound compound);

    TagCompound getOriginalData();

    boolean hasOriginalData();
}