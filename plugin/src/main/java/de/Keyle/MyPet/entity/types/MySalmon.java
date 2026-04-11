/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 */

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class MySalmon extends MyPet implements de.Keyle.MyPet.api.entity.types.MySalmon {

    protected int variant = 0;

    public MySalmon(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = Util.clamp(variant, 0, 2);
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        return info.putInt("Variant", variant);
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("Variant")) {
            setVariant(info.getInt("Variant"));
        }
    }
}
