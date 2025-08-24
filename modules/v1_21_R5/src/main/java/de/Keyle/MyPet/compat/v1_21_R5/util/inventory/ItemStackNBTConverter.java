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

package de.Keyle.MyPet.compat.v1_21_R5.util.inventory;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.keyle.knbt.TagType;
import de.keyle.knbt.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.craftbukkit.v1_21_R5.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Compat("v1_21_R5")
public class ItemStackNBTConverter {

	private static final Field TAG_LIST_LIST = ReflectionUtil.getField(ListTag.class, "v"); //List-Field (or value)
    public static RegistryAccess registryAccess = CraftRegistry.getMinecraftRegistry();
    private static final CompoundTag EMPTY_ITEM_COMPOUND;

    static {
        EMPTY_ITEM_COMPOUND = new CompoundTag();
        EMPTY_ITEM_COMPOUND.putString("id", "minecraft:air");
        EMPTY_ITEM_COMPOUND.putInt("count", 1);
    }

    public static TagCompound itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return itemStackToCompound(CraftItemStack.asNMSCopy(itemStack));
    }

    public static TagCompound itemStackToCompound(ItemStack itemStack) {
        return (TagCompound) vanillaCompoundToCompound(itemStackToVanillaCompound(itemStack));
    }

    public static CompoundTag itemStackToVanillaCompound(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return EMPTY_ITEM_COMPOUND.copy();
        }

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        DataResult<Tag> res = ItemStack.CODEC.encodeStart(ops, itemStack);
        Tag out = res.result().orElseGet(CompoundTag::new);

        // For ItemStack this will be a CompoundTag
        return (out instanceof CompoundTag ct) ? ct : new CompoundTag();
    }

    public static ItemStack compoundToItemStack(TagCompound compound) {
        CompoundTag tagCompound = (CompoundTag) compoundToVanillaCompound(compound);
        return vanillaCompoundToItemStack(tagCompound);
    }

    public static ItemStack vanillaCompoundToItemStack(CompoundTag compoundTag) {
        if (compoundTag == null || compoundTag.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // quick air checks (defensive)
        Optional<String> id = compoundTag.getString("id"); // returns "" if missing
        if (id.isEmpty() || "minecraft:air".equals(id.get()) || "air".equals(id.get())) {
            return ItemStack.EMPTY;
        }

        // If you have legacy structure adjustments, keep your converter:
        CompoundTag toParse = compoundTag.contains("tag")
                ? convertOldVanillaCompound(compoundTag)
                : compoundTag;

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        return ItemStack.CODEC.parse(ops, toParse).result().orElse(ItemStack.EMPTY);
    }

    public static CompoundTag convertOldVanillaCompound(CompoundTag oldTag) {
        Dynamic<Tag> dyn = new Dynamic<>(NbtOps.INSTANCE, oldTag);
        Dynamic<Tag> updatedDyn = DataFixers.getDataFixer().update(References.ITEM_STACK, dyn,
                1519, SharedConstants.getCurrentVersion().dataVersion().version());
        return (CompoundTag) updatedDyn.getValue();
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
                return new TagByte(((ByteTag) vanillaTag).byteValue());
            case 2:
                return new TagShort(((ShortTag) vanillaTag).shortValue());
            case 3:
                return new TagInt(((IntTag) vanillaTag).intValue());
            case 4:
                return new TagLong(((LongTag) vanillaTag).longValue());
            case 5:
                return new TagFloat(((FloatTag) vanillaTag).floatValue());
            case 6:
                return new TagDouble(((DoubleTag) vanillaTag).doubleValue());
            case 7:
                return new TagByteArray(((ByteArrayTag) vanillaTag).getAsByteArray());
            case 8:
                return new TagString(((StringTag) vanillaTag).value());
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
                Set<String> keys = tagCompound.keySet();
                for (String tagName : keys) {
                    compound.getCompoundData().put(tagName, vanillaCompoundToCompound(tagCompound.get(tagName)));
                }
                return compound;
            case 11:
                return new TagIntArray(((IntArrayTag) vanillaTag).getAsIntArray());
        }
        return null;
    }
}
