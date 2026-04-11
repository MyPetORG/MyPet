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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.entity.MushroomCow;

@Getter
public class MyMooshroom extends MyPet implements de.Keyle.MyPet.api.entity.types.MyMooshroom {

    protected MushroomCow.Variant type = MushroomCow.Variant.RED;

    public MyMooshroom(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        // New format: stores the Bukkit enum name — "RED" or "BROWN".
        return info.putString("CowTypeName", getType().name());
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("CowTypeName")) {
            String name = info.getString("CowTypeName");
            if (name != null && !name.isEmpty()) {
                try {
                    setType(MushroomCow.Variant.valueOf(name));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    @Override
    public void setType(MushroomCow.Variant type) {
        if (type == null) return;
        this.type = type;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }
}
