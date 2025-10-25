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

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.Material;

public class MaterialHolder {

    private String introduced;
    String id;
    LegacyNamedData legacyName = null;
    LegacyIdData legacyId = null;

    public MaterialHolder(String introduced, String id) {
        this.introduced = introduced;
        this.id = id;
    }

    public MaterialHolder(String introduced, String id, String legacyName, int legacyId, short legacyData) {
        this(introduced, id);
        this.legacyId = new LegacyIdData(legacyId, legacyData);
        this.legacyName = new LegacyNamedData(legacyName, legacyData);
    }

    public MaterialHolder(String introduced, String id, int legacyId, short legacyData) {
        this(introduced, id);
        this.legacyId = new LegacyIdData(legacyId, legacyData);
    }

    public String availableSince() {
        return introduced;
    }

    public String getId() {
        return id;
    }

    public LegacyNamedData getLegacyName() {
        return legacyName;
    }

    public LegacyIdData getLegacyId() {
        return legacyId;
    }

    public boolean hasLegacyName() {
        return legacyName != null;
    }

    public boolean hasLegacyId() {
        return legacyId != null;
    }

    public boolean isLegacy() {
        return hasLegacyId() || hasLegacyName();
    }

    public Material getMaterial() {
        return MyPetApi.getPlatformHelper().getMaterial(this);
    }
}