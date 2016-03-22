/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.api.util.ConfigItem;

public class Configuration {
    public static class Misc {
        public static boolean CONSUME_LEASH_ITEM = false;
        public static boolean OWNER_CAN_ATTACK_PET = false;
        public static boolean DISABLE_PET_VS_PLAYER = false;
        public static boolean REMOVE_PETS_AFTER_RELEASE = false;
        public static double MYPET_FOLLOW_START_DISTANCE = 7.0F;
        public static boolean RELEASE_PETS_ON_DEATH = false;
        public static boolean RETAIN_EQUIPMENT_ON_TAME = true;
        public static boolean INVISIBLE_LIKE_OWNER = true;
        public static String WIKI_URL = "https://wiki.mypet-plugin.de";
        public static int MAX_STORED_PET_COUNT = 45;
    }

    public static class Log {
        public static String LEVEL = "INFO";
    }

    public static class Repository {
        public static String REPOSITORY_TYPE = "NBT";

        public static class NBT {
            public static int AUTOSAVE_TIME = 60;
            public static boolean SAVE_ON_PET_ADD = true;
            public static boolean SAVE_ON_PET_UPDATE = true;
            public static boolean SAVE_ON_PET_REMOVE = true;
            public static boolean SAVE_ON_PLAYER_ADD = true;
            public static boolean SAVE_ON_PLAYER_UPDATE = true;
            public static boolean SAVE_ON_PLAYER_REMOVE = true;
            public static boolean MAKE_BACKUPS = true;
            public static int SAVE_INTERVAL = 1440;
            public static String DATE_FORMAT = "yyyy_MM_dd_HH.mm";
        }
    }

    public static class Respawn {
        public static int TIME_FACTOR = 5;
        public static int TIME_PLAYER_FACTOR = 5;
        public static int TIME_FIXED = 0;
        public static int TIME_PLAYER_FIXED = 0;
        public static double COSTS_FACTOR = 1.0;
        public static double COSTS_FIXED = 0.0;
    }

    public static class Name {
        public static String OVERHEAD_PREFIX = "<aqua>";
        public static String OVERHEAD_SUFFIX = "";
        public static int MAX_LENGTH = 32;
        public static boolean OVERHEAD_NAME = true;
    }

    public static class Permissions {
        public static boolean ENABLED = true;
        public static boolean EXTENDED = false;
        public static boolean LEGACY = false;
    }

    public static class LevelSystem {
        public static String CALCULATION_MODE = "Default";
        public static boolean FIREWORK = true;
        public static int FIREWORK_COLOR = 0x00FF00;

        public static class Experience {
            public static int LOSS_PERCENT = 0;
            public static double LOSS_FIXED = 0;
            public static boolean DROP_LOST_EXP = true;
            public static boolean FROM_MONSTER_SPAWNER_MOBS = true;
            public static boolean DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = false;
            public static boolean ALWAYS_GRANT_PASSIVE_XP = true;
            public static int PASSIVE_PERCENT_PER_MONSTER = 25;
            public static int LEVEL_CAP = 100;
        }
    }

    public static class HungerSystem {
        public static boolean USE_HUNGER_SYSTEM = true;
        public static int HUNGER_SYSTEM_TIME = 60;
        public static int HUNGER_SYSTEM_POINTS_PER_FEED = 6;
    }

    public static class Skilltree {
        public static int SWITCH_PENALTY_PERCENT = 5;
        public static double SWITCH_PENALTY_FIXED = 0.0;
        public static boolean SWITCH_PENALTY_ADMIN = false;
        public static boolean AUTOMATIC_SKILLTREE_ASSIGNMENT = false;
        public static boolean RANDOM_SKILLTREE_ASSIGNMENT = false;
        public static boolean CHOOSE_SKILLTREE_ONLY_ONCE = false;
        public static boolean PREVENT_LEVELLING_WITHOUT_SKILLTREE = true;
        public static boolean INHERIT_ALREADY_INHERITED_SKILLS = false;

        public static class Skill {
            public static ConfigItem CONTROL_ITEM;

            public static class Ride {
                public static ConfigItem RIDE_ITEM;
                public static double HUNGER_PER_METER = 0.01;
            }

            public static class Beacon {
                public static int HUNGER_DECREASE_TIME = 100;
                public static boolean PARTY_SUPPORT = true;
            }

            public static class Inventory {
                public static boolean OPEN_IN_CREATIVE = true;
                public static boolean DROP_WHEN_OWNER_DIES = true;
            }
        }
    }

    public static class MyPet {
        public static class Chicken {
            public static ConfigItem GROW_UP_ITEM;
            public static boolean CAN_LAY_EGGS = true;
        }

        public static class Cow {
            public static boolean CAN_GIVE_MILK = true;
            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Sheep {

            public static boolean CAN_BE_SHEARED = true;
            public static boolean CAN_REGROW_WOOL = true;
            public static ConfigItem GROW_UP_ITEM;
        }

        public static class IronGolem {

            public static boolean CAN_THROW_UP = true;
        }

        public static class Snowman {

            public static boolean FIX_SNOW_TRACK = true;
        }

        public static class Horse {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Mooshroom {

            public static ConfigItem GROW_UP_ITEM;
            public static boolean CAN_GIVE_SOUP;
        }

        public static class Ocelot {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Pig {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Villager {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Wolf {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Zombie {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class PigZombie {

            public static ConfigItem GROW_UP_ITEM;
        }

        public static class Rabbit {
            public static ConfigItem GROW_UP_ITEM;
        }
    }

    public static class Hooks {
        public static String BUNGEE_MODE = "online";
        public static boolean USE_ECONOMY = true;
        public static boolean DISABLE_PETS_IN_MINIGAMES = true;
        public static boolean DISABLE_PETS_IN_ARENA = true;
        public static boolean DISABLE_PETS_IN_SURVIVAL_GAMES = true;
        public static boolean DISABLE_PETS_IN_HUNGER_GAMES = true;
        public static boolean DISABLE_PETS_IN_MOB_ARENA = true;
        public static boolean USE_Towny = true;
        public static boolean USE_Factions = true;
        public static boolean USE_WorldGuard = true;
        public static boolean USE_Citizens = true;
        public static boolean USE_Heroes = true;
        public static boolean USE_Regios = true;
        public static boolean USE_MobArena = true;
        public static boolean USE_McMMO = true;
        public static boolean USE_Residence = true;
        public static boolean USE_AncientRPG = true;
        public static boolean USE_GriefPrevention = true;
        public static boolean USE_PvPArena = true;
        public static boolean USE_PvPManager = true;
        public static boolean USE_SurvivalGame = true;

        public static class SkillAPI {
            public static boolean GRANT_EXP = true;
            public static boolean DISABLE_VANILLA_EXP = false;
            public static int EXP_PERCENT = 100;
        }
    }
}