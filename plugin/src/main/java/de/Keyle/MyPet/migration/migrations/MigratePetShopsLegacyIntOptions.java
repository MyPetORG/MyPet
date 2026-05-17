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

package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.migration.ConfigMigration;
import de.Keyle.MyPet.migration.ConfigMigrationContext;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Converts pre-alpha-11 {@code pet-shops.yml} {@code Options:} entries
 * from int-ordinal form (e.g. {@code variant:2}, {@code color:14}) to the
 * string form the alpha-11 runtime accepts (e.g. {@code variant:gold},
 * {@code color:red}).
 *
 * <p>Walks {@code Shops.*.Pets.*}, identifies the pet by its
 * {@code PetType} field, and rewrites the {@code Options} list per the
 * mappings below. The migration is idempotent — string-form values pass
 * through unchanged.
 *
 * <p><b>Per-pet rewrites:</b>
 * <ul>
 *   <li>Axolotl / Frog / Parrot / Llama / TraderLlama / Salmon:
 *       {@code variant:N} → {@code variant:<enum-name>}</li>
 *   <li>Rabbit: {@code variant:N} → {@code variant:<type-name>}, with
 *       the special {@code variant:99} → {@code variant:the_killer_bunny}</li>
 *   <li>Horse: {@code variant:N} (packed: low byte = color, high byte =
 *       style) → two entries {@code color:<name>} + {@code style:<name>}</li>
 *   <li>TropicalFish: {@code variant:N} (packed: pattern in byte 1, body
 *       and pattern colors discarded by the legacy reader) → single
 *       {@code pattern:<name>} entry. Body and pattern colors aren't
 *       recovered (they were never persisted to the option string).</li>
 *   <li>Sheep: {@code color:N} → {@code color:<dye-name>}</li>
 *   <li>Cat: {@code collar:N} → {@code collar:<dye-name>}; also
 *       <b>key-rename</b> {@code type:N} → {@code variant:<cat-type-name>}
 *       (Cat's variant key was renamed from {@code type:} in alpha-11).</li>
 *   <li>Wolf: {@code collar:N} → {@code collar:<dye-name>}</li>
 *   <li>Villager / ZombieVillager: {@code profession:N} →
 *       {@code profession:<key>}; {@code type:N} → {@code type:<key>}</li>
 *   <li>Fox: {@code type:white} → {@code type:snow} (matches the actual
 *       {@code Fox.Type.SNOW} enum constant name)</li>
 *   <li>Hoglin / Piglin / PiglinBrute: {@code noshake} → <b>dropped</b>
 *       (zombification immunity moved to the {@code AllowZombification}
 *       config option in alpha 7)</li>
 *   <li>SnowGolem: {@code sheared} → {@code derp} (matches
 *       {@code Snowman.setDerp})</li>
 * </ul>
 *
 * <p>Anything not matching one of the rules above is left unchanged. A
 * line is logged for every entry actually rewritten.
 */
@Migration(
        version = "4.0.0-alpha-11",
        description = "Convert pre-alpha-11 int-form pet-shops.yml Options to string form"
)
public class MigratePetShopsLegacyIntOptions implements ConfigMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");
    private static final String PET_SHOPS = "pet-shops.yml";

    // Ordinal → name arrays. Order matches the legacy `EnumType.values()` /
    // ordinal-to-Bukkit-API dispatch each pet went through pre-alpha-11.

    private static final String[] AXOLOTL_VARIANTS = {
            "lucy", "wild", "gold", "cyan", "blue"
    };
    private static final String[] FROG_VARIANTS = {
            "temperate", "warm", "cold"
    };
    private static final String[] PARROT_VARIANTS = {
            "red", "blue", "green", "cyan", "gray"
    };
    private static final String[] RABBIT_TYPES = {
            "brown", "white", "black", "black_and_white", "gold", "salt_and_pepper"
            // 99 → "the_killer_bunny" — special-cased below
    };
    private static final String[] LLAMA_COLORS = {
            "creamy", "white", "brown", "gray"
    };
    private static final String[] SALMON_VARIANTS = {
            "small", "medium", "large"
    };
    private static final String[] HORSE_COLORS = {
            "white", "creamy", "chestnut", "brown", "black", "gray", "dark_brown"
    };
    private static final String[] HORSE_STYLES = {
            "none", "white", "whitefield", "white_dots", "black_dots"
    };
    private static final String[] TROPICAL_FISH_PATTERNS = {
            "kob", "sunstreak", "snooper", "dasher", "brinely", "spotty",
            "flopper", "stripey", "glitter", "blockfish", "betty", "clayfish"
    };
    private static final String[] DYE_COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black"
    };
    private static final String[] VILLAGER_PROFESSIONS = {
            "none", "armorer", "butcher", "cartographer", "cleric", "farmer",
            "fisherman", "fletcher", "leatherworker", "librarian", "mason",
            "nitwit", "shepherd", "toolsmith", "weaponsmith"
    };
    private static final String[] VILLAGER_TYPES = {
            "desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"
    };
    private static final String[] CAT_TYPES = {
            "tabby", "black", "red", "siamese", "british_shorthair",
            "calico", "persian", "ragdoll", "white", "jellie", "all_black"
    };

    private int converted;

    @Override
    public void migrate(ConfigMigrationContext ctx) throws MigrationException {
        if (!new File(ctx.getDataFolder(), PET_SHOPS).exists()) {
            return;
        }
        YamlConfiguration config = ctx.getConfig(PET_SHOPS);
        ConfigurationSection shops = config.getConfigurationSection("Shops");
        if (shops == null) {
            return;
        }

        converted = 0;
        boolean changed = false;
        for (String shopKey : shops.getKeys(false)) {
            ConfigurationSection shop = shops.getConfigurationSection(shopKey);
            if (shop == null) continue;
            ConfigurationSection pets = shop.getConfigurationSection("Pets");
            if (pets == null) continue;
            for (String petKey : pets.getKeys(false)) {
                ConfigurationSection pet = pets.getConfigurationSection(petKey);
                if (pet == null) continue;
                String petTypeName = pet.getString("PetType");
                if (petTypeName == null) continue;
                List<String> options = pet.getStringList("Options");
                if (options.isEmpty()) continue;

                List<String> rewritten = rewriteOptions(petTypeName, options);
                if (!rewritten.equals(options)) {
                    pet.set("Options", rewritten);
                    changed = true;
                    LOG.info("Migrated " + shopKey + "/" + petKey + " (" + petTypeName
                            + ") " + options + " → " + rewritten);
                }
            }
        }

        if (changed) {
            ctx.saveConfig(PET_SHOPS);
        }
        LOG.info("Pet-shops legacy int-options migration complete ("
                + converted + " entries converted).");
    }

    private List<String> rewriteOptions(String petTypeName, List<String> options) {
        List<String> out = new ArrayList<>(options.size());
        for (String option : options) {
            List<String> rewritten = rewriteOption(petTypeName, option);
            if (rewritten == null) {
                converted++;  // dropped
                continue;
            }
            if (rewritten.size() != 1 || !rewritten.get(0).equals(option)) {
                converted++;  // changed
            }
            out.addAll(rewritten);
        }
        return out;
    }

    /**
     * Rewrites a single option string. Returns:
     * <ul>
     *   <li>A list of one or more new entries when the option is rewritten.</li>
     *   <li>A single-element list containing the original option when no
     *       rewrite applies (already migrated, unknown shape, etc.).</li>
     *   <li>{@code null} to drop the option entirely (orphan {@code noshake}).</li>
     * </ul>
     */
    private List<String> rewriteOption(String petTypeName, String option) {
        int colon = option.indexOf(':');
        String key = colon < 0 ? option : option.substring(0, colon);
        String value = colon < 0 ? "" : option.substring(colon + 1);

        switch (petTypeName) {
            case "Axolotl":
                if ("variant".equals(key)) return ordinalRewrite("variant", value, AXOLOTL_VARIANTS, option);
                break;
            case "Frog":
                if ("variant".equals(key)) return ordinalRewrite("variant", value, FROG_VARIANTS, option);
                break;
            case "Parrot":
                if ("variant".equals(key)) return ordinalRewrite("variant", value, PARROT_VARIANTS, option);
                break;
            case "Rabbit":
                if ("variant".equals(key)) return rewriteRabbitVariant(value, option);
                break;
            case "Llama":
            case "TraderLlama":
                if ("variant".equals(key)) return ordinalRewrite("variant", value, LLAMA_COLORS, option);
                break;
            case "Salmon":
                if ("variant".equals(key)) return ordinalRewrite("variant", value, SALMON_VARIANTS, option);
                break;
            case "Horse":
                if ("variant".equals(key)) return rewriteHorsePackedVariant(value, option);
                break;
            case "TropicalFish":
                if ("variant".equals(key)) return rewriteTropicalFishPackedVariant(value, option);
                break;
            case "Sheep":
                if ("color".equals(key)) return ordinalRewrite("color", value, DYE_COLORS, option);
                break;
            case "Cat":
                if ("collar".equals(key)) return ordinalRewrite("collar", value, DYE_COLORS, option);
                if ("type".equals(key)) {
                    // Key rename + value rewrite: pre-v4 Cat used type:N; v4
                    // renamed the option key to variant: (matches other
                    // registry-backed pets).
                    if (!Util.isInt(value)) return List.of(option);
                    int n = Integer.parseInt(value);
                    if (n < 0 || n >= CAT_TYPES.length) return List.of(option);
                    return List.of("variant:" + CAT_TYPES[n]);
                }
                break;
            case "Wolf":
                if ("collar".equals(key)) return ordinalRewrite("collar", value, DYE_COLORS, option);
                break;
            case "Villager":
            case "ZombieVillager":
                if ("profession".equals(key)) return ordinalRewrite("profession", value, VILLAGER_PROFESSIONS, option);
                if ("type".equals(key)) return ordinalRewrite("type", value, VILLAGER_TYPES, option);
                break;
            case "Fox":
                // type:white → type:snow (matches Fox.Type.SNOW enum constant).
                // Other Fox type values (red) and non-type options are unchanged.
                if ("type".equals(key) && "white".equalsIgnoreCase(value)) {
                    return List.of("type:snow");
                }
                break;
            case "Hoglin":
            case "Piglin":
            case "PiglinBrute":
                // noshake was an admin override of the same vanilla
                // zombification immunity now handled by the
                // AllowZombification config option. Drop the orphan entry —
                // match on the parsed key so that hand-edited variants like
                // "noshake:true" are also caught.
                if ("noshake".equalsIgnoreCase(key)) return null;
                break;
            case "SnowGolem":
                // Match on key (not full option) so hand-edited "sheared:true"
                // is also renamed; the runtime only sets derp = true.
                if ("sheared".equalsIgnoreCase(key)) return List.of("derp");
                break;
        }
        return List.of(option);
    }

    /**
     * Helper for the common {@code key:N → key:names[N]} pattern. Returns
     * the original option unchanged when the value isn't an int (already
     * migrated) or the ordinal is out of range (corrupted config).
     */
    private List<String> ordinalRewrite(String key, String value, String[] names, String original) {
        if (!Util.isInt(value)) return List.of(original);
        int n = Integer.parseInt(value);
        if (n < 0 || n >= names.length) return List.of(original);
        return List.of(key + ":" + names[n]);
    }

    private List<String> rewriteRabbitVariant(String value, String original) {
        if (!Util.isInt(value)) return List.of(original);
        int n = Integer.parseInt(value);
        if (n == 99) return List.of("variant:the_killer_bunny");
        if (n < 0 || n >= RABBIT_TYPES.length) return List.of(original);
        return List.of("variant:" + RABBIT_TYPES[n]);
    }

    private List<String> rewriteHorsePackedVariant(String value, String original) {
        if (!Util.isInt(value)) return List.of(original);
        int packed = Math.max(0, Math.min(1030, Integer.parseInt(value)));
        int colorIdx = Math.floorMod(packed & 0xFF, HORSE_COLORS.length);
        int styleIdx = Math.floorMod((packed >> 8) & 0xFF, HORSE_STYLES.length);
        return List.of("color:" + HORSE_COLORS[colorIdx], "style:" + HORSE_STYLES[styleIdx]);
    }

    private List<String> rewriteTropicalFishPackedVariant(String value, String original) {
        if (!Util.isInt(value)) return List.of(original);
        int packed = Integer.parseInt(value);
        int patternIdx = Math.floorMod((packed >> 8) & 0xFF, TROPICAL_FISH_PATTERNS.length);
        // Body and pattern colors were discarded by the legacy reader, so we
        // don't synthesize body-color:/pattern-color: entries here — the
        // alpha-11 runtime falls back to default colors when those keys are
        // omitted, matching the pre-v4 visual behavior.
        return List.of("pattern:" + TROPICAL_FISH_PATTERNS[patternIdx]);
    }
}
