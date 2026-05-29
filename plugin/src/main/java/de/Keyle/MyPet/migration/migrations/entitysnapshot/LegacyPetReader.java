/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.migration.migrations.entitysnapshot;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.util.CompatUtil;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import io.papermc.paper.world.WeatheringCopperState;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Mule;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Strider;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import de.Keyle.MyPet.entity.types.PetCopperGolem;

/**
 * Single entry point for applying a legacy (pre-EntitySnapshot) pet info
 * compound to a live Bukkit {@link Mob}. Used by:
 *
 * <ul>
 *   <li>{@code EntitySnapshotMigration} — once at startup, to convert legacy
 *       DB rows into envelope format (spawn transient mob, apply legacy info,
 *       capture vanilla NBT, write envelope back).</li>
 *   <li>{@code VanillaMobSpawner#spawn} — transitionally, when a player joins
 *       between plugin enable and bulk-migration completion and their pet's
 *       row is still in legacy format.</li>
 * </ul>
 *
 * <p>Holds every per-type legacy reader plus the universal {@code Equipment}
 * list and {@code Baby} flag handler that used to live in
 * {@code Pet#readExtendedInfo}. Production code is legacy-free; this class
 * (and the rest of {@code plugin/migration/migrations/entitysnapshot/}) is
 * scheduled for deletion at v5 once every server has had at least one boot to
 * complete the bulk migration.
 *
 * <p>Several pet types share legacy NBT shapes (Llama / TraderLlama,
 * Piglin / PiglinBrute, Donkey / Mule horse-chest carriers, etc.); those
 * reuse private helpers below rather than each keeping their own copy.
 */
public final class LegacyPetReader {

    private static final String[] LEGACY_CAT_ORDINAL_KEYS = {
            "tabby", "black", "red", "siamese", "british_shorthair",
            "calico", "persian", "ragdoll", "white", "jellie", "all_black"
    };

    private static final String[] LEGACY_VILLAGER_TYPE_KEYS = {
            "desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"
    };

    private LegacyPetReader() {
    }

    /**
     * Applies the legacy info compound to the given mob. The mob must already
     * have been spawned and be of the Bukkit class that matches {@code petType}.
     */
    public static void applyToMob(Mob mob, PetType petType, CompoundBinaryTag info) {
        try {
            applyTypeSpecific(mob, petType, info);
            LegacyUniversalReader.apply(mob, info);
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("LegacyPetReader.applyToMob: legacy reader for "
                    + petType.name() + " threw " + t.getClass().getSimpleName()
                    + " — mob may be missing some legacy state. " + t.getMessage());
        }
    }

    private static void applyTypeSpecific(Mob mob, PetType petType, CompoundBinaryTag info) {
        switch (petType.name()) {
            case "Axolotl" -> applyAxolotl(mob, info);
            case "Bee" -> applyBee(mob, info);
            case "Blaze" -> applyBlaze(mob, info);
            case "Camel" -> applyCamel(mob, info);
            case "Cat" -> applyCat(mob, info);
            case "Chicken" -> applyChicken(mob, info);
            case "CopperGolem" -> applyCopperGolem(mob, info);
            case "Cow" -> applyCow(mob, info);
            case "Creeper" -> applyCreeper(mob, info);
            case "Donkey", "Mule" -> applyHorseChestCarrier(mob, info);
            case "Enderman" -> applyEnderman(mob, info);
            case "Fox" -> applyFox(mob, info);
            case "Frog" -> applyFrog(mob, info);
            case "Goat" -> applyGoat(mob, info);
            case "Hoglin" -> applyShakeImmuneFlag(mob, info);
            case "Horse" -> applyHorse(mob, info);
            case "Llama", "TraderLlama" -> applyLlamaFamily(mob, info);
            case "MagmaCube" -> applySize(mob, info, true);
            case "Mooshroom" -> applyMooshroom(mob, info);
            case "Panda" -> applyPanda(mob, info);
            case "Parrot" -> applyParrot(mob, info);
            case "Phantom" -> applySize(mob, info, true);
            case "Pig" -> applyPig(mob, info);
            case "Piglin", "PiglinBrute" -> applyShakeImmuneFlag(mob, info);
            case "Pufferfish" -> applyPufferfish(mob, info);
            case "Rabbit" -> applyRabbit(mob, info);
            case "Salmon" -> applySalmon(mob, info);
            case "Sheep" -> applySheep(mob, info);
            case "SkeletonHorse", "ZombieHorse" -> applyHorseSaddleOnly(mob, info);
            case "Slime" -> applySize(mob, info, true);
            case "SnowGolem" -> applySnowGolem(mob, info);
            case "Strider" -> applyStrider(mob, info);
            case "TropicalFish" -> applyTropicalFish(mob, info);
            case "Vex" -> applyVex(mob, info);
            case "Villager" -> applyVillager(mob, info);
            case "Wolf" -> applyWolf(mob, info);
            case "ZombieVillager" -> applyZombieVillager(mob, info);
            default -> { /* type has no per-type legacy keys */ }
        }
    }

