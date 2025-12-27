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

package de.Keyle.MyPet.api.util.inventory.material;

import com.google.common.base.Objects;

public class LegacyIdData implements LegacyData {

    int id;
    short data;

    public LegacyIdData(int id, short data) {
        this.id = id;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    @Override
    public short getData() {
        return this.data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LegacyIdData that = (LegacyIdData) o;
        return id == that.id && data == that.data;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, data);
    }
}
