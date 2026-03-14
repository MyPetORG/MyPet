/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_20_R1.util.inventory;

import net.kyori.adventure.nbt.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemStackNBTConverter {

    public static CompoundBinaryTag itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return itemStackToCompound(CraftItemStack.asNMSCopy(itemStack));
    }

    public static CompoundBinaryTag itemStackToCompound(ItemStack itemStack) {
        CompoundTag tagCompound = new CompoundTag();
        itemStack.save(tagCompound);
        return (CompoundBinaryTag) vanillaCompoundToCompound(tagCompound);
    }

    public static ItemStack compoundToItemStack(CompoundBinaryTag compound) {
        CompoundTag tagCompound = (CompoundTag) compoundToVanillaCompound(compound);
        return ItemStack.of(tagCompound);
    }

    public static org.bukkit.inventory.ItemStack compoundToBukkitItemStack(CompoundBinaryTag compound) {
        return CraftItemStack.asBukkitCopy(compoundToItemStack(compound));
    }

    public static Tag compoundToVanillaCompound(BinaryTag tag) {
        if (tag == null) {
            return null;
        }
        if (tag instanceof IntBinaryTag intTag) {
            return IntTag.valueOf(intTag.value());
        } else if (tag instanceof ShortBinaryTag shortTag) {
            return ShortTag.valueOf(shortTag.value());
        } else if (tag instanceof StringBinaryTag stringTag) {
            return StringTag.valueOf(stringTag.value());
        } else if (tag instanceof ByteBinaryTag byteTag) {
            return ByteTag.valueOf(byteTag.value());
        } else if (tag instanceof ByteArrayBinaryTag byteArrayTag) {
            return new ByteArrayTag(byteArrayTag.value());
        } else if (tag instanceof DoubleBinaryTag doubleTag) {
            return DoubleTag.valueOf(doubleTag.value());
        } else if (tag instanceof FloatBinaryTag floatTag) {
            return FloatTag.valueOf(floatTag.value());
        } else if (tag instanceof IntArrayBinaryTag intArrayTag) {
            return new IntArrayTag(intArrayTag.value());
        } else if (tag instanceof LongBinaryTag longTag) {
            return LongTag.valueOf(longTag.value());
        } else if (tag instanceof LongArrayBinaryTag longArrayTag) {
            return new LongArrayTag(longArrayTag.value());
        } else if (tag instanceof ListBinaryTag listTag) {
            ListTag nbtList = new ListTag();
            for (BinaryTag element : listTag) {
                nbtList.add(compoundToVanillaCompound(element));
            }
            return nbtList;
        } else if (tag instanceof CompoundBinaryTag compoundTag) {
            CompoundTag nbtCompound = new CompoundTag();
            for (String key : compoundTag.keySet()) {
                nbtCompound.put(key, compoundToVanillaCompound(compoundTag.get(key)));
            }
            return nbtCompound;
        } else if (tag instanceof EndBinaryTag) {
            return null;
        }
        throw new IllegalArgumentException("Not a valid tag type: " + tag.type());
    }

    public static BinaryTag vanillaCompoundToCompound(Tag vanillaTag) {
        if (vanillaTag == null) {
            return null;
        }
        return switch (vanillaTag.getId()) {
            case 1 -> ByteBinaryTag.byteBinaryTag(((ByteTag) vanillaTag).getAsByte());
            case 2 -> ShortBinaryTag.shortBinaryTag(((ShortTag) vanillaTag).getAsShort());
            case 3 -> IntBinaryTag.intBinaryTag(((IntTag) vanillaTag).getAsInt());
            case 4 -> LongBinaryTag.longBinaryTag(((LongTag) vanillaTag).getAsLong());
            case 5 -> FloatBinaryTag.floatBinaryTag(((FloatTag) vanillaTag).getAsFloat());
            case 6 -> DoubleBinaryTag.doubleBinaryTag(((DoubleTag) vanillaTag).getAsDouble());
            case 7 -> ByteArrayBinaryTag.byteArrayBinaryTag(((ByteArrayTag) vanillaTag).getAsByteArray());
            case 8 -> StringBinaryTag.stringBinaryTag(vanillaTag.getAsString());
            case 9 -> {
                ListTag tagList = (ListTag) vanillaTag;
                List<BinaryTag> elements = new ArrayList<>();
                for (Tag element : tagList) {
                    elements.add(vanillaCompoundToCompound(element));
                }
                yield ListBinaryTag.from(elements);
            }
            case 10 -> {
                CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
                CompoundTag tagCompound = ((CompoundTag) vanillaTag);
                for (String tagName : tagCompound.getAllKeys()) {
                    builder.put(tagName, vanillaCompoundToCompound(tagCompound.get(tagName)));
                }
                yield builder.build();
            }
            case 11 -> IntArrayBinaryTag.intArrayBinaryTag(((IntArrayTag) vanillaTag).getAsIntArray());
            case 12 -> LongArrayBinaryTag.longArrayBinaryTag(((LongArrayTag) vanillaTag).getAsLongArray());
            default -> null;
        };
    }
}