    // ─── Per-type readers ──────────────────────────────────────────────────

    private static void applyAxolotl(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Axolotl axolotl)) return;
        Axolotl.Variant variant = enumByName(info, "VariantName", Axolotl.Variant.class);
        if (variant == null) variant = enumByOrdinal(info, "Variant", Axolotl.Variant.values());
        if (variant != null) axolotl.setVariant(variant);
    }

    private static void applyBee(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Bee bee)) return;
        if (info.keySet().contains("HasNectar")) bee.setHasNectar(info.getBoolean("HasNectar"));
        if (info.keySet().contains("HasStung")) bee.setHasStung(info.getBoolean("HasStung"));
        if (info.keySet().contains("Angry")) bee.setAnger(info.getBoolean("Angry") ? 400 : 0);
    }

    private static void applyBlaze(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Blaze blaze)) return;
        // Bukkit's Entity#setVisualFire flips the ON_FIRE flag without
        // subjecting the mob to fire damage.
        if (info.keySet().contains("Fire") && info.getBoolean("Fire")) blaze.setVisualFire(true);
    }

    private static void applyCamel(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Camel camel)) return;
        ItemStack saddle = readSaddleItem(info);
        if (saddle != null) camel.getInventory().setSaddle(saddle);
    }

    private static void applyCat(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Cat cat)) return;
        String catTypeKey = null;
        if (info.keySet().contains("CatTypeKey")) {
            String key = info.getString("CatTypeKey");
            if (!key.isEmpty()) catTypeKey = key;
        } else if (info.keySet().contains("CatType")) {
            try {
                int ord = info.getInt("CatType");
                if (ord >= 0 && ord < LEGACY_CAT_ORDINAL_KEYS.length) {
                    catTypeKey = LEGACY_CAT_ORDINAL_KEYS[ord];
                }
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Failed to migrate legacy Cat variant ordinal: " + e.getMessage());
            }
        }
        if (catTypeKey != null) {
            Cat.Type type = Registry.CAT_VARIANT.get(NamespacedKey.minecraft(catTypeKey));
            if (type != null) cat.setCatType(type);
        }
        if (info.keySet().contains("CollarColor")) {
            BinaryTag collar = info.get("CollarColor");
            if (collar instanceof IntBinaryTag) {
                cat.setCollarColor(DyeColor.values()[info.getInt("CollarColor")]);
            } else if (collar instanceof ByteBinaryTag) {
                cat.setCollarColor(DyeColor.values()[info.getByte("CollarColor")]);
            }
        }
        if (info.keySet().contains("Tamed")) cat.setTamed(info.getBoolean("Tamed"));
    }

    private static void applyChicken(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Chicken chicken)) return;
        // Chicken.setVariant + RegistryKey.CHICKEN_VARIANT were added in 1.21.5;
        // calling either on a 1.20.5–1.21.4 runtime fails with LinkageError.
        try {
            Chicken.Variant variant = registryByKey(info, "Variant", RegistryKey.CHICKEN_VARIANT);
            if (variant != null) chicken.setVariant(variant);
        } catch (LinkageError ignored) {
        }
    }

    private static void applyCopperGolem(Mob mob, CompoundBinaryTag info) {
        if (!CompatUtil.minecraftVersionEqualsOrAbove("1.21.9")) return;
        // CopperGolem.Oxidizing lives in the nested class, not here: the migration
        // scanner force-loads LegacyPetReader (Class.forName initialize=true), which
        // verifies every method. Keeping the typed refs out of this class's bytecode
        // lets it load on pre-1.21.9 servers; the nested class only links when called.
        CopperGolemApplier.apply(mob, info);
    }

    private static final class CopperGolemApplier {
        private static void apply(Mob mob, CompoundBinaryTag info) {
            if (!(mob instanceof CopperGolem golem)) return;
            if (info.keySet().contains("OxidationState")) {
                try {
                    golem.setWeatheringState(WeatheringCopperState.valueOf(info.getString("OxidationState")));
                } catch (Throwable ignored) {}
            }
            boolean waxed = info.keySet().contains("Waxed") && info.getBoolean("Waxed");
            long remaining = info.keySet().contains("OxidationRemainingTicks")
                    ? Math.max(0L, info.getLong("OxidationRemainingTicks")) : 0L;
            // Suppress vanilla's natural oxidation tick when waxed or admin-disabled.
            // Otherwise resume the saved schedule (atTime offset by remaining ticks)
            // so legacy data preserves oxidation progress; fall through to Unset on
            // a fresh tame so vanilla picks its own first schedule.
            CopperGolem.Oxidizing oxidizing;
            if (waxed || !PetCopperGolem.CAN_OXIDIZE.get()) {
                oxidizing = CopperGolem.Oxidizing.waxed();
            } else if (remaining > 0) {
                oxidizing = CopperGolem.Oxidizing.atTime(golem.getWorld().getFullTime() + remaining);
            } else {
                oxidizing = CopperGolem.Oxidizing.unset();
            }
            golem.setOxidizing(oxidizing);
            if (info.keySet().contains("Poppy")) {
                try {
                    ItemStack stack = LegacyNbtItemDecoder.decode(info.getCompound("Poppy"));
                    if (stack != null) golem.getEquipment().setItem(EquipmentSlot.HAND, stack);
                } catch (Exception ignored) {}
            }
        }
    }

    private static void applyCow(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Cow cow)) return;
        // Cow.setVariant + RegistryKey.COW_VARIANT were added in 1.21.5; calling
        // either on a 1.20.5–1.21.4 runtime fails with LinkageError.
        try {
            Cow.Variant variant = registryByKey(info, "Variant", RegistryKey.COW_VARIANT);
            if (variant != null) cow.setVariant(variant);
        } catch (LinkageError ignored) {
        }
    }

    private static void applyCreeper(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Creeper creeper)) return;
        if (info.keySet().contains("Powered")) creeper.setPowered(info.getBoolean("Powered"));
    }

    private static void applyEnderman(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Enderman enderman)) return;
        if (info.keySet().contains("BlockName")) {
            Material material = Material.matchMaterial(info.getString("BlockName"));
            if (material != null) {
                try { enderman.setCarriedBlock(material.createBlockData()); } catch (Throwable ignored) {}
            }
        } else if (info.keySet().contains("Block")) {
            try {
                ItemStack stack = LegacyNbtItemDecoder.decode(info.getCompound("Block"));
                if (stack != null && stack.getType() != Material.AIR) {
                    enderman.setCarriedBlock(stack.getType().createBlockData());
                }
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Block item from pet data!");
            }
        }
        if (info.keySet().contains("Screaming") && info.getBoolean("Screaming")) {
            // Pre-v4 "Screaming" was a plugin-only perma flag. Vanilla
            // Enderman screaming is AI-driven and does not round-trip through
            // serializeEntity, so this set is best-effort only.
            enderman.setScreaming(true);
        }
    }

    private static void applyFox(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Fox fox)) return;
        // Storage by enum name (drift-safe across reorder); pre-v4 used ordinal.
        Fox.Type type = enumByName(info, "FoxTypeName", Fox.Type.class);
        if (type == null) type = enumByOrdinal(info, "FoxType", Fox.Type.values());
        if (type != null) fox.setFoxType(type);
    }

    private static void applyFrog(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Frog frog)) return;
        Frog.Variant variant = enumByName(info, "FrogTypeName", Frog.Variant.class);
        if (variant == null) variant = enumByOrdinal(info, "FrogType", Frog.Variant.values());
        if (variant != null) frog.setVariant(variant);
    }

    private static void applyGoat(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Goat goat)) return;
        if (info.keySet().contains("Screaming")) goat.setScreaming(info.getBoolean("Screaming"));
        if (info.keySet().contains("LeftHorn")) goat.setLeftHorn(info.getBoolean("LeftHorn"));
        if (info.keySet().contains("RightHorn")) goat.setRightHorn(info.getBoolean("RightHorn"));
    }

    private static void applyHorse(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Horse horse)) return;
        applyHorseColorAndStyle(info, horse);
        // Saddle / armor: legacy stored as ItemStack compounds, possibly under
        // top-level "Saddle"/"Armor" keys (pre-v4 split storage).
        ItemStack saddle = readSaddleItem(info);
        if (saddle != null) horse.getInventory().setSaddle(saddle);
        if (info.keySet().contains("Armor")) {
            ItemStack armor = readItem(info, "Armor");
            if (armor != null) horse.getInventory().setArmor(armor);
        }
    }

    private static void applyMooshroom(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof MushroomCow mooshroom)) return;
        MushroomCow.Variant variant = enumByName(info, "CowTypeName", MushroomCow.Variant.class);
        if (variant != null) mooshroom.setVariant(variant);
    }

    private static void applyPanda(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Panda panda)) return;
        Panda.Gene main = readPandaGene(info, "MainGeneName", "MainGene");
        if (main != null) panda.setMainGene(main);
        Panda.Gene hidden = readPandaGene(info, "HiddenGeneName", "HiddenGene");
        if (hidden != null) panda.setHiddenGene(hidden);
    }

    private static void applyParrot(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Parrot parrot)) return;
        Parrot.Variant variant = enumByName(info, "VariantName", Parrot.Variant.class);
        if (variant == null) variant = enumByOrdinal(info, "Variant", Parrot.Variant.values());
        if (variant != null) parrot.setVariant(variant);
    }

    private static void applyPig(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Pig pig)) return;
        // Legacy stored Saddle as either a boolean (pre-v4) or an ItemStack
        // compound (v4 dev builds). Bukkit's Pig only cares about the boolean.
        if (info.keySet().contains("Saddle")) {
            boolean saddled = info.get("Saddle") instanceof CompoundBinaryTag
                    ? !info.getCompound("Saddle").keySet().isEmpty()
                    : info.getBoolean("Saddle");
            pig.setSaddle(saddled);
        }
    }

    private static void applyPufferfish(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof PufferFish puffer)) return;
        if (info.keySet().contains("PuffState")) {
            puffer.setPuffState(Math.max(0, Math.min(2, info.getInt("PuffState"))));
        }
    }

    private static void applyRabbit(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Rabbit rabbit)) return;
        Rabbit.Type type = enumByName(info, "VariantName", Rabbit.Type.class);
        if (type != null) rabbit.setRabbitType(type);
    }

    private static void applySalmon(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Salmon salmon)) return;
        // Salmon.setVariant + Salmon.Variant were added in 1.21.2; referencing
        // Salmon.Variant.values() (or the setter) on a 1.20.5–1.21.1 runtime
        // fails with LinkageError.
        try {
            Salmon.Variant variant = enumByOrdinal(info, "Variant", Salmon.Variant.values());
            if (variant != null) salmon.setVariant(variant);
        } catch (LinkageError ignored) {
        }
    }

    private static void applySheep(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Sheep sheep)) return;
        if (info.keySet().contains("Color")) {
            DyeColor color = DyeColor.getByDyeData(info.getByte("Color"));
            if (color != null) sheep.setColor(color);
        }
        if (info.keySet().contains("Sheared")) sheep.setSheared(info.getBoolean("Sheared"));
    }

    private static void applySnowGolem(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Snowman snowman)) return;
        if (info.keySet().contains("Sheared")) snowman.setDerp(info.getBoolean("Sheared"));
    }

    private static void applyStrider(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Strider strider)) return;
        if (info.keySet().contains("Saddle")) {
            BinaryTag saddleTag = info.get("Saddle");
            boolean saddled;
            if (saddleTag instanceof ByteBinaryTag) saddled = info.getBoolean("Saddle");
            else if (saddleTag instanceof CompoundBinaryTag) saddled = !info.getCompound("Saddle").keySet().isEmpty();
            else saddled = false;
            strider.setSaddle(saddled);
        }
    }

    private static void applyTropicalFish(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof TropicalFish fish)) return;
        if (info.keySet().contains("Variant")) {
            try {
                int packed = info.getInt("Variant");
                TropicalFish.Pattern[] patterns = TropicalFish.Pattern.values();
                fish.setPattern(patterns[Math.floorMod((packed >> 8) & 0xFF, patterns.length)]);
            } catch (Throwable ignored) {}
        }
    }

    private static void applyVex(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Vex vex)) return;
        // Vex's "glowing" attack pose is exposed via setCharging — when true
        // the entity flares red as if mid-attack.
        if (info.keySet().contains("Glowing")) vex.setCharging(info.getBoolean("Glowing"));
    }

    private static void applyVillager(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Villager villager)) return;
        Villager.Profession prof = readVillagerProfession(info);
        if (prof != null) villager.setProfession(prof);
        Villager.Type type = readVillagerType(info);
        if (type != null) villager.setVillagerType(type);
        if (info.keySet().contains("VillagerLevel")) {
            villager.setVillagerLevel(Math.max(1, Math.min(5, info.getInt("VillagerLevel"))));
        }
    }

    private static void applyWolf(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Wolf wolf)) return;
        if (info.keySet().contains("CollarColor")) {
            wolf.setCollarColor(DyeColor.values()[info.getByte("CollarColor")]);
        }
        if (info.keySet().contains("Tamed")) wolf.setTamed(info.getBoolean("Tamed"));
        if (info.keySet().contains("Angry")) wolf.setAngry(info.getBoolean("Angry"));
        if (info.keySet().contains("Variant")) {
            Wolf.Variant variant = Registry.WOLF_VARIANT.get(
                    NamespacedKey.minecraft(info.getString("Variant").toLowerCase(Locale.ROOT)));
            if (variant != null) wolf.setVariant(variant);
        }
    }

    private static void applyZombieVillager(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof ZombieVillager zv)) return;
        Villager.Profession prof = readVillagerProfession(info);
        if (prof != null) zv.setVillagerProfession(prof);
        Villager.Type type = readVillagerType(info);
        if (type != null) zv.setVillagerType(type);
        // TradingLevel was a Pet-only concept; vanilla zombie villagers
        // don't expose a trade level.
    }

    // ─── Shared helpers (multi-type) ───────────────────────────────────────

    /** Donkey + Mule: optional saddle item + chest carrier flag. */
    private static void applyHorseChestCarrier(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof AbstractHorse horse)) return;
        ItemStack saddle = readSaddleItem(info);
        if (saddle != null) horse.getInventory().setSaddle(saddle);
        boolean carry = false;
        BinaryTag chestTag = info.get("Chest");
        if (chestTag != null) {
            if (chestTag.type() == BinaryTagTypes.BYTE) {
                carry = info.getBoolean("Chest");
            } else if (chestTag.type() == BinaryTagTypes.COMPOUND) {
                ItemStack chestItem = readItem(info, "Chest");
                carry = chestItem != null && (chestItem.getType() == Material.CHEST
                        || chestItem.getType() == Material.TRAPPED_CHEST);
            }
        }
        if (mob instanceof Donkey d) d.setCarryingChest(carry);
        else if (mob instanceof Mule m) m.setCarryingChest(carry);
    }

    /** Llama + TraderLlama: same legacy color, chest, and decor schema. */
    private static void applyLlamaFamily(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof Llama llama)) return;
        Llama.Color color = enumByName(info, "ColorName", Llama.Color.class);
        if (color == null) {
            // Pre-v4 packed the color into the low byte of "Variant".
            if (info.keySet().contains("Variant")) {
                try {
                    int idx = info.getInt("Variant") & 0xFF;
                    Llama.Color[] values = Llama.Color.values();
                    color = values[Math.min(values.length - 1, Math.max(0, idx))];
                } catch (Throwable ignored) {}
            }
        }
        if (color != null) llama.setColor(color);
        if (info.keySet().contains("Chest")) {
            if (info.get("Chest") instanceof CompoundBinaryTag) {
                ItemStack item = readItem(info, "Chest");
                if (item != null) {
                    if (mob instanceof TraderLlama tl) tl.setCarryingChest(true);
                    else llama.setCarryingChest(true);
                }
            } else {
                boolean carry = info.getBoolean("Chest");
                if (mob instanceof TraderLlama tl) tl.setCarryingChest(carry);
                else llama.setCarryingChest(carry);
            }
        }
        if (info.keySet().contains("Decor")) {
            ItemStack decor = readItem(info, "Decor");
            if (decor != null && decor.getType().name().endsWith("CARPET")) {
                llama.getInventory().setDecor(decor);
            }
        }
    }

    /** SkeletonHorse + ZombieHorse: only saddle, no chest / armor. */
    private static void applyHorseSaddleOnly(Mob mob, CompoundBinaryTag info) {
        if (!(mob instanceof AbstractHorse horse)) return;
        ItemStack saddle = readSaddleItem(info);
        if (saddle != null) horse.getInventory().setSaddle(saddle);
    }

    /** Hoglin + Piglin + PiglinBrute: zombification-immunity flag. */
    private static void applyShakeImmuneFlag(Mob mob, CompoundBinaryTag info) {
        if (!info.keySet().contains("ShakeImmune")) return;
        boolean immune = info.getBoolean("ShakeImmune");
        if (mob instanceof Hoglin hoglin) hoglin.setImmuneToZombification(immune);
        else if (mob instanceof PiglinAbstract piglin) piglin.setImmuneToZombification(immune);
    }

    /** MagmaCube / Phantom / Slime: int "Size" with min-1 floor. */
    private static void applySize(Mob mob, CompoundBinaryTag info, boolean clampToOne) {
        if (!info.keySet().contains("Size")) return;
        int size = clampToOne ? Math.max(1, info.getInt("Size")) : info.getInt("Size");
        if (mob instanceof Slime slime) slime.setSize(size);
        else if (mob instanceof Phantom phantom) phantom.setSize(size);
        else if (mob instanceof MagmaCube cube) cube.setSize(size);
    }

    private static void applyHorseColorAndStyle(CompoundBinaryTag info, Horse horse) {
        Horse.Color color = enumByName(info, "ColorName", Horse.Color.class);
        Horse.Style style = enumByName(info, "StyleName", Horse.Style.class);
        // Pre-v4 packed both into one int: color in low byte, style in next byte.
        if ((color == null || style == null) && info.keySet().contains("Variant")) {
            try {
                int packed = info.getInt("Variant");
                if (color == null) {
                    Horse.Color[] colors = Horse.Color.values();
                    int idx = packed & 0xFF;
                    if (idx >= 0 && idx < colors.length) color = colors[idx];
                }
                if (style == null) {
                    Horse.Style[] styles = Horse.Style.values();
                    int idx = (packed >> 8) & 0xFF;
                    if (idx >= 0 && idx < styles.length) style = styles[idx];
                }
            } catch (Throwable ignored) {}
        }
        if (color != null) horse.setColor(color);
        if (style != null) horse.setStyle(style);
    }

    private static Panda.Gene readPandaGene(CompoundBinaryTag info, String nameKey, String legacyOrdinalKey) {
        if (info.keySet().contains(nameKey)) {
            String name = info.getString(nameKey);
            if (!name.isEmpty()) {
                try { return Panda.Gene.valueOf(name); } catch (Throwable ignored) {}
            }
        } else if (info.keySet().contains(legacyOrdinalKey)) {
            try {
                int ord = info.getInt(legacyOrdinalKey);
                Panda.Gene[] values = Panda.Gene.values();
                if (ord >= 0 && ord < values.length) return values[ord];
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Villager.Profession readVillagerProfession(CompoundBinaryTag info) {
        String key = null;
        if (info.keySet().contains("ProfessionKey")) {
            String s = info.getString("ProfessionKey");
            if (!s.isEmpty()) key = s;
        } else if (info.keySet().contains("Profession")) {
            try {
                int ord = info.getInt("Profession");
                Villager.Profession[] values = Villager.Profession.values();
                if (ord >= 0 && ord < values.length) {
                    Villager.Profession p = values[ord];
                    if (p.getKey() != null) key = p.getKey().getKey();
                }
            } catch (Throwable ignored) {}
        }
        if (key == null) return null;
        try {
            return Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(key));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Villager.Type readVillagerType(CompoundBinaryTag info) {
        String keyPath = null;
        if (info.keySet().contains("VillagerTypeKey")) {
            String s = info.getString("VillagerTypeKey");
            if (!s.isEmpty()) keyPath = s;
        } else if (info.keySet().contains("VillagerType")) {
            try {
                int ord = info.getInt("VillagerType");
                if (ord >= 0 && ord < LEGACY_VILLAGER_TYPE_KEYS.length) {
                    keyPath = LEGACY_VILLAGER_TYPE_KEYS[ord];
                }
            } catch (Throwable ignored) {}
        }
        if (keyPath == null) return null;
        try {
            return Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(keyPath.toLowerCase(Locale.ROOT)));
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ─── Generic helpers ───────────────────────────────────────────────────

    /**
     * Lookup a Bukkit type by its name(). Works for true {@link Enum} types
     * AND for registry-keyed Bukkit interfaces that retain a deprecated
     * {@code valueOf(String)} static method (e.g. {@code Frog.Variant},
     * {@code Salmon.Variant}). The reflection allows both shapes without
     * having to touch the call site when Mojang flips a type from enum to
     * registry between MC versions.
     */
    @SuppressWarnings("unchecked")
    private static <T> T enumByName(CompoundBinaryTag info, String key, Class<T> cls) {
        if (!info.keySet().contains(key)) return null;
        String name = info.getString(key);
        if (name.isEmpty()) return null;
        try {
            return (T) cls.getMethod("valueOf", String.class).invoke(null, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Pre-v4 ordinal fallback — clamps to range to survive type extension. */
    private static <T> T enumByOrdinal(CompoundBinaryTag info, String key, T[] values) {
        if (!info.keySet().contains(key)) return null;
        try {
            int ord = info.getInt(key);
            int clamped = Math.min(values.length - 1, Math.max(0, ord));
            return values[clamped];
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Registry lookup by NamespacedKey stored as a string under {@code key}. */
    private static <T extends org.bukkit.Keyed> T registryByKey(CompoundBinaryTag info, String key, RegistryKey<T> registry) {
        if (!info.keySet().contains(key)) return null;
        String name = info.getString(key);
        if (name.isEmpty()) return null;
        try {
            return RegistryAccess.registryAccess().getRegistry(registry).get(Key.key(name.toLowerCase(Locale.ROOT)));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Read an ItemStack from a "Slot"-keyed compound. Returns null on missing/empty. */
    private static ItemStack readItem(CompoundBinaryTag info, String key) {
        try {
            CompoundBinaryTag itemTag = info.getCompound(key);
            if (itemTag.keySet().isEmpty()) return null;
            return LegacyNbtItemDecoder.decode(itemTag);
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("Could not load " + key + " item from pet data!");
            return null;
        }
    }

    /** Read the legacy "Saddle" compound and only return it if it's actually a SADDLE. */
    private static ItemStack readSaddleItem(CompoundBinaryTag info) {
        if (!info.keySet().contains("Saddle")) return null;
        ItemStack saddle = readItem(info, "Saddle");
        return saddle != null && saddle.getType() == Material.SADDLE ? saddle : null;
    }
}
