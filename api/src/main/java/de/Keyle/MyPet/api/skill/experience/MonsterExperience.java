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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.types.MonsterExperienceHook;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Represents the experience value (or range) awarded to a pet when a specific mob type is killed.
 *
 * <p>Each instance defines a minimum and maximum experience amount for a single mob identifier.
 * When a pet earns experience from a kill, {@link #getRandomExp()} returns a uniformly distributed
 * random value within that range (or the exact value if min equals max).
 *
 * <p>The class maintains two static registries:
 * <ul>
 *   <li>{@link #mobExp} -- default experience values for vanilla entity types, populated in a
 *       static initializer block.</li>
 *   <li>{@link #CUSTOM_MOB_EXP} -- experience values for custom-named mobs, populated at
 *       runtime via {@link #addCustomExperience(MonsterExperience)}.</li>
 * </ul>
 *
 * <p>Resolution order when looking up an entity's experience reward (via
 * {@link #getMonsterExperience(Entity)}):
 * <ol>
 *   <li>{@link de.Keyle.MyPet.api.util.hooks.types.MonsterExperienceHook} plugins (e.g. MythicMobs)</li>
 *   <li>Custom mob name entries in {@link #CUSTOM_MOB_EXP}</li>
 *   <li>Vanilla type entries in {@link #mobExp}</li>
 *   <li>{@link #UNKNOWN} (0 experience) as a final fallback</li>
 * </ol>
 */
@Getter
public class MonsterExperience {

    /** Default experience values for vanilla entity types, keyed by {@code EntityType.name()}. */
    public static final Map<String, MonsterExperience> mobExp = new HashMap<>();
    /** Custom experience entries for named mobs, keyed by the mob's custom display name. */
    public static final Map<String, MonsterExperience> CUSTOM_MOB_EXP = new HashMap<>();
    /** Fallback experience entry used when no mapping exists for a given entity. Always 0 XP. */
    public static final MonsterExperience UNKNOWN = new MonsterExperience(0., "UNKNOWN");
    /** Pattern matching plugin-config-style custom names (e.g. {@code [MythicMobs]=BossName}). */
    public static final Pattern PLUGIN_CONFIG_PATTERN = Pattern.compile("\\[\\w+]=.+");

    static {
        mobExp.put("SKELETON", new MonsterExperience(5., "SKELETON"));
        mobExp.put("ZOMBIE", new MonsterExperience(5., "ZOMBIE"));
        mobExp.put("SPIDER", new MonsterExperience(5., "SPIDER"));
        mobExp.put("WOLF", new MonsterExperience(1., 3., "WOLF"));
        mobExp.put("CREEPER", new MonsterExperience(5., "CREEPER"));
        mobExp.put("GHAST", new MonsterExperience(5., "GHAST"));
        mobExp.put("ENDERMAN", new MonsterExperience(5., "ENDERMAN"));
        mobExp.put("ENDERMITE", new MonsterExperience(3., "ENDERMITE"));
        mobExp.put("CAVE_SPIDER", new MonsterExperience(5., "CAVE_SPIDER"));
        mobExp.put("MAGMA_CUBE", new MonsterExperience(1., 4., "MAGMA_CUBE"));
        mobExp.put("SLIME", new MonsterExperience(1., 4., "SLIME"));
        mobExp.put("SILVERFISH", new MonsterExperience(5., "SILVERFISH"));
        mobExp.put("BLAZE", new MonsterExperience(10., "BLAZE"));
        mobExp.put("GIANT", new MonsterExperience(25., "GIANT"));
        mobExp.put("GUARDIAN", new MonsterExperience(10., "GUARDIAN"));
        mobExp.put("ELDER_GUARDIAN", new MonsterExperience(10., "ELDER_GUARDIAN"));
        mobExp.put("HORSE", new MonsterExperience(1, 3, "HORSE"));
        mobExp.put("COW", new MonsterExperience(1., 3., "COW"));
        mobExp.put("CAMEL", new MonsterExperience(1., 3., "CAMEL"));
        mobExp.put("SNIFFER", new MonsterExperience(1., 3., "SNIFFER"));
        mobExp.put("PIG", new MonsterExperience(1., 3., "PIG"));
        mobExp.put("CHICKEN", new MonsterExperience(1., 3., "CHICKEN"));
        mobExp.put("SQUID", new MonsterExperience(1., 3., "SQUID"));
        mobExp.put("GLOW_SQUID", new MonsterExperience(1., 3., "GLOW_SQUID"));
        mobExp.put("SHEEP", new MonsterExperience(1., 3., "SHEEP"));
        mobExp.put("GOAT", new MonsterExperience(1., 3., "GOAT"));
        mobExp.put("OCELOT", new MonsterExperience(1., 3., "OCELOT"));
        mobExp.put("MOOSHROOM", new MonsterExperience(1., 3., "MOOSHROOM"));
        mobExp.put("VILLAGER", new MonsterExperience(0., "VILLAGER"));
        mobExp.put("SHULKER", new MonsterExperience(5., "SHULKER"));
        mobExp.put("SNOW_GOLEM", new MonsterExperience(0., "SNOW_GOLEM"));
        mobExp.put("IRON_GOLEM", new MonsterExperience(0., "IRON_GOLEM"));
        mobExp.put("ENDER_DRAGON", new MonsterExperience(20000., "ENDER_DRAGON"));
        mobExp.put("WITCH", new MonsterExperience(10., "WITCH"));
        mobExp.put("BAT", new MonsterExperience(1., "BAT"));
        mobExp.put("ENDER_CRYSTAL", new MonsterExperience(10., "ENDER_CRYSTAL"));
        mobExp.put("WITHER", new MonsterExperience(100., "WITHER"));
        mobExp.put("RABBIT", new MonsterExperience(1., "RABBIT"));
        mobExp.put("VINDICATOR", new MonsterExperience(5., "VINDICATOR"));
        mobExp.put("EVOKER", new MonsterExperience(10., "EVOKER"));
        mobExp.put("VEX", new MonsterExperience(3., "VEX"));
        mobExp.put("LLAMA", new MonsterExperience(0., "LLAMA"));
        mobExp.put("WITHER_SKELETON", new MonsterExperience(5., "WITHER_SKELETON"));
        mobExp.put("SKELETON_HORSE", new MonsterExperience(1, 3, "SKELETON_HORSE"));
        mobExp.put("ZOMBIE_HORSE", new MonsterExperience(1, 3, "ZOMBIE_HORSE"));
        mobExp.put("DONKEY", new MonsterExperience(1, 3, "DONKEY"));
        mobExp.put("MULE", new MonsterExperience(1, 3, "MULE"));
        mobExp.put("ILLUSIONER", new MonsterExperience(5, "ILLUSIONER"));
        mobExp.put("ZOMBIE_VILLAGER", new MonsterExperience(5, "ZOMBIE_VILLAGER"));
        mobExp.put("POLAR_BEAR", new MonsterExperience(1, 3, "POLAR_BEAR"));
        mobExp.put("PARROT", new MonsterExperience(1, 3, "PARROT"));
        mobExp.put("HUSK", new MonsterExperience(5, "HUSK"));
        mobExp.put("STRAY", new MonsterExperience(5, "STRAY"));
        mobExp.put("DOLPHIN", new MonsterExperience(0, "DOLPHIN"));
        mobExp.put("DROWNED", new MonsterExperience(5, "DROWNED"));
        mobExp.put("PHANTOM", new MonsterExperience(5, "PHANTOM"));
        mobExp.put("TURTLE", new MonsterExperience(1, 3, "TURTLE"));
        mobExp.put("COD", new MonsterExperience(0, "COD"));
        mobExp.put("SALMON", new MonsterExperience(0, "SALMON"));
        mobExp.put("PUFFERFISH", new MonsterExperience(0, "PUFFERFISH"));
        mobExp.put("TROPICAL_FISH", new MonsterExperience(0, "TROPICAL_FISH"));
        mobExp.put("CAT", new MonsterExperience(1, 3, "CAT"));
        mobExp.put("FOX", new MonsterExperience(1, 2, "FOX"));
        mobExp.put("PANDA", new MonsterExperience(1, 3, "PANDA"));
        mobExp.put("PILLAGER", new MonsterExperience(5, "PILLAGER"));
        mobExp.put("RAVAGER", new MonsterExperience(4, 5, "RAVAGER"));
        mobExp.put("TRADER_LLAMA", new MonsterExperience(1, 3, "TRADER_LLAMA"));
        mobExp.put("WANDERING_TRADER", new MonsterExperience(1, 2, "WANDERING_TRADER"));
        mobExp.put("BEE", new MonsterExperience(1, 3, "BEE"));
        mobExp.put("AXOLOTL", new MonsterExperience(1, 3, "AXOLOTL"));
        mobExp.put("ALLAY", new MonsterExperience(1, 3, "ALLAY"));
        mobExp.put("FROG", new MonsterExperience(1, 3, "FROG"));
        mobExp.put("TADPOLE", new MonsterExperience(1, 3, "TADPOLE"));
        mobExp.put("ZOMBIFIED_PIGLIN", new MonsterExperience(5, "ZOMBIFIED_PIGLIN"));
        mobExp.put("HOGLIN", new MonsterExperience(5, "HOGLIN"));
        mobExp.put("WARDEN", new MonsterExperience(5, "WARDEN"));
        mobExp.put("ZOGLIN", new MonsterExperience(5, "ZOGLIN"));
        mobExp.put("STRIDER", new MonsterExperience(1, 2, "STRIDER"));
        mobExp.put("PIGLIN", new MonsterExperience(1, 3, "PIGLIN"));
        mobExp.put("PIGLIN_BRUTE", new MonsterExperience(20, 23, "PIGLIN_BRUTE")); // 1.16.2
        mobExp.put("BOGGED", new MonsterExperience(6, 8, "BOGGED"));
        mobExp.put("BREEZE", new MonsterExperience(10, "BREEZE"));
        mobExp.put("ARMADILLO", new MonsterExperience(1, 3, "ARMADILLO"));
    }

    private double min;
    private double max;
    private final String identifier;

    /**
     * Creates a monster experience entry with a random range.
     *
     * <p>If {@code max < min}, the values are swapped internally so that
     * {@link #getRandomExp()} always produces a value in the correct range.
     *
     * @param min        the minimum experience awarded
     * @param max        the maximum experience awarded
     * @param identifier the mob identifier (typically the {@code EntityType} name or custom name)
     */
    public MonsterExperience(double min, double max, String identifier) {
        if (max >= min) {
            this.max = max;
            this.min = min;
        } else {
            this.max = min;
            this.min = max;
        }
        this.identifier = identifier;
    }

    /**
     * Creates a monster experience entry with a fixed (non-random) value.
     *
     * @param exp        the exact experience awarded on kill
     * @param identifier the mob identifier
     */
    public MonsterExperience(double exp, String identifier) {
        this.max = exp;
        this.min = exp;
        this.identifier = identifier;
    }

    /**
     * Registers a custom mob experience entry, keyed by the entry's identifier.
     *
     * @param experience the experience entry to register
     */
    public static void addCustomExperience(MonsterExperience experience) {
        CUSTOM_MOB_EXP.put(experience.identifier, experience);
    }

    private static double doubleRandom(double low, double high) {
        return ThreadLocalRandom.current().nextDouble() * (high - low) + low;
    }

    /**
     * Resolves the experience entry for a specific entity instance.
     *
     * <p>Checks plugin hooks first, then custom-name entries, then vanilla type entries,
     * falling back to {@link #UNKNOWN} if no match is found.
     *
     * @param entity the killed entity
     * @return the matching experience entry (never {@code null})
     */
    public static MonsterExperience getMonsterExperience(Entity entity) {
        Component customName = entity.customName();
        String name = customName != null
                ? PlainTextComponentSerializer.plainText().serialize(customName)
                : null;

        List<MonsterExperienceHook> hooks = MyPetApi.getServiceManager().getServices(MonsterExperienceHook.class);
        for (MonsterExperienceHook hook : hooks) {
            MonsterExperience monsterExperience = hook.getMonsterExperience(entity);
            if (monsterExperience != null) {
                return monsterExperience;
            }
        }

        if (name != null) {
            if (!PLUGIN_CONFIG_PATTERN.matcher(name).matches() && CUSTOM_MOB_EXP.containsKey(name)) {
                return CUSTOM_MOB_EXP.get(name);
            }
        }

        if (mobExp.containsKey(entity.getType().name())) {
            return mobExp.get(entity.getType().name());
        }
        return UNKNOWN;
    }

    /**
     * Resolves the experience entry for a Bukkit entity type (vanilla mobs only).
     *
     * @param type the entity type
     * @return the matching experience entry, or {@link #UNKNOWN} if none is registered
     */
    public static MonsterExperience getMonsterExperience(EntityType type) {
        if (mobExp.containsKey(type.name())) {
            return mobExp.get(type.name());
        }
        return UNKNOWN;
    }

    /**
     * Resolves the experience entry by raw string identifier, checking custom entries first.
     *
     * @param identifier the mob identifier (entity type name or custom name)
     * @return the matching experience entry, or {@link #UNKNOWN} if none is registered
     */
    public static MonsterExperience getMonsterExperience(String identifier) {
        if (CUSTOM_MOB_EXP.containsKey(identifier)) {
            return CUSTOM_MOB_EXP.get(identifier);
        }
        if (mobExp.containsKey(identifier)) {
            return mobExp.get(identifier);
        }
        return UNKNOWN;
    }

    /**
     * Returns a random experience value uniformly distributed between {@link #min} and {@link #max}.
     *
     * <p>If min equals max, returns that exact value. The result is truncated to two decimal places.
     */
    public double getRandomExp() {
        return max == min ? max : ((int) (doubleRandom(min, max) * 100)) / 100.;
    }

    /**
     * Sets the minimum experience. If the new minimum exceeds the current maximum,
     * the maximum is raised to match.
     */
    public void setMin(double min) {
        this.min = min;
        if (min > max) {
            max = min;
        }
    }

    /**
     * Sets the maximum experience. If the new maximum is below the current minimum,
     * the minimum is lowered to match.
     */
    public void setMax(double max) {
        this.max = max;
        if (max < min) {
            min = max;
        }
    }

    /** Sets both min and max to the same value, making this a fixed-experience entry. */
    public void setExp(double exp) {
        max = (min = exp);
    }

}
