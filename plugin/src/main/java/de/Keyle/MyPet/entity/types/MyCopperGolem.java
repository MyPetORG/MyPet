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
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class MyCopperGolem extends MyPet implements de.Keyle.MyPet.api.entity.types.MyCopperGolem {

    protected OxidationState oxidationState = OxidationState.UNAFFECTED;
    protected boolean waxed = false;
    protected ItemStack poppy = null;

    public MyCopperGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("OxidationState", new TagString(oxidationState.name()));
        info.getCompoundData().put("Waxed", new TagByte(waxed ? 1 : 0));
        if (hasPoppy()) {
            info.getCompoundData().put("Poppy", MyPetApi.getPlatformHelper().itemStackToCompund(poppy));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("OxidationState")) {
            try {
                oxidationState = OxidationState.valueOf(info.getAs("OxidationState", TagString.class).getStringData());
            } catch (IllegalArgumentException e) {
                oxidationState = OxidationState.UNAFFECTED;
            }
        }
        if (info.containsKey("Waxed")) {
            waxed = info.getAs("Waxed", TagByte.class).getBooleanData();
        }
        if (info.containsKey("Poppy")) {
            TagCompound poppyCompound = info.getAs("Poppy", TagCompound.class);
            try {
                poppy = MyPetApi.getPlatformHelper().compundToItemStack(poppyCompound);
            } catch (Exception e) {
                poppy = null;
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.CopperGolem;
    }

    @Override
    public OxidationState getOxidationState() {
        return oxidationState;
    }

    @Override
    public void setOxidationState(OxidationState state) {
        if (state == null) {
            state = OxidationState.UNAFFECTED;
        }
        this.oxidationState = state;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public boolean isWaxed() {
        return waxed;
    }

    @Override
    public void setWaxed(boolean waxed) {
        this.waxed = waxed;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public ItemStack getPoppy() {
        return poppy;
    }

    @Override
    public boolean hasPoppy() {
        return poppy != null;
    }

    @Override
    public void setPoppy(ItemStack item) {
        this.poppy = item;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public String toString() {
        return "MyCopperGolem{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", oxidation=" + oxidationState + ", waxed=" + waxed + ", hasPoppy=" + hasPoppy() + "}";
    }
}
