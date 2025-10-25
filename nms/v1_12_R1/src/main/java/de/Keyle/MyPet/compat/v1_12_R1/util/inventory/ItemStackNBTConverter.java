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

package de.Keyle.MyPet.compat.v1_12_R1.util.inventory;

import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.keyle.knbt.*;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Compat("v1_12_R1")
public class ItemStackNBTConverter {
    private static Field TAG_LIST_LIST = ReflectionUtil.getField(NBTTagList.class, "list");

    public static TagCompound itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return itemStackToCompound(CraftItemStack.asNMSCopy(itemStack));
    }

    public static TagCompound itemStackToCompound(ItemStack itemStack) {
        TagCompound compound = new TagCompound();

        compound.getCompoundData().put("id", new TagShort((short) Item.getId(itemStack.getItem())));
        compound.getCompoundData().put("Count", new TagByte((byte) itemStack.getCount()));
        compound.getCompoundData().put("Damage", new TagShort((short) itemStack.getData()));

        if (itemStack.getTag() != null) {
            compound.getCompoundData().put("tag", vanillaCompoundToCompound(itemStack.getTag()));
        }
        return compound;
    }

    public static ItemStack compoundToItemStack(TagCompound compound) {
        int id = compound.getAs("id", TagShort.class).getShortData();
        int count = compound.getAs("Count", TagByte.class).getByteData();
        int damage = compound.getAs("Damage", TagShort.class).getShortData();

        ItemStack itemstack = new ItemStack(Item.getById(id), count, damage);
        if (compound.containsKeyAs("tag", TagCompound.class)) {
            TagCompound compoundToConvert = compound.get("tag");
            itemstack.setTag((NBTTagCompound) compoundToVanillaCompound(compoundToConvert));
        }
        return itemstack;
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
        switch (vanillaTag.getTypeId()) {
            case 1:
                return new TagByte(((NBTTagByte) vanillaTag).g());
            case 2:
                return new TagShort(((NBTTagShort) vanillaTag).f());
            case 3:
                return new TagInt(((NBTTagInt) vanillaTag).e());
            case 4:
                return new TagLong(((NBTTagLong) vanillaTag).d());
            case 5:
                return new TagFloat(((NBTTagFloat) vanillaTag).i());
            case 6:
                return new TagDouble(((NBTTagDouble) vanillaTag).asDouble());
            case 7:
                return new TagByteArray(((NBTTagByteArray) vanillaTag).c());
            case 8:
                return new TagString(((NBTTagString) vanillaTag).c_());
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
                Set<String> keys = tagCompound.c();
                for (String tagName : keys) {
                    compound.getCompoundData().put(tagName, vanillaCompoundToCompound(tagCompound.get(tagName)));
                }
                return compound;
            case 11:
                return new TagIntArray(((NBTTagIntArray) vanillaTag).d());
        }
        return null;
    }
}