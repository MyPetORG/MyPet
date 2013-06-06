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

import net.minecraft.server.v1_5_R3.*;
import org.spout.nbt.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemStackNBTConverter
{
    public static CompoundTag ItemStackToCompund(ItemStack itemStack)
    {
        return ItemStackToCompund(itemStack, "Item");
    }

    public static CompoundTag ItemStackToCompund(ItemStack itemStack, String tagName)
    {
        CompoundTag compound = new CompoundTag(tagName, new CompoundMap());

        compound.getValue().put("id", new ShortTag("id", (short) itemStack.id));
        compound.getValue().put("Count", new ByteTag("Count", (byte) itemStack.count));
        compound.getValue().put("Damage", new ShortTag("Damage", (short) itemStack.getData()));

        if (itemStack.tag != null)
        {
            compound.getValue().put("tag", VanillaCompoundToCompound(itemStack.tag.setName("tag")));
        }
        return compound;
    }

    public static ItemStack CompundToItemStack(CompoundTag compound)
    {
        int id = ((ShortTag) compound.getValue().get("id")).getValue();
        int count = ((ByteTag) compound.getValue().get("Count")).getValue();
        int damage = ((ShortTag) compound.getValue().get("Damage")).getValue();

        ItemStack itemstack = new ItemStack(id, count, damage);
        if (compound.getValue().containsKey("tag"))
        {
            CompoundTag compoundToConvert = (CompoundTag) compound.getValue().get("tag");
            itemstack.tag = (NBTTagCompound) CompoundToVanillaCompound(compoundToConvert);
        }
        return itemstack;
    }

    @SuppressWarnings("unchecked")
    public static NBTBase CompoundToVanillaCompound(Tag<?> tag)
    {
        switch (tag.getType())
        {
            case TAG_INT:
                return new NBTTagInt(tag.getName(), ((IntTag) tag).getValue());
            case TAG_SHORT:
                return new NBTTagShort(tag.getName(), ((ShortTag) tag).getValue());
            case TAG_STRING:
                return new NBTTagString(tag.getName(), ((StringTag) tag).getValue());
            case TAG_BYTE:
                return new NBTTagByte(tag.getName(), ((ByteTag) tag).getValue());
            case TAG_BYTE_ARRAY:
                return new NBTTagByteArray(tag.getName(), ((ByteArrayTag) tag).getValue());
            case TAG_DOUBLE:
                return new NBTTagDouble(tag.getName(), ((DoubleTag) tag).getValue());
            case TAG_FLOAT:
                return new NBTTagFloat(tag.getName(), ((FloatTag) tag).getValue());
            case TAG_INT_ARRAY:
                return new NBTTagIntArray(tag.getName(), ((IntArrayTag) tag).getValue());
            case TAG_LONG:
                return new NBTTagLong(tag.getName(), ((LongTag) tag).getValue());
            case TAG_SHORT_ARRAY:
                short[] shortArray = ((ShortArrayTag) tag).getValue();
                int[] intArray = new int[shortArray.length];
                for (int i = 0 ; i < shortArray.length ; i++)
                {
                    intArray[i] = shortArray[i];
                }
                return new NBTTagIntArray(tag.getName(), intArray);
            case TAG_LIST:
                ListTag<Tag<?>> listTag = (ListTag<Tag<?>>) tag;
                NBTTagList tagList = new NBTTagList(listTag.getName());
                for (Tag tagInList : listTag.getValue())
                {
                    tagList.add(CompoundToVanillaCompound(tagInList));
                }
                return tagList;
            case TAG_COMPOUND:
                CompoundTag compoundTag = (CompoundTag) tag;
                NBTTagCompound tagCompound = new NBTTagCompound(tag.getName());
                for (String name : compoundTag.getValue().keySet())
                {
                    tagCompound.set(name, CompoundToVanillaCompound(compoundTag.getValue().get(name)));
                }
                return tagCompound;
            case TAG_END:
                return new NBTTagEnd();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Tag VanillaCompoundToCompound(NBTBase vanillaTag)
    {
        switch (vanillaTag.getTypeId())
        {
            case 0:
                return new EndTag();
            case 1:
                return new ByteTag(vanillaTag.getName(), ((NBTTagByte) vanillaTag).data);
            case 2:
                return new ShortTag(vanillaTag.getName(), ((NBTTagShort) vanillaTag).data);
            case 3:
                return new IntTag(vanillaTag.getName(), ((NBTTagInt) vanillaTag).data);
            case 4:
                return new LongTag(vanillaTag.getName(), ((NBTTagLong) vanillaTag).data);
            case 5:
                return new FloatTag(vanillaTag.getName(), ((NBTTagFloat) vanillaTag).data);
            case 6:
                return new DoubleTag(vanillaTag.getName(), ((NBTTagDouble) vanillaTag).data);
            case 7:
                return new ByteArrayTag(vanillaTag.getName(), ((NBTTagByteArray) vanillaTag).data);
            case 8:
                return new StringTag(vanillaTag.getName(), ((NBTTagString) vanillaTag).data);
            case 9:
                NBTTagList tagList = (NBTTagList) vanillaTag;
                List<Tag<?>> compoundList = new ArrayList<Tag<?>>();
                for (int i = 0 ; i < tagList.size() ; i++)
                {
                    Tag<?> t = VanillaCompoundToCompound(tagList.get(i));
                    compoundList.add(t);
                }
                Class type;
                if (tagList.size() > 0)
                {
                    type = compoundList.get(compoundList.size() - 1).getClass();
                }
                else
                {
                    type = EndTag.class;
                }
                return new ListTag(vanillaTag.getName(), type, compoundList);
            case 10:
                CompoundTag compound = new CompoundTag(vanillaTag.getName(), new CompoundMap());
                Map<String, NBTBase> compoundMap;
                try
                {
                    Field f = NBTTagCompound.class.getDeclaredField("map");
                    f.setAccessible(true);
                    compoundMap = (Map<String, NBTBase>) f.get(vanillaTag);
                }
                catch (NoSuchFieldException e)
                {
                    return compound;
                }
                catch (IllegalAccessException e)
                {
                    return compound;
                }
                for (String tagName : compoundMap.keySet())
                {
                    compound.getValue().put(tagName, VanillaCompoundToCompound(compoundMap.get(tagName)));
                }
                return compound;
            case 11:
                return new IntArrayTag(vanillaTag.getName(), ((NBTTagIntArray) vanillaTag).data);
        }
        return null;
    }
}
