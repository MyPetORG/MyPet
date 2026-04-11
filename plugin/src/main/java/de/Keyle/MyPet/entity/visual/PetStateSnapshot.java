package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reads a vanilla Bukkit mob's current visual/state data and writes it into a
 * {@link CompoundBinaryTag} matching the key format that
 * {@code plugin/entity/types/My*.readExtendedInfo()} expects.
 */
public final class PetStateSnapshot {

    private static final Random RANDOM = new Random();

    private PetStateSnapshot() {}

    /**
     * Reads a vanilla mob's state into a tag for MyPet persistence. The tag
     * format matches what each {@code My*.readExtendedInfo()} expects.
     *
     * <p>Used on the initial leash/tame path — includes the random equipment
     * drop roll for zombies per {@code Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME}.
     */
    public static CompoundBinaryTag toTag(LivingEntity entity) {
        return toTag(entity, true);
    }

    /**
     * Reads a vanilla mob's state into a tag. {@code fullSnapshot} distinguishes
     * two call sites:
     * <ul>
     *   <li>{@code true} — initial leash/tame path. Includes ALL fields, including
     *       cosmetic fields that can't normally change via vanilla interaction
     *       (cat type, wolf variant, horse color, etc.). Also includes the
     *       zombie-family droppable equipment roll.</li>
     *   <li>{@code false} — post-interaction re-sync of an already-tamed pet.
     *       ONLY writes fields that vanilla interaction can legitimately mutate
     *       (saddle add/remove, sheared, powered, equipment, chest, etc.).
     *       Cosmetic-only fields are skipped so they aren't overwritten by a
     *       round-trip through potentially-lossy ordinal lookups.</li>
     * </ul>
     *
     * <p>The cosmetic-vs-interactive split matters because of how some pet
     * type classes read the tag: e.g. {@code MyCat.readExtendedInfo} uses an
     * internal {@code OwnCatType} enum for ordinal→Bukkit-Type translation,
     * whose hardcoded ordering can drift from Paper's registry ordering on
     * newer Minecraft versions. Writing the Bukkit ordinal and reading it
     * back through the local enum corrupts the variant. Skipping cosmetic
     * fields in the re-sync path entirely avoids the issue for interaction
     * events, and the initial tame path still round-trips via the same code
     * (which was already the case pre-refactor, so no regression there).
     */
    public static CompoundBinaryTag toTag(LivingEntity entity, boolean fullSnapshot) {
        CompoundBinaryTag.Builder b = CompoundBinaryTag.builder();

        switch (entity.getType()) {
            case WOLF -> {
                Wolf wolf = (Wolf) entity;
                b.putBoolean("Tamed", wolf.isTamed());
                b.putByte("CollarColor", (byte) wolf.getCollarColor().ordinal());
                if (fullSnapshot) {
                    b.putString("Variant", wolf.getVariant().getKey().getKey());
                }
            }
            case SHEEP -> {
                Sheep sheep = (Sheep) entity;
                b.putInt("Color", sheep.getColor().getDyeData());
                b.putBoolean("Sheared", sheep.isSheared());
            }
            case PIG -> {
                Pig pig = (Pig) entity;
                b.putBoolean("Saddle", pig.hasSaddle());
                if (fullSnapshot) {
                    // Pig variant API added in 1.21.4+ — resolve reflectively
                    try {
                        Object variant = pig.getClass().getMethod("getVariant").invoke(pig);
                        Object key = variant.getClass().getMethod("getKey").invoke(variant);
                        Object keyStr = key.getClass().getMethod("getKey").invoke(key);
                        b.putString("Variant", String.valueOf(keyStr));
                    } catch (Throwable ignored) {}
                }
            }
            case CREEPER -> {
                b.putBoolean("Powered", ((Creeper) entity).isPowered());
            }
            case SLIME, MAGMA_CUBE -> {
                if (fullSnapshot) {
                    b.putInt("Size", ((Slime) entity).getSize());
                }
            }
            case PHANTOM -> {
                if (fullSnapshot) {
                    b.putInt("Size", ((Phantom) entity).getSize());
                }
            }
            case CAT -> {
                Cat cat = (Cat) entity;
                b.putInt("CollarColor", cat.getCollarColor().ordinal());
                b.putBoolean("Tamed", cat.isTamed());
                if (fullSnapshot) {
                    try {
                        // Drift-safe: store the NamespacedKey path so MyCat can
                        // resolve via Registry.CAT_VARIANT regardless of enum
                        // ordering changes between Paper versions.
                        b.putString("CatTypeKey", cat.getCatType().getKey().getKey());
                    } catch (Throwable ignored) {}
                }
            }
            case PARROT -> {
                if (fullSnapshot) {
                    try {
                        b.putString("VariantName", ((Parrot) entity).getVariant().name());
                    } catch (Throwable ignored) {}
                }
            }
            case RABBIT -> {
                if (fullSnapshot) {
                    // New format: stores the Bukkit Rabbit.Type name under
                    // "VariantName". Matches the v4 format written by
                    // MyRabbit#writeExtendedInfo. The legacy "Variant" byte
                    // key is not written — see MyRabbit Javadoc for the
                    // migration note.
                    try {
                        b.putString("VariantName", ((Rabbit) entity).getRabbitType().name());
                    } catch (Throwable ignored) {}
                }
            }
            case AXOLOTL -> {
                if (fullSnapshot) {
                    try {
                        b.putString("VariantName", ((Axolotl) entity).getVariant().name());
                    } catch (Throwable ignored) {}
                }
            }
            case FROG -> {
                if (fullSnapshot) {
                    try {
                        // MyFrog reads by Frog.Variant#name(), matching how it
                        // stores internally. Use the same method reference so
                        // read and write stay in sync regardless of whether
                        // Frog.Variant is an enum or a Keyed interface on this
                        // runtime (both expose name()).
                        b.putString("FrogTypeName", ((Frog) entity).getVariant().name());
                    } catch (Throwable ignored) {}
                }
            }
            case TROPICAL_FISH -> {
                if (fullSnapshot) {
                    b.putInt("Variant", ((TropicalFish) entity).getPattern().ordinal());
                }
            }
            case SALMON -> {
                if (fullSnapshot) {
                    try { b.putInt("Variant", ((Salmon) entity).getVariant().ordinal()); } catch (Throwable ignored) {}
                }
            }
            case PUFFERFISH -> {
                b.putInt("PuffState", Util.clamp(((PufferFish) entity).getPuffState(), 0, 2));
            }
            case MOOSHROOM -> {
                if (fullSnapshot) {
                    try {
                        // Stores the Bukkit MushroomCow.Variant name directly
                        // ("RED" or "BROWN"). Matches the v4 format written
                        // by MyMooshroom#writeExtendedInfo. Pre-v4 data stored
                        // PascalCase ("Red"/"Brown") — see MyMooshroom Javadoc
                        // for the migration note.
                        b.putString("CowTypeName", ((MushroomCow) entity).getVariant().name());
                    } catch (Throwable ignored) {}
                }
            }
            case FOX -> {
                if (fullSnapshot) {
                    try {
                        b.putString("FoxTypeName", ((Fox) entity).getFoxType().name());
                    } catch (Throwable ignored) {}
                }
            }
            case PANDA -> {
                if (fullSnapshot) {
                    try {
                        Panda panda = (Panda) entity;
                        b.putString("MainGeneName", panda.getMainGene().name());
                        b.putString("HiddenGeneName", panda.getHiddenGene().name());
                    } catch (Throwable ignored) {}
                }
            }
            case BEE -> {
                Bee bee = (Bee) entity;
                b.putBoolean("Angry", bee.getAnger() > 1);
                b.putBoolean("HasStung", bee.hasStung());
                b.putBoolean("HasNectar", bee.hasNectar());
            }
            case GOAT -> {
                Goat goat = (Goat) entity;
                b.putBoolean("screaming", goat.isScreaming());
                b.putBoolean("LeftHorn", goat.hasLeftHorn());
                b.putBoolean("RightHorn", goat.hasRightHorn());
            }
            case HORSE -> {
                Horse horse = (Horse) entity;
                if (fullSnapshot) {
                    try {
                        b.putString("ColorName", horse.getColor().name());
                        b.putString("StyleName", horse.getStyle().name());
                    } catch (Throwable ignored) {}
                }
                writeEquipment(horse, b);
            }
            case SKELETON_HORSE, ZOMBIE_HORSE -> {
                AbstractHorse ah = (AbstractHorse) entity;
                if (ah.getInventory().getSaddle() != null) {
                    writeEquipmentItem(b, EquipmentSlot.BODY, ah.getInventory().getSaddle());
                }
            }
            case DONKEY, MULE -> {
                ChestedHorse ch = (ChestedHorse) entity;
                b.putBoolean("Chest", ch.isCarryingChest());
                if (ch.getInventory().getSaddle() != null) {
                    writeEquipmentItem(b, EquipmentSlot.BODY, ch.getInventory().getSaddle());
                }
            }
            case CAMEL -> {
                Camel camel = (Camel) entity;
                if (camel.getInventory().getSaddle() != null) {
                    writeEquipmentItem(b, EquipmentSlot.BODY, camel.getInventory().getSaddle());
                }
            }
            case LLAMA -> {
                Llama llama = (Llama) entity;
                if (fullSnapshot) {
                    try {
                        b.putString("ColorName", llama.getColor().name());
                    } catch (Throwable ignored) {}
                }
                if (llama.getInventory().getDecor() != null && llama.getInventory().getDecor().getType() != Material.AIR) {
                    b.put("Decor", MyPetApi.getPlatformHelper().itemStackToCompound(llama.getInventory().getDecor()));
                }
                if (llama.isCarryingChest()) {
                    b.put("Chest", MyPetApi.getPlatformHelper().itemStackToCompound(new ItemStack(Material.CHEST)));
                }
            }
            case TRADER_LLAMA -> {
                if (fullSnapshot) {
                    try {
                        b.putString("ColorName", ((TraderLlama) entity).getColor().name());
                    } catch (Throwable ignored) {}
                }
            }
            case ENDERMAN -> {
                Enderman enderman = (Enderman) entity;
                if (enderman.getCarriedBlock() != null) {
                    ItemStack block = enderman.getCarriedMaterial().toItemStack(1);
                    b.put("Block", MyPetApi.getPlatformHelper().itemStackToCompound(block));
                }
            }
            case VILLAGER -> {
                Villager villager = (Villager) entity;
                // Level can change via trading — always sync.
                b.putInt("VillagerLevel", villager.getVillagerLevel());
                if (fullSnapshot) {
                    try {
                        b.putString("ProfessionKey", villager.getProfession().getKey().getKey());
                    } catch (Throwable ignored) {}
                    try {
                        b.putString("VillagerTypeKey", villager.getVillagerType().getKey().getKey());
                    } catch (Throwable ignored) {}
                }
            }
            case ZOMBIE_VILLAGER -> {
                if (fullSnapshot) {
                    ZombieVillager zv = (ZombieVillager) entity;
                    try {
                        b.putString("ProfessionKey", zv.getVillagerProfession().getKey().getKey());
                    } catch (Throwable ignored) {}
                    try {
                        b.putString("VillagerTypeKey", zv.getVillagerType().getKey().getKey());
                    } catch (Throwable ignored) {}
                }
            }
            case ZOMBIE, HUSK, DROWNED, ZOMBIFIED_PIGLIN -> {
                if (fullSnapshot) {
                    b.putBoolean("Baby", ((Zombie) entity).isBaby());
                    if (Configuration.Misc.RETAIN_EQUIPMENT_ON_TAME) {
                        writeDroppableEquipment(entity, b);
                    }
                }
            }
            case COW -> {
                if (fullSnapshot) {
                    try {
                        Object variant = entity.getClass().getMethod("getVariant").invoke(entity);
                        Object key = variant.getClass().getMethod("getKey").invoke(variant);
                        Object keyStr = key.getClass().getMethod("getKey").invoke(key);
                        b.putString("Variant", String.valueOf(keyStr));
                    } catch (Throwable ignored) {}
                }
            }
            case CHICKEN -> {
                if (fullSnapshot) {
                    try {
                        Object variant = entity.getClass().getMethod("getVariant").invoke(entity);
                        Object key = variant.getClass().getMethod("getKey").invoke(variant);
                        Object keyStr = key.getClass().getMethod("getKey").invoke(key);
                        b.putString("Variant", String.valueOf(keyStr));
                    } catch (Throwable ignored) {}
                }
            }
            case STRIDER -> {
                Strider strider = (Strider) entity;
                b.putBoolean("Saddle", strider.hasSaddle());
            }
            case SNOW_GOLEM -> {
                b.putBoolean("Sheared", ((Snowman) entity).isDerp());
            }
            default -> {
                // Types with no visual state — nothing to snapshot.
            }
        }

        // Baby flag: technically immutable for an already-spawned mob, but cheap
        // to sync and harmless if the mob hasn't grown up.
        if (fullSnapshot && entity instanceof Ageable ageable) {
            b.putBoolean("Baby", !ageable.isAdult());
        }

        return b.build();
    }

