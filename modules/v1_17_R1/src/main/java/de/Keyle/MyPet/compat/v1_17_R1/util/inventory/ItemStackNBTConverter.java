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

package de.Keyle.MyPet.compat.v1_17_R1.util.inventory;

import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.keyle.knbt.*;
import de.keyle.knbt.TagType;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Compat("v1_17_R1")
public class ItemStackNBTConverter {

	private static final Field TAG_LIST_LIST = ReflectionUtil.getField(ListTag.class, "c"); //List-Field

    public static TagCompound itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return itemStackToCompound(CraftItemStack.asNMSCopy(itemStack));
    }

    public static TagCompound itemStackToCompound(ItemStack itemStack) {
        CompoundTag tagCompound = new CompoundTag();
        itemStack.save(tagCompound);
        return (TagCompound) vanillaCompoundToCompound(tagCompound);
    }

    public static ItemStack compoundToItemStack(TagCompound compound) {
        CompoundTag tagCompound = (CompoundTag) compoundToVanillaCompound(compound);
        return ItemStack.of(tagCompound);
    }

    public static Tag compoundToVanillaCompound(TagBase tag) {
        switch (TagType.getTypeById(tag.getTagTypeId())) {
            case Int:
                return IntTag.valueOf(((TagInt) tag).getIntData());
            case Short:
                return ShortTag.valueOf(((TagShort) tag).getShortData());
            case String:
                return StringTag.valueOf(((TagString) tag).getStringData());
            case Byte:
                return ByteTag.valueOf(((TagByte) tag).getByteData());
            case Byte_Array:
                return new ByteArrayTag(((TagByteArray) tag).getByteArrayData());
            case Double:
                return DoubleTag.valueOf(((TagDouble) tag).getDoubleData());
            case Float:
                return FloatTag.valueOf(((TagFloat) tag).getFloatData());
            case Int_Array:
                return new IntArrayTag(((TagIntArray) tag).getIntArrayData());
            case Long:
                return LongTag.valueOf(((TagLong) tag).getLongData());
            case List:
                TagList TagList = (TagList) tag;
                ListTag tagList = new ListTag();
                for (TagBase tagInList : TagList.getReadOnlyList()) {
                    tagList.add(compoundToVanillaCompound(tagInList));
                }
                return tagList;
            case Compound:
                TagCompound TagCompound = (TagCompound) tag;
                CompoundTag tagCompound = new CompoundTag();
                for (String name : TagCompound.getCompoundData().keySet()) {
                    tagCompound.put(name, compoundToVanillaCompound(TagCompound.getCompoundData().get(name)));
                }
                return tagCompound;
            case End:
                return null;
        }
        throw new IllegalArgumentException("Not a valid tag type");
    }

    public static TagBase vanillaCompoundToCompound(Tag vanillaTag) {
        switch (vanillaTag.getId()) {
            case 1:
                return new TagByte(((ByteTag) vanillaTag).getAsByte());
            case 2:
                return new TagShort(((ShortTag) vanillaTag).getAsShort());
            case 3:
                return new TagInt(((IntTag) vanillaTag).getAsInt());
            case 4:
                return new TagLong(((LongTag) vanillaTag).getAsLong());
            case 5:
                return new TagFloat(((FloatTag) vanillaTag).getAsFloat());
            case 6:
                return new TagDouble(((DoubleTag) vanillaTag).getAsDouble());
            case 7:
                return new TagByteArray(((ByteArrayTag) vanillaTag).getAsByteArray());
            case 8:
                return new TagString(vanillaTag.getAsString());
            case 9:
                ListTag tagList = (ListTag) vanillaTag;
                List compoundList = new ArrayList();
                try {
                    ArrayList list = (ArrayList) TAG_LIST_LIST.get(tagList);
                    for (Object aList : list) {
                        compoundList.add(vanillaCompoundToCompound((Tag) aList));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                return new TagList(compoundList);
            case 10:
                TagCompound compound = new TagCompound();
                CompoundTag tagCompound = ((CompoundTag) vanillaTag);
                Set<String> keys = tagCompound.getAllKeys();
                for (String tagName : keys) {
                    compound.getCompoundData().put(tagName, vanillaCompoundToCompound(tagCompound.get(tagName)));
                }
                return compound;
            case 11:
                return new TagIntArray(((TagIntArray) vanillaTag).getIntArrayData());
        }
        return null;
    }
}
