/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.types.irongolem;

import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import static de.Keyle.MyPet.entity.types.MyPet.LeashFlag.UserCreated;
import static org.bukkit.Material.IRON_INGOT;

@MyPetInfo(food = {IRON_INGOT}, leashFlags = {UserCreated})
public class MyIronGolem extends MyPet {
    public static boolean CAN_THROW_UP = true;
    protected ItemStack flower;

    public MyIronGolem(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        if (hasFlower()) {
            info.getCompoundData().put("Flower", ItemStackNBTConverter.itemStackToCompund(CraftItemStack.asNMSCopy(getFlower())));
        }
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.containsKeyAs("Flower", TagCompound.class)) {
            TagCompound itemTag = info.get("Flower");
            ItemStack item = CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compundToItemStack(itemTag));
            setFlower(item);
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
        if (item != null && item.getType() != Material.RED_ROSE && item.getData().getData() == 0) {
            return;
        }
        this.flower = item;
        if (this.flower != null) {
            this.flower.setAmount(1);
        }
        if (status == PetState.Here) {
            ((EntityMyIronGolem) getCraftPet().getHandle()).setFlower(hasFlower());
        }
    }

    @Override
    public String toString() {
        return "MyIronGolem{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + "}";
    }
}