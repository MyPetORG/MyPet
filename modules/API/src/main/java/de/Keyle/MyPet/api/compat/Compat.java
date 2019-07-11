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

package de.Keyle.MyPet.api.compat;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.util.KeyValue;

import java.util.LinkedList;
import java.util.List;

public class Compat<T> {

    List<KeyValue<String, CompatValueProvider<T>>> valueProviders = new LinkedList<>();
    CompatValueProvider<T> defaultValueProvider = null;

    KeyValue<String, T> foundValue = null;
    T defaultValue = null;

    public Compat(T value) {
        defaultValueProvider = () -> value;
    }

    public Compat(CompatValueProvider<T> func) {
        defaultValueProvider = func;
    }

    public Compat() {
    }

    public Compat<T> d(T value) {
        defaultValueProvider = () -> value;
        return this;
    }

    public Compat<T> v(String version, T value) {
        valueProviders.add(new KeyValue<>(version, () -> value));
        return this;
    }

    public Compat<T> v(String version, CompatValueProvider<T> func) {
        valueProviders.add(new KeyValue<>(version, func));
        return this;
    }

    public Compat<T> d(CompatValueProvider<T> func) {
        defaultValueProvider = func;
        return this;
    }

    public T get() {
        if (foundValue == null) {
            return defaultValue;
        }
        return foundValue.getValue();
    }

    public Compat<T> search() {
        for (KeyValue<String, CompatValueProvider<T>> pair : valueProviders) {
            if (MyPetApi.getCompatUtil().isCompatible(pair.getKey())) {
                if (foundValue != null) {
                    if (Util.versionCompare(foundValue.getKey(), pair.getKey()) < 0) {
                        foundValue = new KeyValue<>(pair.getKey(), pair.getValue().value());
                    }
                } else {
                    foundValue = new KeyValue<>(pair.getKey(), pair.getValue().value());
                }
            }
        }
        if (foundValue == null) {
            if (defaultValueProvider != null) {
                defaultValue = defaultValueProvider.value();
            }
        }
        return this;
    }

    public T searchAndGet() {
        search();
        return get();
    }

    public interface CompatValueProvider<T> {

        T value();
    }
}
