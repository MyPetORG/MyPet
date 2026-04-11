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
import org.bukkit.entity.Rabbit;

@Getter
public class MyRabbit extends MyPet implements de.Keyle.MyPet.api.entity.types.MyRabbit {

    protected Rabbit.Type variant = Rabbit.Type.BROWN;

    public MyRabbit(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        // New format: stores the Bukkit enum name under a NEW key to avoid
        // colliding with the legacy byte-id Variant key.
        info = info.putString("VariantName", variant.name());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("VariantName")) {
            String name = info.getString("VariantName");
            if (name != null && !name.isEmpty()) {
                try {
                    setVariant(Rabbit.Type.valueOf(name));
                } catch (IllegalArgumentException ignored) {
                    // Unrecognised variant name — fall back to the default.
                }
            }
        }
    }

    public void setVariant(Rabbit.Type variant) {
        if (variant == null) return;
        this.variant = variant;

        if (status == PetState.Here) {
            updateVisuals();
        }
    }
}