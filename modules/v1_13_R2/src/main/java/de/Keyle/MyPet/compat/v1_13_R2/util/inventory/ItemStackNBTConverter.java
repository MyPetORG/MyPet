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

package de.Keyle.MyPet.compat.v1_13_R2.util.inventory;

import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.keyle.knbt.*;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Compat("v1_13_R2")
public class ItemStackNBTConverter {

    private static Field TAG_LIST_LIST = ReflectionUtil.getField(NBTTagList.class, "list");
    private static Method METHOD_BYTE_g = ReflectionUtil.getMethod(NBTTagByte.class, "g");
    private static Method METHOD_SHORT_f = ReflectionUtil.getMethod(NBTTagShort.class, "f");
    private static Method METHOD_INT_e = ReflectionUtil.getMethod(NBTTagInt.class, "e");
    private static Method METHOD_LONG_d = ReflectionUtil.getMethod(NBTTagLong.class, "d");
    private static Method METHOD_FLOAT_i = ReflectionUtil.getMethod(NBTTagFloat.class, "i");
    private static Method METHOD_STRING_b_ = ReflectionUtil.getMethod(NBTTagString.class, "b_");

    public static TagCompound itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return itemStackToCompound(CraftItemStack.asNMSCopy(itemStack));
    }

    public static TagCompound itemStackToCompound(ItemStack itemStack) {
        NBTTagCompound tagCompound = new NBTTagCompound();
        itemStack.save(tagCompound);
        return (TagCompound) vanillaCompoundToCompound(tagCompound);
    }

    public static ItemStack compoundToItemStack(TagCompound compound) {
        NBTTagCompound tagCompound = (NBTTagCompound) compoundToVanillaCompound(compound);
        return ItemStack.a(tagCompound);
    }

    @SuppressWarnings("unchecked")
    public static NBTBase compoundToVanillaCompound(TagBase tag) {
        switch (TagType.getTypeById(tag.getTagTypeId())) {
            case Int:
                return new NBTTagInt(((TagInt) tag).getIntData());
            case Short:
                return new NBTTagShort(((TagShort) tag).getShortData());
            case String:
                return new NBTTagString(((TagString) tag).getStringData());
            case Byte:
                return new NBTTagByte(((TagByte) tag).getByteData());
            case Byte_Array:
                return new NBTTagByteArray(((TagByteArray) tag).getByteArrayData());
            case Double:
                return new NBTTagDouble(((TagDouble) tag).getDoubleData());
            case Float:
                return new NBTTagFloat(((TagFloat) tag).getFloatData());
            case Int_Array:
                return new NBTTagIntArray(((TagIntArray) tag).getIntArrayData());
            case Long:
                return new NBTTagLong(((TagLong) tag).getLongData());
            case List:
                TagList TagList = (TagList) tag;
                NBTTagList tagList = new NBTTagList();
                for (TagBase tagInList : TagList.getReadOnlyList()) {
                    tagList.add(compoundToVanillaCompound(tagInList));
                }
                return tagList;
            case Compound:
                TagCompound TagCompound = (TagCompound) tag;
                NBTTagCompound tagCompound = new NBTTagCompound();
                for (String name : TagCompound.getCompoundData().keySet()) {
                    tagCompound.set(name, compoundToVanillaCompound(TagCompound.getCompoundData().get(name)));
                }
                return tagCompound;
            case End:
                return null;
        }
        throw new IllegalArgumentException("Not a valid tag type");
    }

    @SuppressWarnings("unchecked")
    public static TagBase vanillaCompoundToCompound(NBTBase vanillaTag) {
        try {
            switch (vanillaTag.getTypeId()) {
                case 1:
                    return new TagByte(((NBTTagByte) vanillaTag).asByte());
                case 2:
                    return new TagShort(((NBTTagShort) vanillaTag).asShort());
                case 3:
                    return new TagInt(((NBTTagInt) vanillaTag).asInt());
                case 4:
                    return new TagLong(((NBTTagLong) vanillaTag).asLong());
                case 5:
                    return new TagFloat(((NBTTagFloat) vanillaTag).asFloat());
                case 6:
                    return new TagDouble(((NBTTagDouble) vanillaTag).asDouble());
                case 7:
                    return new TagByteArray(((NBTTagByteArray) vanillaTag).c());
                case 8:
                    return new TagString(vanillaTag.asString());
                case 9:
                    NBTTagList tagList = (NBTTagList) vanillaTag;
                    List compoundList = new ArrayList();
                    try {
                        ArrayList list = (ArrayList) TAG_LIST_LIST.get(tagList);
                        for (Object aList : list) {
                            compoundList.add(vanillaCompoundToCompound((NBTBase) aList));
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    return new TagList(compoundList);
                case 10:
                    TagCompound compound = new TagCompound();
                    NBTTagCompound tagCompound = ((NBTTagCompound) vanillaTag);
                    Set<String> keys = tagCompound.getKeys();
                    for (String tagName : keys) {
                        compound.getCompoundData().put(tagName, vanillaCompoundToCompound(tagCompound.get(tagName)));
                    }
                    return compound;
                case 11:
                    return new TagIntArray(((NBTTagIntArray) vanillaTag).d());
            }
        } catch (NoSuchMethodError e) {
            return vanillaCompoundToCompoundLegacy(vanillaTag);
        }
        return null;
    }

    public static TagBase vanillaCompoundToCompoundLegacy(NBTBase vanillaTag) {
        try {
            switch (vanillaTag.getTypeId()) {
                case 1:
                    return new TagByte((Byte) METHOD_BYTE_g.invoke(vanillaTag));
                case 2:
                    return new TagShort((Short) METHOD_SHORT_f.invoke(vanillaTag));
                case 3:
                    return new TagInt((Integer) METHOD_INT_e.invoke(vanillaTag));
                case 4:
                    return new TagLong((Long) METHOD_LONG_d.invoke(vanillaTag));
                case 5:
                    return new TagFloat((Float) METHOD_FLOAT_i.invoke(vanillaTag));
                case 8:
                    return new TagString(METHOD_STRING_b_.invoke(vanillaTag).toString());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}