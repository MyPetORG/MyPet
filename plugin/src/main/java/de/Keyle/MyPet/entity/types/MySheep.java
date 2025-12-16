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
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import lombok.Getter;
import lombok.Setter;
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
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
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
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Color", new TagByte(getColor().getDyeData()));
        info.getCompoundData().put("Sheared", new TagByte(isSheared()));
        info.getCompoundData().put("Rainbow", new TagByte(isRainbow()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKeyAs("Color", TagInt.class)) {
            setColor(DyeColor.getByDyeData((byte) info.getAs("Color", TagInt.class).getIntData()));
        } else if (info.containsKeyAs("Color", TagByte.class)) {
            setColor(DyeColor.getByDyeData(info.getAs("Color", TagByte.class).getByteData()));
        }
        if (info.containsKey("Sheared")) {
            setSheared(info.getAs("Sheared", TagByte.class).getBooleanData());
        }
        if (info.containsKey("Rainbow")) {
            setRainbow(info.getAs("Rainbow", TagByte.class).getBooleanData());
        }
    }

    public void setSheared(boolean flag) {
        this.sheared = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }
}