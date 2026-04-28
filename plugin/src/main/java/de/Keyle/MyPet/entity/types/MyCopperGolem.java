/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyCopperGolem extends MyPet implements de.Keyle.MyPet.api.entity.types.MyCopperGolem {

    protected OxidationState oxidationState = OxidationState.UNAFFECTED;
    protected boolean waxed = false;
    protected ItemStack poppy = null;
    protected long oxidationRemainingTicks = 0;

    public MyCopperGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("OxidationState", oxidationState.name())
                   .putBoolean("Waxed", waxed)
                   .putLong("OxidationRemainingTicks", oxidationRemainingTicks);
        if (hasPoppy()) {
            info = info.put("Poppy", MyPetApi.getPlatformHelper().itemStackToCompound(poppy));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("OxidationState")) {
            try {
                oxidationState = OxidationState.valueOf(info.getString("OxidationState"));
            } catch (IllegalArgumentException e) {
                oxidationState = OxidationState.UNAFFECTED;
            }
        }
        if (info.keySet().contains("Waxed")) {
            waxed = info.getBoolean("Waxed");
        }
        if (info.keySet().contains("OxidationRemainingTicks")) {
            oxidationRemainingTicks = Math.max(0L, info.getLong("OxidationRemainingTicks"));
        }
        if (info.keySet().contains("Poppy")) {
            CompoundBinaryTag poppyCompound = info.getCompound("Poppy");
            try {
                poppy = MyPetApi.getPlatformHelper().compoundToItemStack(poppyCompound);
            } catch (Exception e) {
                poppy = null;
            }
        }
    }

    @Override
    public void setOxidationState(OxidationState state) {
        if (state == null) {
            state = OxidationState.UNAFFECTED;
        }
        this.oxidationState = state;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public void setWaxed(boolean waxed) {
        this.waxed = waxed;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public void setPoppy(ItemStack item) {
        this.poppy = item;
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    @Override
    public void setOxidationRemainingTicks(long ticks) {
        this.oxidationRemainingTicks = Math.max(0L, ticks);
    }

    @Override
    public boolean hasPoppy() {
        return poppy != null;
    }
}
