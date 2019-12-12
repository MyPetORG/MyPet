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

package de.Keyle.MyPet.api.skill.experience;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.types.MonsterExperienceHook;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MonsterExperience {

    public static final Map<String, MonsterExperience> mobExp = new HashMap<>();
    public static final Map<String, MonsterExperience> CUSTOM_MOB_EXP = new HashMap<>();
    public static MonsterExperience UNKNOWN = new MonsterExperience(0., "UNKNOWN");
    public static Pattern PLUGIN_CONFIG_PATTERN = Pattern.compile("\\[\\w+]=.+");

    static {
        mobExp.put("SKELETON", new MonsterExperience(5., "SKELETON"));
        mobExp.put("ZOMBIE", new MonsterExperience(5., "ZOMBIE"));
        mobExp.put("SPIDER", new MonsterExperience(5., "SPIDER"));
        mobExp.put("WOLF", new MonsterExperience(1., 3., "WOLF"));
        mobExp.put("CREEPER", new MonsterExperience(5., "CREEPER"));
        mobExp.put("GHAST", new MonsterExperience(5., "GHAST"));
        mobExp.put("PIG_ZOMBIE", new MonsterExperience(5., "PIG_ZOMBIE"));
        mobExp.put("ENDERMAN", new MonsterExperience(5., "ENDERMAN"));
        mobExp.put("ENDERMITE", new MonsterExperience(3., "ENDERMITE"));
        mobExp.put("CAVE_SPIDER", new MonsterExperience(5., "CAVE_SPIDER"));
        mobExp.put("MAGMA_CUBE", new MonsterExperience(1., 4., "MAGMA_CUBE"));
        mobExp.put("SLIME", new MonsterExperience(1., 4., "SLIME"));
        mobExp.put("SILVERFISH", new MonsterExperience(5., "SILVERFISH"));
        mobExp.put("BLAZE", new MonsterExperience(10., "BLAZE"));
        mobExp.put("GIANT", new MonsterExperience(25., "GIANT"));
        mobExp.put("GUARDIAN", new MonsterExperience(10., "GUARDIAN"));
        mobExp.put("COW", new MonsterExperience(1., 3., "COW"));
        mobExp.put("PIG", new MonsterExperience(1., 3., "PIG"));
        mobExp.put("CHICKEN", new MonsterExperience(1., 3., "CHICKEN"));
        mobExp.put("SQUID", new MonsterExperience(1., 3., "SQUID"));
        mobExp.put("SHEEP", new MonsterExperience(1., 3., "SHEEP"));
        mobExp.put("OCELOT", new MonsterExperience(1., 3., "OCELOT"));
        mobExp.put("MUSHROOM_COW", new MonsterExperience(1., 3., "MUSHROOM_COW"));
        mobExp.put("VILLAGER", new MonsterExperience(0., "VILLAGER"));
        mobExp.put("SHULKER", new MonsterExperience(5., "SHULKER"));
        mobExp.put("SNOWMAN", new MonsterExperience(0., "SNOWMAN"));
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
    }

    private double min;
    private double max;
    private String identifier;

    public MonsterExperience(double min, double max, String identifier) {
        if (max >= min) {
            this.max = max;
            this.min = min;
        } else if (max <= min) {
            this.max = min;
            this.min = max;
        }
        this.identifier = identifier;
    }

    public MonsterExperience(double exp, String identifier) {
        this.max = exp;
        this.min = exp;
        this.identifier = identifier;
    }

    public double getRandomExp() {
        return max == min ? max : ((int) (doubleRandom(min, max) * 100)) / 100.;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setMin(double min) {
        this.min = min;
        if (min > max) {
            max = min;
        }
    }

    public void setMax(double max) {
        this.max = max;
        if (max < min) {
            min = max;
        }
    }

    public void setExp(double exp) {
        max = (min = exp);
    }

    public static void addCustomExperience(MonsterExperience experience) {
        CUSTOM_MOB_EXP.put(experience.identifier, experience);
    }

    private static double doubleRandom(double low, double high) {
        return Math.random() * (high - low) + low;
    }

    @Override
    public String toString() {
        return identifier + "{min=" + min + ", max=" + max + "}";
    }

    @SuppressWarnings("RedundantCast")
    public static MonsterExperience getMonsterExperience(Entity entity) {
        String name = null;
        if (MyPetApi.getCompatUtil().isCompatible("1.8")) {
            name = entity.getCustomName();
        } else if (entity instanceof LivingEntity) {
            // casting for 1.7.10
            name = ((LivingEntity) entity).getCustomName();
        }

        List<MonsterExperienceHook> hooks = MyPetApi.getPluginHookManager().getHooks(MonsterExperienceHook.class);
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

    public static MonsterExperience getMonsterExperience(EntityType type) {
        if (mobExp.containsKey(type.name())) {
            return mobExp.get(type.name());
        }
        return UNKNOWN;
    }

    public static MonsterExperience getMonsterExperience(String identifier) {
        if (CUSTOM_MOB_EXP.containsKey(identifier)) {
            return CUSTOM_MOB_EXP.get(identifier);
        }
        if (mobExp.containsKey(identifier)) {
            return mobExp.get(identifier);
        }
        return UNKNOWN;
    }
}