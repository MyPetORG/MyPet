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

import net.minecraft.server.v1_4_R1.*;
import org.spout.nbt.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemStackNBTConverter
{
    public static CompoundTag ItemStackToCompund(ItemStack itemStack)
    {
        CompoundTag compound = new CompoundTag(null, new CompoundMap());

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

    /*public static NBTTagCompound CompoundToVanillaCompound(CompoundTag compound)
    {
        NBTTagCompound vanillaCompound = new NBTTagCompound(compound.getName());
        for(String tagName : compound.getValue().keySet())
        {
            Tag tag = compound.getValue().get(tagName);
            switch (tag.getType())
            {
                case TAG_INT:
                    vanillaCompound.setInt(tagName, ((IntTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_SHORT:
                    vanillaCompound.setShort(tagName, ((ShortTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_BYTE:
                    vanillaCompound.setByte(tagName, ((ByteTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_BYTE_ARRAY:
                    vanillaCompound.setByteArray(tagName, ((ByteArrayTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_DOUBLE:
                    vanillaCompound.setDouble(tagName, ((DoubleTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_FLOAT:
                    vanillaCompound.setFloat(tagName, ((FloatTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_INT_ARRAY:
                    vanillaCompound.setIntArray(tagName, ((IntArrayTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_LONG:
                    vanillaCompound.setLong(tagName, ((LongTag)compound.getValue().get(tagName)).getValue());
                    break;
                case TAG_SHORT_ARRAY:
                    short[] shortArray = ((ShortArrayTag)compound.getValue().get(tagName)).getValue();
                    int[] intArray = new int[shortArray.length];
                    for(int i = 0; i < shortArray.length; i++)
                    {
                        intArray[i] = shortArray[i];
                    }
                    vanillaCompound.setIntArray(tagName, intArray);
                    break;
                case TAG_LIST:
                    ListTag listTag = (ListTag) compound.getValue().get(tagName);
                    NBTTagList tagList = new NBTTagList(listTag.getName());
                    for(Object tagObj : listTag.getValue())
                    {
                        Tag tagInList = (Tag) tagObj;
                        switch (tagInList.getType())
                        {
                            case TAG_INT:
                                vanillaCompound.setInt(tagName, ((IntTag)compound.getValue().get(tagName)).getValue());
                                tagList.add(new NBTTagLong(tagName, ((LongTag) compound.getValue().get(tagName)).getValue()));
                                break;
                            case TAG_SHORT:
                                vanillaCompound.setShort(tagName, ((ShortTag)compound.getValue().get(tagName)).getValue());
                                tagList.add(new NBTTagLong(tagName, ((LongTag) compound.getValue().get(tagName)).getValue()));
                                break;
                            case TAG_BYTE:
                                vanillaCompound.setByte(tagName, ((ByteTag)compound.getValue().get(tagName)).getValue());
                                tagList.add(new NBTTagLong(tagName, ((LongTag) compound.getValue().get(tagName)).getValue()));
                                break;
                            case TAG_BYTE_ARRAY:
                                tagList.add(new NBTTagByteArray(tagName, ((ByteArrayTag)compound.getValue().get(tagName)).getValue());
                                break;
                            case TAG_DOUBLE:
                                tagList.add(new NBTTagDouble(tagName, ((DoubleTag)compound.getValue().get(tagName)).getValue());
                                break;
                            case TAG_FLOAT:
                                tagList.add(new NBTTagFloat(tagName, ((FloatTag)compound.getValue().get(tagName)).getValue());
                                break;
                            case TAG_INT_ARRAY:
                                tagList.add(new NBTTagIntArray(tagName, ((IntArrayTag) compound.getValue().get(tagName)).getValue());
                                break;
                            case TAG_LONG:
                                tagList.add(new NBTTagLong(tagName, ((LongTag) compound.getValue().get(tagName)).getValue()));
                                break;
                            case TAG_SHORT_ARRAY:
                                short[] shortArray2 = ((ShortArrayTag)compound.getValue().get(tagName)).getValue();
                                int[] intArray2 = new int[shortArray2.length];
                                for(int i = 0; i < shortArray2.length; i++)
                                {
                                    intArray2[i] = shortArray2[i];
                                }
                                tagList.add(new NBTTagIntArray(tagName, intArray2));
                                break;
                            case TAG_COMPOUND:
                                tagList.add(CompoundToVanillaCompound((CompoundTag) tagInList));
                                break;
                            case TAG_END:
                                break;
                        }
                    }
                    vanillaCompound.set(tagName, tagList);
                    break;
                case TAG_COMPOUND:
                    vanillaCompound.setCompound(tagName, CompoundToVanillaCompound((CompoundTag) compound.getValue().get(tagName)));
                    break;
                case TAG_END:
                    break;

            }
        }
        return vanillaCompound;
    }
    */

    /*
    @SuppressWarnings("unchecked")
    public static CompoundTag VanillaCompoundToCompound(NBTTagCompound vanillaCompound)
    {
        CompoundTag compound = new CompoundTag(vanillaCompound.getName(), new CompoundMap());
        Map<String, NBTBase> compoundMap;
        try
        {
            Field a = NBTTagCompound.class.getDeclaredField("map");
            a.setAccessible(true);
            compoundMap = (Map<String, NBTBase>) a.get(vanillaCompound);
        }
        catch (NoSuchFieldException e)
        {
            return compound;
        }
        catch (IllegalAccessException e)
        {
            return compound;
        }
        for(String tagName : compoundMap.keySet())
        {
            NBTBase baseCompound = vanillaCompound.get(tagName);
            switch (baseCompound.getTypeId())
            {
                case 3:
                    compound.getValue().put(tagName, new IntTag(tagName, vanillaCompound.getInt(tagName)));
                    break;
                case 2:
                    compound.getValue().put(tagName, new ShortTag(tagName, vanillaCompound.getShort(tagName)));
                    break;
                case 1:
                    compound.getValue().put(tagName, new ByteTag(tagName, vanillaCompound.getByte(tagName)));
                    break;
                case 7:
                    compound.getValue().put(tagName, new ByteArrayTag(tagName, vanillaCompound.getByteArray(tagName)));
                    break;
                case 6:
                    compound.getValue().put(tagName, new DoubleTag(tagName, vanillaCompound.getDouble(tagName)));
                    break;
                case 5:
                    compound.getValue().put(tagName, new FloatTag(tagName, vanillaCompound.getFloat(tagName)));
                    break;
                case 11:
                    compound.getValue().put(tagName, new IntArrayTag(tagName, vanillaCompound.getIntArray(tagName)));
                    break;
                case 4:
                    compound.getValue().put(tagName, new LongTag(tagName, vanillaCompound.getLong(tagName)));
                    break;
                case 9:
                    NBTTagList tagList = vanillaCompound.getList(tagName);
                    List<Tag> compoundList = new ArrayList<Tag>();
                    for (int i = 0 ; i < tagList.size() ; i++)
                    {
                        MyPetLogger.write("n: " + tagList.get(i).getName() + " - t: " + tagList.get(i).getTypeId());
                        if(tagList.get(i) instanceof NBTTagCompound)
                        {
                            compoundList.add(VanillaCompoundToCompound((NBTTagCompound) tagList.get(i)));
                        }
                        switch (tagList.get(i).getTypeId())
                        {
                            case 3:
                                compoundList.add(new IntTag(tagName, vanillaCompound.getInt(tagName)));
                                break;
                            case 2:
                                compoundList.add(new ShortTag(tagName, vanillaCompound.getShort(tagName)));
                                break;
                            case 1:
                                compoundList.add(new ByteTag(tagName, vanillaCompound.getByte(tagName)));
                                break;
                            case 7:
                                compoundList.add(new ByteArrayTag(tagName, vanillaCompound.getByteArray(tagName)));
                                break;
                            case 6:
                                compoundList.add(new DoubleTag(tagName, vanillaCompound.getDouble(tagName)));
                                break;
                            case 5:
                                compoundList.add(new FloatTag(tagName, vanillaCompound.getFloat(tagName)));
                                break;
                            case 11:
                                compoundList.add(new IntArrayTag(tagName, vanillaCompound.getIntArray(tagName)));
                                break;
                            case 4:
                                compoundList.add(new LongTag(tagName, vanillaCompound.getLong(tagName)));
                                break;
                            case 10:
                                compoundList.add(VanillaCompoundToCompound(vanillaCompound.getCompound(tagName)));
                                break;
                            case 0:
                                break;
                        }
                    }
                    compound.getValue().put(tagName, new ListTag(tagName, CompoundTag.class, compoundList));
                    break;
                case 10:
                    compound.getValue().put(tagName, VanillaCompoundToCompound(vanillaCompound.getCompound(tagName)));
                    break;
                case 0:
                    break;
            }
        }
        return compound;
    }
    */
}
