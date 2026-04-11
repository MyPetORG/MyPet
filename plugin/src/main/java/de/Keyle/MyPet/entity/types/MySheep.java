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
import lombok.Setter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.DyeColor;

@Getter
public class MySheep extends MyPet implements de.Keyle.MyPet.api.entity.types.MySheep {

    protected DyeColor color = DyeColor.WHITE;
    protected boolean sheared = false;
    @Setter
    protected boolean rainbow = false;

    public MySheep(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setColor(DyeColor color) {
        this.color = color;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public void schedule() {
        super.schedule();
        if (rainbow) {
            this.setColor(DyeColor.values()[(getColor().ordinal() + 1) % (DyeColor.values().length - 1)]);
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putByte("Color", getColor().getDyeData());
        info = info.putBoolean("Sheared", isSheared());
        info = info.putBoolean("Rainbow", isRainbow());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("Color")) {
            setColor(DyeColor.getByDyeData(info.getByte("Color")));
        }
        if (info.keySet().contains("Sheared")) {
            setSheared(info.getBoolean("Sheared"));
        }
        if (info.keySet().contains("Rainbow")) {
            setRainbow(info.getBoolean("Rainbow"));
        }
    }

    public void setSheared(boolean flag) {
        this.sheared = flag;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }
}