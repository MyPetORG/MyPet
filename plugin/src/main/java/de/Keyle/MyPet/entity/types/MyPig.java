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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyPig extends MyPet implements de.Keyle.MyPet.api.entity.types.MyPig {

    protected ItemStack saddle = null;
    protected String variant = "temperate";

    public MyPig(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        if (hasSaddle()) {
            info = info.put("Saddle", MyPetApi.getPlatformHelper().itemStackToCompound(getSaddle()));
        }
        info = info.putString("Variant", getVariant());
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("Saddle")) {
            if (info.get("Saddle") instanceof CompoundBinaryTag) {
                CompoundBinaryTag itemTag = info.getCompound("Saddle");
                try {
                    ItemStack item = MyPetApi.getPlatformHelper().compoundToItemStack(itemTag);
                    setSaddle(item);
                } catch (Exception e) {
                    MyPetApi.getLogger().warning("Could not load Saddle item from pet data!");
                }
            } else {
                boolean saddle = info.getBoolean("Saddle");
                if (saddle) {
                    ItemStack item = new ItemStack(Material.SADDLE);
                    setSaddle(item);
                } else {
                    // Explicit Saddle: false — clear any existing saddle.
                    // Needed for the post-interaction re-sync path: shearing a
                    // pig writes Saddle: false into the snapshot, and we need
                    // to propagate that to the MyPet field so it persists
                    // across despawn/respawn cycles.
                    setSaddle(null);
                }
            }
        }
        if (info.keySet().contains("Variant")) {
            setVariant(info.getString("Variant"));
        }
    }

    public void setSaddle(ItemStack item) {
        if (item != null && item.getType() != Material.SADDLE) {
            return;
        }

        this.saddle = item;
        if (this.saddle != null) {
            this.saddle.setAmount(1);
        }

        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public boolean hasSaddle() {
        return saddle != null;
    }

    @Override
    public void setVariant(String variant) {
        this.variant = variant;
    }
}