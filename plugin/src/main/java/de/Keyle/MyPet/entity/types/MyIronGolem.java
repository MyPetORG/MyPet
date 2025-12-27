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
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MyIronGolem extends MyPet implements de.Keyle.MyPet.api.entity.types.MyIronGolem {
    protected ItemStack flower;

    public MyIronGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        if (hasFlower()) {
            info.getCompoundData().put("Flower", MyPetApi.getPlatformHelper().itemStackToCompund(getFlower()));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKeyAs("Flower", TagCompound.class)) {
            TagCompound itemTag = info.get("Flower");
            try {
                ItemStack item = MyPetApi.getPlatformHelper().compundToItemStack(itemTag);
                setFlower(item);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Flower item from pet data!");
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.IronGolem;
    }

    public ItemStack getFlower() {
        return flower;
    }

    public boolean hasFlower() {
        return flower != null;
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

    @Override
    public String toString() {
        return "MyIronGolem{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + "}";
    }
}