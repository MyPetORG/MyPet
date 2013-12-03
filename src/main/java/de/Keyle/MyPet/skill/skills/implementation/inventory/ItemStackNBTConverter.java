/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.skill.skills.implementation.inventory;

import net.minecraft.server.v1_7_R1.*;
import org.spout.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemStackNBTConverter {
    public static CompoundTag ItemStackToCompund(ItemStack itemStack) {
        return ItemStackToCompund(itemStack, "Item");
    }

    public static CompoundTag ItemStackToCompund(ItemStack itemStack, String tagName) {
        CompoundTag compound = new CompoundTag(tagName, new CompoundMap());

        compound.getValue().put("id", new ShortTag("id", (short) Item.b(itemStack.getItem())));
        compound.getValue().put("Count", new ByteTag("Count", (byte) itemStack.count));
        compound.getValue().put("Damage", new ShortTag("Damage", (short) itemStack.getData()));

        if (itemStack.tag != null) {
            compound.getValue().put("tag", VanillaCompoundToCompound(itemStack.tag));
        }
        return compound;
    }

    public static ItemStack CompundToItemStack(CompoundTag compound) {
        int id = ((ShortTag) compound.getValue().get("id")).getValue();
        int count = ((ByteTag) compound.getValue().get("Count")).getValue();
        int damage = ((ShortTag) compound.getValue().get("Damage")).getValue();

        ItemStack itemstack = new ItemStack(Item.d(id), count, damage);
        if (compound.getValue().containsKey("tag")) {
            CompoundTag compoundToConvert = (CompoundTag) compound.getValue().get("tag");
            itemstack.tag = (NBTTagCompound) CompoundToVanillaCompound(compoundToConvert);
        }
        return itemstack;
    }

    @SuppressWarnings("unchecked")
    public static NBTBase CompoundToVanillaCompound(Tag<?> tag) {
        switch (tag.getType()) {
            case TAG_INT:
                return new NBTTagInt(((IntTag) tag).getValue());
            case TAG_SHORT:
                return new NBTTagShort(((ShortTag) tag).getValue());
            case TAG_STRING:
                return new NBTTagString(((StringTag) tag).getValue());
            case TAG_BYTE:
                return new NBTTagByte(((ByteTag) tag).getValue());
            case TAG_BYTE_ARRAY:
                return new NBTTagByteArray(((ByteArrayTag) tag).getValue());
            case TAG_DOUBLE:
                return new NBTTagDouble(((DoubleTag) tag).getValue());
            case TAG_FLOAT:
                return new NBTTagFloat(((FloatTag) tag).getValue());
            case TAG_INT_ARRAY:
                return new NBTTagIntArray(((IntArrayTag) tag).getValue());
            case TAG_LONG:
                return new NBTTagLong(((LongTag) tag).getValue());
            case TAG_SHORT_ARRAY:
                short[] shortArray = ((ShortArrayTag) tag).getValue();
                int[] intArray = new int[shortArray.length];
                for (int i = 0; i < shortArray.length; i++) {
                    intArray[i] = shortArray[i];
                }
                return new NBTTagIntArray(intArray);
            case TAG_LIST:
                ListTag<Tag<?>> listTag = (ListTag<Tag<?>>) tag;
                NBTTagList tagList = new NBTTagList();
                for (Tag tagInList : listTag.getValue()) {
                    tagList.add(CompoundToVanillaCompound(tagInList));
                }
                return tagList;
            case TAG_COMPOUND:
                CompoundTag compoundTag = (CompoundTag) tag;
                NBTTagCompound tagCompound = new NBTTagCompound();
                for (String name : compoundTag.getValue().keySet()) {
                    tagCompound.set(name, CompoundToVanillaCompound(compoundTag.getValue().get(name)));
                }
                return tagCompound;
            case TAG_END:
                return null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Tag VanillaCompoundToCompound(NBTBase vanillaTag) {
        switch (vanillaTag.getTypeId()) {
            case 0:
                return new EndTag();
            case 1:
                return new ByteTag("", ((NBTTagByte) vanillaTag).f());
            case 2:
                return new ShortTag("", ((NBTTagShort) vanillaTag).e());
            case 3:
                return new IntTag("", ((NBTTagInt) vanillaTag).d());
            case 4:
                return new LongTag("", ((NBTTagLong) vanillaTag).c());
            case 5:
                return new FloatTag("", ((NBTTagFloat) vanillaTag).h());
            case 6:
                return new DoubleTag("", ((NBTTagDouble) vanillaTag).g());
            case 7:
                return new ByteArrayTag("", ((NBTTagByteArray) vanillaTag).c());
            case 8:
                return new StringTag("", ((NBTTagString) vanillaTag).a_());
            case 9:
                NBTTagList tagList = (NBTTagList) vanillaTag;
                List compoundList = new ArrayList();
                for (int i = 0; i < tagList.size(); i++) {
                    compoundList.add(VanillaCompoundToCompound(tagList.get(i)));
                }
                Class type;
                if (tagList.size() > 0) {
                    type = compoundList.get(compoundList.size() - 1).getClass();
                } else {
                    type = CompoundTag.class;
                }
                return new ListTag("", type, compoundList);
            case 10:
                CompoundTag compound = new CompoundTag("", new CompoundMap());
                NBTTagCompound tagCompound = ((NBTTagCompound) vanillaTag);
                Set<String> keys = tagCompound.c();
                for (String tagName : keys) {
                    compound.getValue().put(tagName, VanillaCompoundToCompound(tagCompound.get(tagName)));
                }
                return compound;
            case 11:
                return new IntArrayTag("", ((NBTTagIntArray) vanillaTag).c());
        }
        return null;
    }
}