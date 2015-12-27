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

package de.Keyle.MyPet.util;

import com.google.common.collect.Maps;
import de.keyle.knbt.TagBase;
import de.keyle.knbt.TagCompound;

import java.util.Map;

public class PluginStorage {
    private Map<String, TagBase> data = Maps.newHashMap();

    public void init(TagCompound rootTag) {
        this.data = rootTag.getCompoundData();
    }

    public TagCompound getStorage(Class clazz) {
        return (TagCompound) data.get(clazz.getName());
    }

    public TagCompound createAndGetStorage(Class clazz) {
        if (data.containsKey(clazz.getName())) {
            return (TagCompound) data.get(clazz.getName());
        } else {
            TagCompound newTag = new TagCompound();
            data.put(clazz.getName(), newTag);
            return newTag;
        }
    }

    public TagCompound save() {
        return new TagCompound(data);
    }
}