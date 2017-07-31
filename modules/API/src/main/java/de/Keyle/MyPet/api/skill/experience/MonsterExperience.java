/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class MonsterExperience {
    public static final Map<String, MonsterExperience> mobExp = new HashMap<>();
    private static MonsterExperience unknown = new MonsterExperience(0., "UNKNOWN");

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
    }
    
    private double min;
    private double max;
    private String entityType;

    public MonsterExperience(double min, double max, String entityType) {
        if (max >= min) {
            this.max = max;
            this.min = min;
        } else if (max <= min) {
            this.max = min;
            this.min = max;
        }
        this.entityType = entityType;
    }

    public MonsterExperience(double exp, String entityType) {
        this.max = exp;
        this.min = exp;
        this.entityType = entityType;
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

    public EntityType getEntityType() {
        return EntityType.valueOf(entityType);
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

    private static double doubleRandom(double low, double high) {
        return Math.random() * (high - low) + low;
    }

    @Override
    public String toString() {
        return entityType + "{min=" + min + ", max=" + max + "}";
    }

    public static MonsterExperience getMonsterExperience(EntityType type) {
        if (mobExp.containsKey(type.name())) {
            return mobExp.get(type.name());
        }
        return unknown;
    }
}