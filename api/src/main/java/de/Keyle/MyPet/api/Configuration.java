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

package de.Keyle.MyPet.api;

import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.util.ConfigItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Configuration {

    public static class Misc {

        public static boolean CONSUME_LEASH_ITEM = false;
        public static boolean ALLOW_RANGED_LEASHING = true;
        public static boolean OWNER_CAN_ATTACK_PET = false;
        public static boolean DISABLE_PET_VS_PLAYER = false;
        public static boolean PET_KILLS_GIVE_PLAYER_REWARDS = true;
        public static boolean RETAIN_EQUIPMENT_ON_TAME = true;
        public static boolean INVISIBLE_LIKE_OWNER = true;
        public static boolean THROW_PLAYER_MOVE_EVENT_WHILE_RIDING = true;
        public static boolean DISABLE_ALL_ACTIONBAR_MESSAGES = false;
        public static boolean RECALL_PET_AFTER_DESPAWN = true;
        public static String OVERWRITE_LANGUAGE = "";
        public static String WIKI_URL = "https://wiki.mypet-plugin.de";
        public static String RIGHT_CLICK_COMMAND = "";
        public static int MAX_STORED_PET_COUNT = 45;
    }

    public static class Entity {

        public static double MYPET_FOLLOW_START_DISTANCE = 7.0F;
    }

    public static class Log {

        public static String LEVEL = "INFO";
    }

    public static class Update {

        public static boolean ASYNC = false;
        public static boolean CHECK = true;
        public static boolean DOWNLOAD = false;
        public static boolean REPLACE_OLD = false;
        public static boolean SHOW_OP = true;
    }

    public static class Repository {

        public static long EXTERNAL_LOAD_DELAY = 20L;

        public static String REPOSITORY_TYPE = "SQLite";
        public static String CONVERT_FROM = "";

        public static class MySQL {

            public static String DATABASE = "mypet";
            public static String PREFIX = "";
            public static String HOST = "localhost";
            public static String PASSWORD = "";
            public static String USER = "root";
            public static String CHARACTER_ENCODING = "utf8";
            public static int PORT = 3306;
            public static int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
        }

    }

    public static class Respawn {

        public static boolean DISABLE_AUTO_RESPAWN = false;
        public static int TIME_FACTOR = 5;
        public static int TIME_PLAYER_FACTOR = 5;
        public static int TIME_FIXED = 0;
        public static int TIME_PLAYER_FIXED = 0;
        public static double COSTS_FACTOR = 1.0;
        public static double COSTS_FIXED = 0.0;
    }

    public static class Name {

        public static int MAX_LENGTH = 32;

        public static class Tag {

            public static boolean SHOW = true;
            public static String PREFIX = "<aqua>";
            public static String SUFFIX = "";
        }
    }

    public static class Permissions {

        public static boolean EXTENDED = false;
    }

    public static class LevelSystem {

        public static String CALCULATION_MODE = "Default";

        public static class Experience {

            public static int LOSS_PERCENT = 0;
            public static double LOSS_FIXED = 0;
            public static boolean ALLOW_LEVEL_DOWNGRADE = false;
            public static boolean DROP_LOST_EXP = true;
            public static boolean DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = true;
            public static boolean ALWAYS_GRANT_PASSIVE_XP = true;
            public static int PASSIVE_PERCENT_PER_MONSTER = 25;
            public static int LEVEL_CAP = 100;
            public static Set<String> PREVENT_FROM_SPAWN_REASON = new HashSet<>();
            public static Set<String> DISABLED_WORLDS = new HashSet<>();

            public static class Modifier {

                public static double GLOBAL = 1;
                public static boolean PERMISSION = false;
            }
        }
    }

    public static class HungerSystem {

        public static boolean USE_HUNGER_SYSTEM = true;
        public static int HUNGER_SYSTEM_TIME = 60;
        public static double HUNGER_SYSTEM_SATURATION_PER_FEED = 6.0;
        public static boolean AFFECT_RIDE_SPEED = true;
        public static boolean AFFECT_BEACON_RANGE = true;
        public static double HUNGER_SYSTEM_FIXED = 1.;
        public static double HUNGER_SYSTEM_FACTOR = 0.;
        public static double HUNGER_SYSTEM_TIME_BEFORE_DAMAGE = 5.;
        public static boolean HUNGER_SYSTEM_CAN_KILL = false;
        public static boolean FEED_FROM_INVENTORY = true;
    }

    public static class Skilltree {

        public static int SWITCH_FEE_PERCENT = 5;
        public static double SWITCH_FEE_FIXED = 0.0;
        public static boolean SWITCH_FEE_ADMIN = false;
        public static boolean AUTOMATIC_SKILLTREE_ASSIGNMENT = false;
        public static boolean RANDOM_SKILLTREE_ASSIGNMENT = false;
        public static boolean CHOOSE_SKILLTREE_ONLY_ONCE = false;
        public static boolean PREVENT_LEVELLING_WITHOUT_SKILLTREE = true;

        public static class Skill {

            public static ConfigItem CONTROL_ITEM;

            public static class Ride {

                public static ConfigItem RIDE_ITEM;
                public static double HUNGER_PER_METER = 0.01;
                public static boolean PREVENT_TELEPORTATION = false;
            }

            public static class Beacon {

                public static int HUNGER_DECREASE_TIME = 100;
                public static boolean PARTY_SUPPORT = true;
                public static boolean DISABLE_HEAD_TEXTURE = false;
                public static boolean ZONE_MESSAGES = true;
            }

            public static class Backpack {

                public static boolean OPEN_IN_CREATIVE = false;
                public static boolean DROP_WHEN_OWNER_DIES = false;
            }
        }
    }

    public static class MyPet {

        // Dynamic per-type maps populated by ConfigurationLoader after pet types
        // register, all keyed by PetType.name(). Adding a new pet only requires
        // implementing the appropriate marker:
        //   MyPetFlyingEntity     → CanFly + CanGlide rows + CAN_FLY/CAN_GLIDE entries
        //   MyPetGlidingEntity    → CanGlide row + CAN_GLIDE entry
        //   MyPetSwimmingEntity   → CanSwim row + CAN_SWIM entry (covers both
        //                            MyPetAquaticEntity + MyPetAmphibiousEntity)
        //   MyPetAquaticEntity    → adds PreventSuffocation row + PREVENT_SUFFOCATION entry
        //   MyPetZombifiable      → AllowZombification row + ALLOW_ZOMBIFICATION entry
        //   MyPetSunSensitive     → PreventDaylightBurn row + PREVENT_DAYLIGHT_BURN entry
        //   MyPetBaby             → GrowUpItem row + GROW_UP_ITEMS entry (default
        //                            comes from @DefaultInfo#growUpItem())
        private static final Map<String, Boolean> CAN_FLY = new HashMap<>();
        private static final Map<String, Boolean> CAN_GLIDE = new HashMap<>();
        private static final Map<String, Boolean> CAN_SWIM = new HashMap<>();
        private static final Map<String, Boolean> ALLOW_ZOMBIFICATION = new HashMap<>();
        private static final Map<String, Boolean> PREVENT_DAYLIGHT_BURN = new HashMap<>();
        private static final Map<String, Boolean> PREVENT_SUFFOCATION = new HashMap<>();
        private static final Map<String, ConfigItem> GROW_UP_ITEMS = new HashMap<>();

        public static boolean canFly(PetType type) {
            return CAN_FLY.getOrDefault(type.name(), true);
        }

        public static void setCanFly(String typeName, boolean value) {
            CAN_FLY.put(typeName, value);
        }

        public static boolean canGlide(PetType type) {
            return CAN_GLIDE.getOrDefault(type.name(), true);
        }

        public static void setCanGlide(String typeName, boolean value) {
            CAN_GLIDE.put(typeName, value);
        }

        public static boolean canSwim(PetType type) {
            return CAN_SWIM.getOrDefault(type.name(), true);
        }

        public static void setCanSwim(String typeName, boolean value) {
            CAN_SWIM.put(typeName, value);
        }

        public static boolean allowZombification(PetType type) {
            return ALLOW_ZOMBIFICATION.getOrDefault(type.name(), false);
        }

        public static void setAllowZombification(String typeName, boolean value) {
            ALLOW_ZOMBIFICATION.put(typeName, value);
        }

        public static boolean preventDaylightBurn(PetType type) {
            return PREVENT_DAYLIGHT_BURN.getOrDefault(type.name(), true);
        }

        public static void setPreventDaylightBurn(String typeName, boolean value) {
            PREVENT_DAYLIGHT_BURN.put(typeName, value);
        }

        public static boolean preventSuffocation(PetType type) {
            return PREVENT_SUFFOCATION.getOrDefault(type.name(), true);
        }

        public static void setPreventSuffocation(String typeName, boolean value) {
            PREVENT_SUFFOCATION.put(typeName, value);
        }

        public static ConfigItem getGrowUpItem(PetType type) {
            return GROW_UP_ITEMS.get(type.name());
        }

        public static void setGrowUpItem(String typeName, ConfigItem item) {
            GROW_UP_ITEMS.put(typeName, item);
        }

        public static class Armadillo {

            public static boolean CAN_SHED_SCUTE = true;
        }

        public static class Chicken {

            public static boolean CAN_LAY_EGGS = true;
        }

        public static class CopperGolem {

            public static boolean CAN_OXIDIZE = true;
        }

        public static class Cow {

            public static boolean CAN_GIVE_MILK = true;
        }

        public static class Creeper {

            /**
             * If {@code true}, players can ignite a Creeper pet with flint-and-steel
             * (which triggers vanilla's fuse). If {@code false}, the
             * {@code PlayerInteractEntityEvent} is cancelled before vanilla's
             * {@code Creeper#mobInteract} can run. Default {@code false}.
             */
            public static boolean ALLOW_FLINT_AND_STEEL_EXPLODE = false;

            /**
             * Independent of {@link #ALLOW_FLINT_AND_STEEL_EXPLODE} — when ignition
             * is allowed, this controls whether non-owner players can perform it.
             * Default {@code false}: only the owner can ignite. Has no effect when
             * {@code ALLOW_FLINT_AND_STEEL_EXPLODE = false}.
             */
            public static boolean ALLOW_NON_OWNER_FLINT_AND_STEEL = false;

            /**
             * Independent of the ignition flags — when an explosion does happen,
             * this controls whether the blast damages blocks (terrain destruction
             * and item drops). Default {@code false}: explosion plays visually,
             * the pet routes through normal death, but the terrain is unharmed.
             */
            public static boolean ALLOW_EXPLOSION_BLOCK_DAMAGE = false;

            /**
             * Independent of {@link #ALLOW_EXPLOSION_BLOCK_DAMAGE} — when an
             * explosion does happen, this controls whether the blast damages
             * nearby entities (players, mobs, item entities). Default {@code false}.
             * Modern Paper handles per-victim explosion damage on a separate code
             * path from block damage, so this flag is enforced via a dedicated
             * {@code EntityDamageByEntityEvent} handler rather than the
             * {@code EntityExplodeEvent} cancellation.
             */
            public static boolean ALLOW_EXPLOSION_ENTITY_DAMAGE = false;
        }

        public static class EnderDragon {

            /**
             * If {@code true}, killing an EnderDragon pet awards the player the
             * {@code minecraft:end/kill_dragon} ("Free the End") advancement,
             * matching vanilla. If {@code false}, a Paper
             * {@code PlayerAdvancementCriterionGrantEvent} listener cancels
             * the criterion grant when the kill came from a marked pet
             * EnderDragon. Default {@code false}: pet kills don't count as
             * boss kills.
             */
            public static boolean GRANT_END_ADVANCEMENT_ON_KILL = false;

            /**
             * If {@code true}, the pet EnderDragon may destroy non-immune blocks
             * its body brushes against, matching vanilla. If {@code false},
             * {@code PetEnderDragonBlockDamageListener} cancels the
             * {@code EntityExplodeEvent} that Paper's patched
             * {@code EnderDragon#checkWalls} fires for brush destruction, so
             * the dragon never removes any block. Default {@code false}.
             */
            public static boolean ALLOW_BLOCK_DAMAGE = false;

            /**
             * If {@code true}, the pet EnderDragon may damage non-owner players
             * via direct body contact (vanilla {@code attackTargets} damage).
             * The owner is universally protected (handled by other listeners).
             * Default {@code false}.
             */
            public static boolean ALLOW_PLAYER_CONTACT_DAMAGE = false;

            /**
             * If {@code true}, the pet EnderDragon may damage non-player entities
             * (mobs, item entities) via direct body contact. Default {@code false}.
             *
             * <p>Independent of {@link #ALLOW_PLAYER_CONTACT_DAMAGE} — modern
             * Paper fires {@link org.bukkit.event.entity.EntityDamageByEntityEvent}
             * once per victim, so a single listener can split the decision by
             * inspecting {@code event.getEntity()}.
             */
            public static boolean ALLOW_ENTITY_CONTACT_DAMAGE = false;
        }

        public static class Goat {

            public static boolean CAN_GIVE_MILK = true;
            public static boolean CAN_DROP_HORN = true;
        }

        public static class IronGolem {

            public static boolean CAN_TOSS_UP = true;
        }

        public static class MagmaCube {

            /**
             * If {@code true}, MagmaCube pets damage non-owner players on contact (vanilla
             * cube-mob contact damage). The owner is universally protected regardless of this
             * flag — pet-on-owner contact damage is always cancelled.
             *
             * <p>Deliberate attacks via {@code PetMeleeAttackGoal} (Aggressive/Farm/Duel
             * Behavior) on the pet's current target are never gated by this flag.
             */
            public static boolean CAN_HURT_PLAYERS_ON_CONTACT = false;
        }

        public static class Mooshroom {

            public static boolean CAN_GIVE_SOUP;
        }

        public static class Panda {

            public static boolean CAN_DROP_SLIMEBALL = true;
        }

        public static class Sheep {

            public static boolean CAN_BE_SHEARED = true;
            public static boolean CAN_REGROW_WOOL = true;
        }

        public static class Slime {

            /**
             * If {@code true}, Slime pets damage non-owner players on contact (vanilla
             * cube-mob contact damage). The owner is universally protected regardless of this
             * flag — pet-on-owner contact damage is always cancelled.
             *
             * <p>Deliberate attacks via {@code PetMeleeAttackGoal} (Aggressive/Farm/Duel
             * Behavior) on the pet's current target are never gated by this flag.
             */
            public static boolean CAN_HURT_PLAYERS_ON_CONTACT = false;
        }

        public static class Sniffer {

            public static boolean CAN_DIG_SEEDS = true;
        }

        public static class SnowGolem {

            public static boolean DISABLE_SNOW_TRACK = true;
        }
    }
}