    private static void writeEquipment(Horse horse, CompoundBinaryTag.Builder b) {
        List<CompoundBinaryTag> items = new ArrayList<>();
        if (horse.getInventory().getArmor() != null) {
            CompoundBinaryTag armor = MyPetApi.getPlatformHelper().itemStackToCompound(horse.getInventory().getArmor())
                    .putString("Slot", EquipmentSlot.BODY.name());
            items.add(armor);
        }
        if (horse.getInventory().getSaddle() != null) {
            CompoundBinaryTag saddle = MyPetApi.getPlatformHelper().itemStackToCompound(horse.getInventory().getSaddle())
                    .putString("Slot", EquipmentSlot.BODY.name());
            items.add(saddle);
        }
        if (!items.isEmpty()) {
            b.put("Equipment", ListBinaryTag.from(items));
        }
    }

    private static void writeEquipmentItem(CompoundBinaryTag.Builder b, EquipmentSlot slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        List<CompoundBinaryTag> items = new ArrayList<>();
        CompoundBinaryTag tag = MyPetApi.getPlatformHelper().itemStackToCompound(item)
                .putString("Slot", slot.name());
        items.add(tag);
        b.put("Equipment", ListBinaryTag.from(items));
    }

    private static void writeDroppableEquipment(LivingEntity entity, CompoundBinaryTag.Builder b) {
        List<CompoundBinaryTag> items = new ArrayList<>();
        var eq = entity.getEquipment();
        if (eq == null) return;

        tryAddEquipment(items, eq.getHelmet(), EquipmentSlot.HEAD, eq.getHelmetDropChance());
        tryAddEquipment(items, eq.getChestplate(), EquipmentSlot.CHEST, eq.getChestplateDropChance());
        tryAddEquipment(items, eq.getLeggings(), EquipmentSlot.LEGS, eq.getLeggingsDropChance());
        tryAddEquipment(items, eq.getBoots(), EquipmentSlot.FEET, eq.getBootsDropChance());
        tryAddEquipment(items, eq.getItemInMainHand(), EquipmentSlot.HAND, eq.getItemInMainHandDropChance());
        tryAddEquipment(items, eq.getItemInOffHand(), EquipmentSlot.OFF_HAND, eq.getItemInOffHandDropChance());

        if (!items.isEmpty()) {
            b.put("Equipment", ListBinaryTag.from(items));
        }
    }

    private static void tryAddEquipment(List<CompoundBinaryTag> list, ItemStack item, EquipmentSlot slot, float dropChance) {
        if (item != null && item.getType() != Material.AIR && RANDOM.nextFloat() <= dropChance) {
            CompoundBinaryTag tag = MyPetApi.getPlatformHelper().itemStackToCompound(item)
                    .putString("Slot", slot.name());
            list.add(tag);
        }
    }
}
