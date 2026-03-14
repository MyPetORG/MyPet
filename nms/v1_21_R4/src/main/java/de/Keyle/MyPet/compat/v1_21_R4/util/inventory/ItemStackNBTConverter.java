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

package de.Keyle.MyPet.compat.v1_21_R4.util.inventory;

import com.mojang.serialization.Dynamic;
import de.Keyle.MyPet.MyPetApi;
import net.kyori.adventure.nbt.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemStackNBTConverter {

    public static RegistryAccess registryAccess = CraftRegistry.getMinecraftRegistry();

    public static CompoundBinaryTag itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return itemStackToCompound(CraftItemStack.asNMSCopy(itemStack));
    }

    public static CompoundBinaryTag itemStackToCompound(ItemStack itemStack) {
        return (CompoundBinaryTag) vanillaCompoundToCompound(itemStackToVanillaCompound(itemStack));
    }

    public static CompoundTag itemStackToVanillaCompound(ItemStack itemStack) {
        return (CompoundTag) itemStack.save(registryAccess);
    }

    public static ItemStack compoundToItemStack(CompoundBinaryTag compound) {
        CompoundTag tagCompound = (CompoundTag) compoundToVanillaCompound(compound);
        return vanillaCompoundToItemStack(tagCompound);
    }

    public static org.bukkit.inventory.ItemStack compoundToBukkitItemStack(CompoundBinaryTag compound) {
        return CraftItemStack.asBukkitCopy(compoundToItemStack(compound));
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

        CompoundTag toParse = compoundTag;
        boolean modified = false;

        // Check if this has the old "tag" format and needs DataFixer conversion
        if (toParse.contains("tag")) {
            toParse = convertOldVanillaCompound(toParse);
            modified = true;
        }

        // If it has components but with old intermediate formats, fix them
        else if (toParse.contains("components")) {
            CompoundTag fixed = fixEnchantmentsFormat(toParse);
            if (!fixed.equals(toParse)) {
                toParse = fixed;
                modified = true;
            }
        }

        // Output the corrected string if conversion happened
        if (modified) {
            MyPetApi.getLogger().warning("Old item format detected! Update your item to the new format (make sure to include the period at the beginning):");
            MyPetApi.getLogger().warning(" . " + toParse);
            MyPetApi.getLogger().warning("This warning will disappear once you update your config.");
        }

        return ItemStack.parse(registryAccess, toParse).orElse(ItemStack.EMPTY);
    }

    private static CompoundTag fixEnchantmentsFormat(CompoundTag compoundTag) {
        CompoundTag result = compoundTag.copy();

        // Check if components exists and has old-format enchantments
        if (result.contains("components")) {
            var componentsOpt = result.getCompound("components");
            if (componentsOpt.isEmpty()) {
                return result;
            }
            CompoundTag components = componentsOpt.get().copy();
            boolean modified = false;

            // Fix enchantments format
            if (components.contains("minecraft:enchantments")) {
                var enchantmentsOpt = components.getCompound("minecraft:enchantments");
                if (enchantmentsOpt.isPresent()) {
                    CompoundTag enchantments = enchantmentsOpt.get();

                    // Check if it has the old format with "levels" and "show_in_tooltip"
                    if (enchantments.contains("levels")) {
                        var levelsOpt = enchantments.getCompound("levels");
                        if (levelsOpt.isPresent()) {
                            CompoundTag levels = levelsOpt.get();
                            components.put("minecraft:enchantments", levels);
                            modified = true;
                        }
                    }
                }
            }

            // Fix text components that are stored as strings (custom_name, lore, etc.)
            modified |= fixTextComponent(components, "minecraft:custom_name");
            modified |= fixTextComponentList(components, "minecraft:lore");

            if (modified) {
                result.put("components", components);
            }
        }

        return result;
    }

    private static boolean fixTextComponent(CompoundTag components, String key) {
        if (components.contains(key)) {
            var textOpt = components.getString(key);
            if (textOpt.isPresent()) {
                String text = textOpt.get();
                // Check if it's a JSON string that needs to be parsed
                if (text.startsWith("{") && text.endsWith("}")) {
                    try {
                        CompoundTag parsed = TagParser.parseCompoundFully(text);
                        components.put(key, parsed);
                        return true;
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("Failed to parse text component for " + key + ": " + e.getMessage());
                    }
                }
            }
        }
        return false;
    }

    private static boolean fixTextComponentList(CompoundTag components, String key) {
        // Similar logic for lore (list of text components)
        if (components.contains(key)) {
            Tag loreTag = components.get(key);
            if (loreTag instanceof ListTag loreList) {
                ListTag newLore = new ListTag();
                boolean modified = false;

                for (int i = 0; i < loreList.size(); i++) {
                    Tag element = loreList.get(i);
                    if (element instanceof StringTag strTag) {
                        String text = strTag.value();
                        if (text.startsWith("{") && text.endsWith("}")) {
                            try {
                                CompoundTag parsed = TagParser.parseCompoundFully(text);
                                newLore.add(parsed);
                                modified = true;
                                continue;
                            } catch (Exception e) {
                                MyPetApi.getLogger().warning("Failed to parse lore component: " + e.getMessage());
                            }
                        }
                    }
                    newLore.add(element);
                }

                if (modified) {
                    components.put(key, newLore);
                    return true;
                }
            }
        }
        return false;
    }

    public static CompoundTag convertOldVanillaCompound(CompoundTag oldTag) {
        Dynamic<Tag> dyn = new Dynamic<>(NbtOps.INSTANCE, oldTag);
        Dynamic<Tag> updatedDyn = DataFixers.getDataFixer().update(References.ITEM_STACK, dyn,
                1519, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        return (CompoundTag) updatedDyn.getValue();
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
            case 1 -> ByteBinaryTag.byteBinaryTag(((ByteTag) vanillaTag).byteValue());
            case 2 -> ShortBinaryTag.shortBinaryTag(((ShortTag) vanillaTag).shortValue());
            case 3 -> IntBinaryTag.intBinaryTag(((IntTag) vanillaTag).intValue());
            case 4 -> LongBinaryTag.longBinaryTag(((LongTag) vanillaTag).longValue());
            case 5 -> FloatBinaryTag.floatBinaryTag(((FloatTag) vanillaTag).floatValue());
            case 6 -> DoubleBinaryTag.doubleBinaryTag(((DoubleTag) vanillaTag).doubleValue());
            case 7 -> ByteArrayBinaryTag.byteArrayBinaryTag(((ByteArrayTag) vanillaTag).getAsByteArray());
            case 8 -> StringBinaryTag.stringBinaryTag(((StringTag) vanillaTag).value());
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
                CompoundTag tagCompound = (CompoundTag) vanillaTag;
                for (String tagName : tagCompound.keySet()) {
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
