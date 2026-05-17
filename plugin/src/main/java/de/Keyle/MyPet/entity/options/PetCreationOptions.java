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

package de.Keyle.MyPet.entity.options;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.entity.PetChested;
import de.Keyle.MyPet.api.entity.PetSaddleable;
import de.Keyle.MyPet.api.entity.PetTameable;
import de.Keyle.MyPet.api.entity.PetType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility for translating per-pet creation-option strings (e.g.
 * {@code "variant:lucy"}, {@code "saddle"}, {@code "collar:red"}) into Bukkit
 * setter calls on a detached {@link Mob}, and for enumerating valid values
 * for tab-completion. Two entry points:
 *
 * <ul>
 *   <li>{@link #applyOptions} — invoked by {@code /petadmin create} and the
 *       petshop checkout via {@code PetEntitySnapshot.captureForOptions}.
 *       Returns the list of per-option validation errors (admin typos like
 *       {@code variant:nonexistent}). Wrong-mob-type options silently skip;
 *       unknown values surface as errors so callers can abort with feedback.
 *       </li>
 *   <li>{@link #optionsFor} — invoked by {@code CommandOptionCreate.suggestOptions}
 *       to populate Brigadier tab-completion. Produces a mixed list of bare
 *       keys ({@code "variant:"}) and value entries ({@code "variant:lucy"})
 *       so the command can two-stage the dropdown: keys before the colon is
 *       typed, values after.</li>
 * </ul>
 *
 * <p>Both entry points read from a single {@link #SPECS} table — one row per
 * {@code (option-key, pet-class)} pair, naming either an {@link Enum} class
 * or a {@link RegistryKey} as the value source plus a setter lambda. Each
 * row's {@link OptionSpec} knows how to do both apply (write side) and
 * expand (read side), so the two stay in lock-step.
 *
 * <p>{@link #SPECS} is populated as a side effect of each Pet class's
 * {@code CREATION_SPECS} static-field initializer calling {@link #specs}.
 * Adding a new variant-capable pet is one new {@code Pet<Type>} class with
 * one field — no central registration. See {@code PetAxolotl} for a
 * one-spec example, {@code PetWolf} for a two-spec example.
 *
 * <p>Five spec subtypes cover every option:
 * <ul>
 *   <li>{@link EnumOptionSpec} — backed by a {@link Enum} class (Axolotl
 *       variant, Horse color, ...).</li>
 *   <li>{@link RegistryOptionSpec} — backed by a Bukkit/Paper registry
 *       (Wolf variant, Villager profession, ...).</li>
 *   <li>{@link FlagOptionSpec} — value-less flag keywords (baby, tamed,
 *       saddle, ...). Auto-generated for pets that implement the
 *       matching marker interface ({@link PetBaby}, {@link PetTameable},
 *       {@link PetChested}, {@link PetSaddleable}).</li>
 *   <li>{@link CustomOptionSpec} — escape hatch for options that need
 *       custom value parsing (size: int clamp, block: Material lookup,
 *       puff: hardcoded literal mapping). Built via the
 *       {@link #sizeSpec}, {@link #blockSpec}, {@link #puffSpec}
 *       factory helpers.</li>
 * </ul>
 */
public final class PetCreationOptions {

    private PetCreationOptions() {}

    // =====================================================================
    // OptionSpec — one row per (option-key, pet-class). Drives both apply
    // (runtime) and expand (tab-completion).
    // =====================================================================

    public sealed interface OptionSpec
            permits EnumOptionSpec, RegistryOptionSpec, FlagOptionSpec, CustomOptionSpec {
        String key();
        Class<? extends Mob> mobType();
        String apply(PetType petType, String rawValue, Mob mob);
        List<String> expand(String prefix);

        /**
         * Returns {@code true} for value-less flag specs (e.g. {@code "baby"},
         * {@code "tamed"}, {@code "saddle"}). Drives the dispatch in
         * {@link #applyOption} (flag args have no colon) and the tab-completion
         * key formatting in {@link #optionsFor} (flags render without a
         * trailing colon).
         */
        default boolean isFlag() { return false; }

        static <M extends Mob, E extends Enum<E>> OptionSpec ofEnum(
                String key, Class<M> mobType, Class<E> enumClass, BiConsumer<M, E> setter) {
            return new EnumOptionSpec<>(key, mobType, enumClass, setter);
        }

        static <M extends Mob, T extends Keyed> OptionSpec ofRegistry(
                String key, Class<M> mobType, RegistryKey<T> registryKey, BiConsumer<M, T> setter) {
            return new RegistryOptionSpec<>(key, mobType, registryKey, setter);
        }

        static <M extends Mob> OptionSpec ofFlag(String key, Class<M> mobType, Consumer<M> action) {
            return new FlagOptionSpec<>(key, mobType, action);
        }

        /**
         * Escape hatch for {@code key:value} options that don't fit the enum,
         * registry, or flag patterns — int parsing with clamps, Material
         * lookups, hardcoded literal value lists, etc.
         *
         * @param values   supplier of tab-completion suggestions (empty list = no suggestions)
         * @param applyFn  receives the cast mob + raw value string; returns
         *                 an error message or {@code null} on success
         */
        static <M extends Mob> OptionSpec ofCustom(String key, Class<M> mobType,
                                                   Supplier<List<String>> values,
                                                   BiFunction<M, String, String> applyFn) {
            return new CustomOptionSpec<>(key, mobType, values, applyFn);
        }
    }

    /**
     * Spec backed by a Java {@link Enum}. Apply: {@code Enum.valueOf} with
     * uppercased input. Expand: enum constants lowercased.
     */
    private record EnumOptionSpec<M extends Mob, E extends Enum<E>>(
            String key,
            Class<M> mobType,
            Class<E> enumClass,
            BiConsumer<M, E> setter
    ) implements OptionSpec {
        @Override
        @SuppressWarnings("unchecked")
        public String apply(PetType petType, String rawValue, Mob mob) {
            try {
                setter.accept((M) mob, Enum.valueOf(enumClass, rawValue.toUpperCase()));
                return null;
            } catch (IllegalArgumentException e) {
                return invalidValueMessage(key, rawValue, petType);
            } catch (LinkageError ignored) {
                return null;  // pre-version Paper missing the enum class
            }
        }

        @Override
        public List<String> expand(String prefix) {
            try {
                return Arrays.stream(enumClass.getEnumConstants())
                        .map(c -> prefix + c.name().toLowerCase())
                        .toList();
            } catch (LinkageError ignored) {
                return List.of();
            }
        }
    }

    /**
     * Spec backed by a Bukkit/Paper registry. Apply: {@code Registry.get} by
     * lowercased namespaced key. Expand: registry iteration.
     */
    private record RegistryOptionSpec<M extends Mob, T extends Keyed>(
            String key,
            Class<M> mobType,
            RegistryKey<T> registryKey,
            BiConsumer<M, T> setter
    ) implements OptionSpec {
        @Override
        @SuppressWarnings("unchecked")
        public String apply(PetType petType, String rawValue, Mob mob) {
            T value;
            try {
                value = RegistryAccess.registryAccess()
                        .getRegistry(registryKey)
                        .get(Key.key("minecraft", rawValue.toLowerCase()));
            } catch (LinkageError ignored) {
                return null;
            } catch (Throwable t) {
                return invalidValueMessage(key, rawValue, petType);
            }
            if (value == null) return invalidValueMessage(key, rawValue, petType);
            setter.accept((M) mob, value);
            return null;
        }

        @Override
        public List<String> expand(String prefix) {
            try {
                List<String> result = new ArrayList<>();
                for (Keyed k : RegistryAccess.registryAccess().getRegistry(registryKey)) {
                    result.add(prefix + k.getKey().getKey());
                }
                return result;
            } catch (LinkageError ignored) {
                return List.of();
            }
        }
    }

    /**
     * Spec for value-less flag options like {@code "baby"} or {@code "tamed"}.
     * Apply: invokes the setter on the cast mob. Expand: empty list (flags
     * have no value list).
     */
    private record FlagOptionSpec<M extends Mob>(
            String key,
            Class<M> mobType,
            Consumer<M> action
    ) implements OptionSpec {
        @Override
        public boolean isFlag() { return true; }

        @Override
        @SuppressWarnings("unchecked")
        public String apply(PetType petType, String rawValue, Mob mob) {
            action.accept((M) mob);
            return null;
        }

        @Override
        public List<String> expand(String prefix) { return List.of(); }
    }

    /**
     * Spec for key:value options that need custom value parsing (int with
     * clamps, Material lookup, hardcoded literal value list). Apply delegates
     * to the {@code applyFn} which receives the cast mob + raw value string
     * and returns an error message (or {@code null} on success). Expand
     * delegates to the {@code values} supplier — return an empty list for
     * options whose valid values aren't enumerable (e.g. {@code size:N}).
     */
    private record CustomOptionSpec<M extends Mob>(
            String key,
            Class<M> mobType,
            Supplier<List<String>> values,
            BiFunction<M, String, String> applyFn
    ) implements OptionSpec {
        @Override
        @SuppressWarnings("unchecked")
        public String apply(PetType petType, String rawValue, Mob mob) {
            return applyFn.apply((M) mob, rawValue);
        }

        @Override
        public List<String> expand(String prefix) {
            return values.get().stream().map(v -> prefix + v).toList();
        }
    }

    /**
     * The aggregated spec table. Populated as a <b>side effect</b> of each
     * pet class's {@code CREATION_SPECS} static-field initializer calling
     * {@link #specs}. {@link CopyOnWriteArrayList} makes concurrent reads
     * during iteration safe without locks; writes only happen at startup
     * during pet class loading.
     *
     * <p><b>Why side-effect registration instead of reflective pull?</b>
     * Pulling {@code CREATION_SPECS} via reflection during this class's
     * {@code <clinit>} would race with any pet class that's already being
     * initialized on the same thread (via {@code new PetXxx(...)} or
     * similar) — the JVM allows recursive class-init access on the
     * initializing thread, but static fields not yet assigned still read
     * their default ({@code null}), so the spec would be silently dropped.
     * Push-based registration avoids the race: PetXxx's static initializer
     * runs to completion and pushes its specs into SPECS regardless of
     * when this class is first touched.
     */
    private static final List<OptionSpec> SPECS = new CopyOnWriteArrayList<>();

    /**
     * Whether {@link #ensurePetsLoaded} has triggered class initialization
     * for every registered {@link PetType}. {@code volatile} for the
     * double-checked-read fast path.
     */
    private static volatile boolean petsLoaded = false;

    /**
     * Triggers class initialization for every registered pet class so each
     * one's static {@code CREATION_SPECS} initializer pushes its specs into
     * {@link #SPECS} before we iterate it. Idempotent — runs once per JVM.
     *
     * <p>Must not be called from inside any pet class's {@code <clinit>} —
     * the recursive class-init request would return immediately with the
     * pet's spec un-registered. The two callers ({@link #optionsFor},
     * {@link #applyOptions}) are runtime entry points that never run
     * inside a pet's initializer, so this is safe in practice.
     */
    private static synchronized void ensurePetsLoaded() {
        if (petsLoaded) return;
        for (PetType petType : PetType.values()) {
            try {
                Class.forName(petType.getPetClass().getName(), true,
                        petType.getPetClass().getClassLoader());
            } catch (Throwable t) {
                // Pet class missing or its static init failed — skip silently.
                // The pet still works for behaviors that don't depend on SPECS;
                // any option-spec-driven features just won't surface for it.
                MyPetApi.getLogger().warning(
                        "PetCreationOptions: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        // Marker-driven flag specs: for each pet that implements a known
        // marker interface (PetBaby, PetTameable, PetChested, PetSaddleable),
        // synthesize the matching FlagOptionSpec automatically. Lets pets
        // declare these universal flags purely via "implements PetX" rather
        // than per-pet CREATION_SPECS rows.
        for (PetType petType : PetType.values()) {
            registerMarkerSpecs(petType);
        }
        petsLoaded = true;
    }

    /**
     * Generates flag specs for any marker interface the pet implements.
     * Each marker pairs a pet-side interface (e.g. {@link PetBaby}) with
     * a Bukkit-side capability (e.g. {@link Ageable}) and a setter.
     *
     * <p>Adding a new universal flag is two lines: declare a new marker
     * interface in {@code api/entity/}, add one block here pairing the
     * marker with the Bukkit interface + setter. Pets opt in by adding
     * {@code implements PetX} to their class declaration.
     */
    private static void registerMarkerSpecs(PetType petType) {
        Class<? extends Pet> petClass = petType.getPetClass();
        Class<? extends Mob> mobClass = petType.getBukkitEntityClass();
        if (mobClass == null) return;

        if (PetBaby.class.isAssignableFrom(petClass) && Ageable.class.isAssignableFrom(mobClass)) {
            @SuppressWarnings("unchecked")
            Class<Ageable> typedMob = (Class<Ageable>) mobClass.asSubclass(Ageable.class);
            SPECS.add(OptionSpec.ofFlag("baby", typedMob, Ageable::setBaby));
        }
        if (PetTameable.class.isAssignableFrom(petClass) && Tameable.class.isAssignableFrom(mobClass)) {
            @SuppressWarnings("unchecked")
            Class<Tameable> typedMob = (Class<Tameable>) mobClass.asSubclass(Tameable.class);
            SPECS.add(OptionSpec.ofFlag("tamed", typedMob, t -> t.setTamed(true)));
        }
        if (PetChested.class.isAssignableFrom(petClass) && ChestedHorse.class.isAssignableFrom(mobClass)) {
            @SuppressWarnings("unchecked")
            Class<ChestedHorse> typedMob = (Class<ChestedHorse>) mobClass.asSubclass(ChestedHorse.class);
            SPECS.add(OptionSpec.ofFlag("chest", typedMob, h -> h.setCarryingChest(true)));
        }
        if (PetSaddleable.class.isAssignableFrom(petClass) && AbstractHorse.class.isAssignableFrom(mobClass)) {
            @SuppressWarnings("unchecked")
            Class<AbstractHorse> typedMob = (Class<AbstractHorse>) mobClass.asSubclass(AbstractHorse.class);
            SPECS.add(OptionSpec.ofFlag("saddle", typedMob,
                    h -> h.getInventory().setSaddle(new ItemStack(Material.SADDLE))));
        }
    }

    /**
     * Factory for a Pet class's {@code CREATION_SPECS} field. Returned
     * list is purely informational — the side effect of this call is what
     * matters: each successfully-resolved spec is registered into the
     * central {@link #SPECS} table.
     *
     * <p>Each spec is supplied as a {@link Supplier} so the underlying
     * enum-class / registry-key reference is evaluated lazily inside this
     * helper, where a {@link LinkageError} (from a Paper version too old
     * to declare the referenced type) is caught and the row is silently
     * dropped.
     *
     * <p>The {@code Supplier<OptionSpec>} signature is a forcing function:
     * a developer can't accidentally write a direct {@code OptionSpec.of...}
     * call here, which would evaluate eagerly at the pet's class-init time
     * and crash the whole pet class on older runtimes.
     *
     * <p>Typical use:
     * <pre>{@code
     * public static final List<OptionSpec> CREATION_SPECS = PetCreationOptions.specs(
     *     () -> OptionSpec.ofEnum("variant", Axolotl.class, Axolotl.Variant.class, Axolotl::setVariant)
     * );
     * }</pre>
     */
    @SafeVarargs
    public static List<OptionSpec> specs(Supplier<OptionSpec>... factories) {
        List<OptionSpec> result = new ArrayList<>(factories.length);
        for (Supplier<OptionSpec> factory : factories) {
            try {
                OptionSpec spec = factory.get();
                result.add(spec);
                SPECS.add(spec);
            } catch (LinkageError ignored) {
                // Pet's spec references a type/field the runtime Paper version doesn't have.
            }
        }
        return List.copyOf(result);
    }

    // =====================================================================
    // Tab-completion side
    // =====================================================================

    /**
     * Returns the option strings accepted by {@code petType}, derived
     * entirely from {@link #SPECS} rows whose mob class matches the pet's
     * Bukkit class. Flag specs contribute the bare key (e.g. {@code "baby"});
     * value-bearing specs contribute the bare key (e.g. {@code "variant:"})
     * plus all expanded values (e.g. {@code "variant:lucy"}). Duplicates
     * across multiple specs sharing a key are deduped.
     *
     * <p>The result mixes <b>bare keys</b> (e.g. {@code "variant:"}) with
     * <b>value entries</b> (e.g. {@code "variant:lucy"}, {@code "puff:none"}).
     * Callers render these as two-stage tab-completion: bare keys before
     * the admin types the colon; value entries after.
     */
    public static List<String> optionsFor(PetType petType) {
        ensurePetsLoaded();
        Class<? extends Mob> mobClass = petType.getBukkitEntityClass();
        if (mobClass == null) return List.of();
        List<String> opts = new ArrayList<>();
        Set<String> seenFlags = new LinkedHashSet<>();
        Set<String> seenBareKeys = new LinkedHashSet<>();
        for (OptionSpec spec : SPECS) {
            if (!spec.mobType().isAssignableFrom(mobClass)) continue;
            if (spec.isFlag()) {
                if (seenFlags.add(spec.key())) opts.add(spec.key());
            } else {
                String bareKey = spec.key() + ":";
                if (seenBareKeys.add(bareKey)) {
                    opts.add(bareKey);
                    opts.addAll(spec.expand(bareKey));
                }
            }
        }
        return opts;
    }

    /**
     * Builds an "Invalid {@code <key>}" error message for {@code petType},
     * appending the list of valid values derived by finding the matching
     * SPECS row and calling its {@link OptionSpec#expand} method.
     */
    private static String invalidValueMessage(String key, String value, PetType petType) {
        Class<? extends Mob> mobClass = petType.getBukkitEntityClass();
        String prefix = key + ":";
        String validList = "";
        if (mobClass != null) {
            for (OptionSpec spec : SPECS) {
                if (spec.key().equals(key) && spec.mobType().isAssignableFrom(mobClass)) {
                    validList = spec.expand(prefix).stream()
                            .map(s -> s.substring(prefix.length()))
                            .collect(Collectors.joining(", "));
                    break;
                }
            }
        }
        return "Invalid " + key + " '" + value + "' for " + petType.name()
                + (validList.isEmpty() ? "" : ". Valid: " + validList);
    }

    // =====================================================================
    // Apply side
    // =====================================================================

    /**
     * Applies the option strings to a freshly-constructed Bukkit {@link Mob}.
     * Returns a list of per-option validation error messages (empty on
     * success). The caller decides what to do with errors — the admin
     * command aborts and prints them; the petshop logs them and proceeds.
     *
     * @param petType the {@link PetType} being created
     * @param args    the option strings to apply
     * @param mob     the target Bukkit mob — typically detached from any
     *                world via {@code World#createEntity}
     */
    public static List<String> applyOptions(PetType petType, String[] args, Mob mob) {
        ensurePetsLoaded();
        List<String> errors = new ArrayList<>();
        for (String arg : args) {
            try {
                String error = applyOption(petType, arg, mob);
                if (error != null) errors.add(error);
            } catch (Throwable t) {
                String err = "option '" + arg + "' for " + petType.name() + " threw "
                        + t.getClass().getSimpleName() + ": " + t.getMessage();
                errors.add(err);
                MyPetApi.getLogger().warning("PetCreationOptions.applyOptions: " + err);
            }
        }
        return errors;
    }

    /**
     * Applies a single option, returning {@code null} on success or a
     * human-readable error message if the value isn't recognized. Options
     * that don't match {@code mob}'s actual Bukkit type are silently
     * skipped — same-key options dispatch across multiple mob types and
     * "wrong type" isn't an admin error.
     *
     * <p>Both {@code key:value} options and value-less flags flow through
     * the SPECS table.
     */
    private static String applyOption(PetType petType, String arg, Mob mob) {
        int colon = arg.indexOf(':');
        boolean wantFlag = colon < 0;
        String key = wantFlag ? arg.toLowerCase() : arg.substring(0, colon).toLowerCase();
        String value = wantFlag ? "" : arg.substring(colon + 1);
        for (OptionSpec spec : SPECS) {
            if (spec.isFlag() != wantFlag) continue;
            if (!spec.key().equalsIgnoreCase(key)) continue;
            if (!spec.mobType().isInstance(mob)) continue;
            return spec.apply(petType, value, mob);
        }
        return null;
    }

    // =====================================================================
    // Spec factory helpers for the three options that need custom value
    // parsing (size:, block:, puff:). These keep the per-pet CREATION_SPECS
    // rows terse instead of inlining the validation logic.
    // =====================================================================

    /**
     * Creates a {@code size:N} spec for size-capable mobs (Slime / MagmaCube
     * / Phantom). Parses {@code N} as an integer, clamps to {@code [1, max]},
     * and dispatches to the setter. Returns an error message if the value
     * isn't an integer — matches the error-surfacing contract of the other
     * spec types.
     */
    public static <M extends Mob> Supplier<OptionSpec> sizeSpec(
            Class<M> mobClass, int max, BiConsumer<M, Integer> setter) {
        return () -> OptionSpec.ofCustom("size", mobClass,
                () -> List.of(),
                (mob, v) -> {
                    if (!Util.isInt(v)) {
                        return "Invalid size '" + v + "' for " + mobClass.getSimpleName()
                                + " — expected an integer in [1, " + max + "]";
                    }
                    setter.accept(mob, Math.max(1, Math.min(max, Integer.parseInt(v))));
                    return null;
                });
    }

    /**
     * Creates a {@code block:<material>} spec for Enderman. Uses
     * {@link Material#matchMaterial} for tolerant parsing (admin can type
     * {@code stone} or {@code minecraft:stone}).
     */
    public static Supplier<OptionSpec> blockSpec(Class<Enderman> mobClass) {
        return () -> OptionSpec.ofCustom("block", mobClass,
                () -> List.of(),  // ~700 block materials — too many to surface as suggestions
                (e, v) -> {
                    Material m = Material.matchMaterial(v);
                    if (m == null || !m.isBlock()) {
                        return "Invalid block '" + v + "' for Enderman";
                    }
                    try {
                        e.setCarriedBlock(m.createBlockData());
                        return null;
                    } catch (Throwable t) {
                        return "Invalid block '" + v + "' for Enderman";
                    }
                });
    }

    /**
     * Creates a {@code puff:<state>} spec for PufferFish. Maps the three
     * literal values {@code none|semi|fully} to PuffState indices 0/1/2.
     */
    public static Supplier<OptionSpec> puffSpec(Class<PufferFish> mobClass) {
        return () -> OptionSpec.ofCustom("puff", mobClass,
                () -> List.of("none", "semi", "fully"),
                (p, v) -> {
                    int state = switch (v.toLowerCase()) {
                        case "none" -> 0;
                        case "semi" -> 1;
                        case "fully" -> 2;
                        default -> -1;
                    };
                    if (state < 0) return "Invalid puff '" + v + "' for PufferFish. Valid: none, semi, fully";
                    p.setPuffState(state);
                    return null;
                });
    }
}
