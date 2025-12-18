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
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyIronGolem extends MyPet implements de.Keyle.MyPet.api.entity.types.MyIronGolem {

    protected ItemStack flower;

    public MyIronGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        if (hasFlower()) {
            info = info.put("Flower", MyPetApi.getPlatformHelper().itemStackToCompound(getFlower()));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("Flower")) {
            try {
                if (info.get("Flower").type().id() == 10) { // COMPOUND type
                    CompoundBinaryTag itemTag = info.getCompound("Flower");
                    try {
                        ItemStack item = MyPetApi.getPlatformHelper().compoundToItemStack(itemTag);
                        setFlower(item);
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("Could not load Flower item from pet data!");
                    }
                }
            } catch (Exception e) {
                // Ignore if can't determine type
            }
        }
    }

    public void setFlower(ItemStack item) {
        if (item != null && item.getType() != EnumSelector.find(Material.class, "RED_ROSE", "POPPY") && item.getData().getData() == 0) {
            return;
        }
        this.flower = item;
        if (this.flower != null) {
            this.flower.setAmount(1);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean hasFlower() {
        return flower != null;
    }
}