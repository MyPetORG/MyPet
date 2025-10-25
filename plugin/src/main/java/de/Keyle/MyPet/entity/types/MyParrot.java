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

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;

public class MyParrot extends MyPet implements de.Keyle.MyPet.api.entity.types.MyParrot {
    int variant = 0;

    public MyParrot(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public double getYSpawnOffset() {
        return 1;
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Parrot;
    }

    @Override
    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = Math.min(4, Math.max(0, variant));
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Variant", new TagInt(getVariant()));
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("Variant")) {
            setVariant(info.getAs("Variant", TagInt.class).getIntData());
        }
    }

    @Override
    public String toString() {
        return "MyParrot{owner=" + getOwner().getName() +
                ", name=" + ChatColor.stripColor(petName) +
                ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() +
                ", lv=" + experience.getLevel() +
                ", status=" + status.name() +
                ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") +
                ", worldgroup=" + worldGroup +
                ", variant=" + variant +
                "}";
    }
}
