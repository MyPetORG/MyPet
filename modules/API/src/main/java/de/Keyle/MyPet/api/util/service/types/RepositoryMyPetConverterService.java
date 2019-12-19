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

package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagShort;
import de.keyle.knbt.TagString;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ServiceName("RepositoryMyPetConverterService")
public class RepositoryMyPetConverterService implements ServiceContainer {

    public enum Version {
        UNKNOWN,
        v1_7_R4,
        v1_8_R1,
        v1_8_R2,
        v1_8_R3,
        v1_9_R1,
        v1_9_R2,
        v1_10_R1,
        v1_11_R1,
        v1_12_R1,
        v1_13_R1,
        v1_13_R2,
        v1_14_R1,
        v1_15_R1,
    }

    Version toVersion;

    @Override
    public boolean onEnable() {
        try {
            toVersion = Version.valueOf(MyPetApi.getCompatUtil().getInternalVersion());
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    public void convert(StoredMyPet pet) {
        Version fromVersion = Version.v1_7_R4;

        TagCompound info = pet.getInfo();
        if (info.containsKey("Version")) {
            if (info.containsKeyAs("Version", TagString.class)) {
                fromVersion = Version.valueOf(info.getAs("Version", TagString.class).getStringData());
            } else if (info.containsKeyAs("Version", TagInt.class)) {
                fromVersion = Version.values()[info.getAs("Version", TagInt.class).getIntData() + 1];
            } else {
                fromVersion = Version.values()[info.getAs("Version", TagShort.class).getShortData()];
            }
        }

        for (Version v : Version.values()) {
            if (v.ordinal() <= fromVersion.ordinal()) {
                continue;
            }
            if (v.ordinal() > toVersion.ordinal()) {
                break;
            }
            try {
                Method m = this.getClass().getDeclaredMethod(v.name(), StoredMyPet.class);
                m.invoke(this, pet);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
    }
}
