/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

package de.Keyle.MyPet.api.compat;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.KeyValue;

import java.util.LinkedList;
import java.util.List;

public class Compat<T> {

    List<KeyValue<String, T>> values = new LinkedList<>();

    KeyValue<String, T> foundValue = null;

    public Compat<T> v(String version, T value) {
        values.add(new KeyValue<>(version, value));
        return this;
    }

    public T get() {
        return foundValue.getValue();
    }

    public Compat<T> search() {
        for (KeyValue<String, T> pair : values) {
            if (MyPetApi.getCompatUtil().isCompatible(pair.getKey())) {
                if (foundValue != null) {
                    if (Util.versionCompare(foundValue.getKey(), pair.getKey()) < 0) {
                        foundValue = pair;
                    }
                } else {
                    foundValue = pair;
                }
            }
        }
        return this;
    }

    public T searchAndGet() {
        search();
        return get();
    }
}
