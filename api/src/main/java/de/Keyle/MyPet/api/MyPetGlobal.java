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

import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.util.ConfigItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Plugin-wide settings from {@code config.yml}, each a {@link ConfigKey}
 * declared as a {@code public static final} field — the single source of truth
 * for global config, mirroring the per-pet {@code ConfigKey} fields on each
 * {@code PetXxx} class. Read with {@code MyPetGlobal.Section.KEY.get()}; values
 * are hot-reloaded by {@code ConfigKeyRegistry.loadGlobalsFromYaml}.
 *
 * <p>Sections are nested classes matching the legacy {@code Configuration}
 * layout. The static initializer force-loads every nested section so their
 * field initializers fire and register with {@code ConfigKeyRegistry} (the
 * registry triggers this via {@code ensureGlobalsLoaded}).
 */
public final class MyPetGlobal {

    private MyPetGlobal() {}

    public static final class Misc {
        public static final ConfigKey<Boolean> CONSUME_LEASH_ITEM = ConfigKey.globalBool("MyPet.Leash.Consume", false);
        public static final ConfigKey<Boolean> ALLOW_RANGED_LEASHING = ConfigKey.globalBool("MyPet.Leash.AllowRanged", true);
        public static final ConfigKey<Boolean> OWNER_CAN_ATTACK_PET = ConfigKey.globalBool("MyPet.OwnerCanAttackPet", false);
        public static final ConfigKey<Boolean> DISABLE_PET_VS_PLAYER = ConfigKey.globalBool("MyPet.DisablePetVersusPlayer", false);
        public static final ConfigKey<Boolean> PET_KILLS_GIVE_PLAYER_REWARDS = ConfigKey.globalBool("MyPet.PetKillsGivePlayerRewards", true);
        public static final ConfigKey<Boolean> RETAIN_EQUIPMENT_ON_TAME = ConfigKey.globalBool("MyPet.RetainEquipmentOnTame", true);
        public static final ConfigKey<Boolean> INVISIBLE_LIKE_OWNER = ConfigKey.globalBool("MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible", true);
        public static final ConfigKey<Boolean> THROW_PLAYER_MOVE_EVENT_WHILE_RIDING = ConfigKey.globalBool("MyPet.Throw-PlayerMoveEvent-While-Riding", true);
        public static final ConfigKey<Boolean> DISABLE_ALL_ACTIONBAR_MESSAGES = ConfigKey.globalBool("MyPet.Disable-All-Actionbar-Messages", false);
        public static final ConfigKey<Boolean> RECALL_PET_AFTER_DESPAWN = ConfigKey.globalBool("MyPet.Recall-Pet-After-Despawn", true);
        public static final ConfigKey<String> OVERWRITE_LANGUAGE = ConfigKey.globalString("MyPet.OverwriteLanguages", "");
        public static final ConfigKey<String> WIKI_URL = ConfigKey.globalString("MyPet.Info.Wiki-URL", "https://wiki.mypet-plugin.de");
        // Strips a leading slash so the command is runnable via Bukkit.dispatchCommand.
        public static final ConfigKey<String> RIGHT_CLICK_COMMAND = ConfigKey.global("MyPet.Right-Click-Command", "", "",
                (config, path) -> {
                    String cmd = config.getString(path, "");
                    return cmd != null && cmd.startsWith("/") ? cmd.substring(1) : cmd;
                });
        public static final ConfigKey<Integer> MAX_STORED_PET_COUNT = ConfigKey.globalInt("MyPet.Max-Stored-Pet-Count", 45);
    }

    public static final class Entity {
        public static final ConfigKey<Double> MYPET_FOLLOW_START_DISTANCE = ConfigKey.globalDouble("MyPet.Entity.FollowStartDistance", 7.0);
    }

    public static final class Update {
        public static final ConfigKey<Boolean> ASYNC = ConfigKey.globalBool("MyPet.Update.In-Background", false);
        public static final ConfigKey<Boolean> CHECK = ConfigKey.globalBool("MyPet.Update.Check", true);
        public static final ConfigKey<Boolean> DOWNLOAD = ConfigKey.globalBool("MyPet.Update.Download", false);
        public static final ConfigKey<Boolean> SHOW_OP = ConfigKey.globalBool("MyPet.Update.OP-Notification", true);
    }

    /** Browser-based config editor (`/mypet editor`) — relay endpoints + on/off. */
    public static final class WebEditor {
        public static final ConfigKey<Boolean> ENABLED = ConfigKey.globalBool("MyPet.WebEditor.Enabled", true);
        public static final ConfigKey<String> BYTEBIN_URL = ConfigKey.globalString("MyPet.WebEditor.BytebinUrl", "https://bytebin.mypet-plugin.de");
        public static final ConfigKey<String> BYTESOCKS_URL = ConfigKey.globalString("MyPet.WebEditor.BytesocksUrl", "wss://bytesocks.mypet-plugin.de");
        public static final ConfigKey<String> EDITOR_URL = ConfigKey.globalString("MyPet.WebEditor.EditorUrl", "https://editor.mypet-plugin.de");
    }

    public static final class Repository {
        public static final ConfigKey<Long> EXTERNAL_LOAD_DELAY = ConfigKey.globalLong("MyPet.Repository.LoadDelay", 20L);
        public static final ConfigKey<String> REPOSITORY_TYPE = ConfigKey.globalString("MyPet.Repository.Type", "SQLite");
        public static final ConfigKey<String> CONVERT_FROM = ConfigKey.globalString("MyPet.Repository.ConvertFrom", "");

        public static final class MySQL {
            public static final ConfigKey<String> DATABASE = ConfigKey.globalString("MyPet.Repository.MySQL.Database", "mypet");
            public static final ConfigKey<String> PREFIX = ConfigKey.globalString("MyPet.Repository.MySQL.TablePrefix", "");
            public static final ConfigKey<String> HOST = ConfigKey.globalString("MyPet.Repository.MySQL.Host", "localhost");
            public static final ConfigKey<String> PASSWORD = ConfigKey.globalString("MyPet.Repository.MySQL.Password", "");
            public static final ConfigKey<String> USER = ConfigKey.globalString("MyPet.Repository.MySQL.User", "root");
            public static final ConfigKey<String> CHARACTER_ENCODING = ConfigKey.globalString("MyPet.Repository.MySQL.CharacterEncoding", "utf8");
            public static final ConfigKey<Integer> PORT = ConfigKey.globalInt("MyPet.Repository.MySQL.Port", 3306);
            public static final ConfigKey<Integer> POOL_SIZE = ConfigKey.globalInt("MyPet.Repository.MySQL.MaxConnections", Runtime.getRuntime().availableProcessors() * 2);
        }
    }

    public static final class Respawn {
        public static final ConfigKey<Boolean> DISABLE_AUTO_RESPAWN = ConfigKey.globalBool("MyPet.Respawn.Time.Disabled", false);
        public static final ConfigKey<Integer> TIME_FACTOR = ConfigKey.globalInt("MyPet.Respawn.Time.Default.Factor", 5);
        public static final ConfigKey<Integer> TIME_PLAYER_FACTOR = ConfigKey.globalInt("MyPet.Respawn.Time.Player.Factor", 5);
        public static final ConfigKey<Integer> TIME_FIXED = ConfigKey.globalInt("MyPet.Respawn.Time.Default.Fixed", 0);
        public static final ConfigKey<Integer> TIME_PLAYER_FIXED = ConfigKey.globalInt("MyPet.Respawn.Time.Player.Fixed", 0);
        public static final ConfigKey<Double> COSTS_FACTOR = ConfigKey.globalDouble("MyPet.Respawn.EconomyCost.Factor", 1.0);
        public static final ConfigKey<Double> COSTS_FIXED = ConfigKey.globalDouble("MyPet.Respawn.EconomyCost.Fixed", 0.0);
    }

    public static final class Name {
        public static final ConfigKey<Integer> MAX_LENGTH = ConfigKey.globalInt("MyPet.Name.MaxLength", 32);

        public static final class Tag {
            public static final ConfigKey<Boolean> SHOW = ConfigKey.globalBool("MyPet.Name.Tag.Show", true);
            public static final ConfigKey<String> PREFIX = ConfigKey.globalString("MyPet.Name.Tag.Prefix", "<aqua>");
            public static final ConfigKey<String> SUFFIX = ConfigKey.globalString("MyPet.Name.Tag.Suffix", "");
        }
    }

    public static final class Permissions {
        public static final ConfigKey<Boolean> EXTENDED = ConfigKey.globalBool("MyPet.Permissions.Extended", false);
    }

    public static final class LevelSystem {
        public static final ConfigKey<String> CALCULATION_MODE = ConfigKey.globalString("MyPet.LevelSystem.CalculationMode", "Default");

        /** Tunable parameters for the built-in {@code Linear}/{@code Power}/{@code Exponential} XP curves. */
        public static final class Curve {
            public static final ConfigKey<Double> LINEAR_BASE = ConfigKey.globalDouble("MyPet.LevelSystem.Curve.Linear.Base", 17.0);
            public static final ConfigKey<Double> POWER_FACTOR = ConfigKey.globalDouble("MyPet.LevelSystem.Curve.Power.Factor", 7.0);
            public static final ConfigKey<Double> POWER_EXPONENT = ConfigKey.globalDouble("MyPet.LevelSystem.Curve.Power.Exponent", 1.5);
            public static final ConfigKey<Double> EXPONENTIAL_BASE = ConfigKey.globalDouble("MyPet.LevelSystem.Curve.Exponential.Base", 10.0);
            public static final ConfigKey<Double> EXPONENTIAL_GROWTH = ConfigKey.globalDouble("MyPet.LevelSystem.Curve.Exponential.Growth", 1.1);
        }

        public static final class Experience {
            public static final ConfigKey<Integer> LOSS_PERCENT = ConfigKey.globalInt("MyPet.Exp.Loss.Percent", 0);
            public static final ConfigKey<Double> LOSS_FIXED = ConfigKey.globalDouble("MyPet.Exp.Loss.Fixed", 0.0);
            public static final ConfigKey<Boolean> ALLOW_LEVEL_DOWNGRADE = ConfigKey.globalBool("MyPet.Exp.Loss.Allow-Level-Drowngrade", false);
            public static final ConfigKey<Boolean> DROP_LOST_EXP = ConfigKey.globalBool("MyPet.Exp.Loss.Drop", true);
            public static final ConfigKey<Boolean> DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = ConfigKey.globalBool("MyPet.Exp.DamageWeightedExperienceDistribution", true);
            public static final ConfigKey<Boolean> ALWAYS_GRANT_PASSIVE_XP = ConfigKey.globalBool("MyPet.Exp.Passive.Always-Grant-Passive-XP", true);
            public static final ConfigKey<Integer> PASSIVE_PERCENT_PER_MONSTER = ConfigKey.globalInt("MyPet.Exp.Passive.PercentPerMonster", 25);
            public static final ConfigKey<Integer> LEVEL_CAP = ConfigKey.globalInt("MyPet.Exp.LevelCap", 100);
            // Only valid SpawnReason names survive; everything else is dropped.
            public static final ConfigKey<Set<String>> PREVENT_FROM_SPAWN_REASON = ConfigKey.global(
                    "MyPet.Exp.Gain.PreventFromSpawnReason", new HashSet<>(), new java.util.ArrayList<>(),
                    MyPetGlobal::readSpawnReasons);
            public static final ConfigKey<Set<String>> DISABLED_WORLDS = ConfigKey.globalStringSet("MyPet.Exp.Disabled-Worlds");

            public static final class Modifier {
                public static final ConfigKey<Double> GLOBAL = ConfigKey.globalDouble("MyPet.Exp.Modifier.Global", 1);
                public static final ConfigKey<Boolean> PERMISSION = ConfigKey.globalBool("MyPet.Exp.Modifier.Use-Permissions", false);
            }
        }
    }

    public static final class HungerSystem {
        public static final ConfigKey<Boolean> USE_HUNGER_SYSTEM = ConfigKey.globalBool("MyPet.HungerSystem.Active", true);
        public static final ConfigKey<Integer> HUNGER_SYSTEM_TIME = ConfigKey.globalInt("MyPet.HungerSystem.Time", 60);
        public static final ConfigKey<Double> HUNGER_SYSTEM_SATURATION_PER_FEED = ConfigKey.globalDouble("MyPet.HungerSystem.SaturationPerFeed", 6.0);
        public static final ConfigKey<Boolean> AFFECT_RIDE_SPEED = ConfigKey.globalBool("MyPet.HungerSystem.Affect-Ride-Speed", true);
        public static final ConfigKey<Boolean> AFFECT_BEACON_RANGE = ConfigKey.globalBool("MyPet.HungerSystem.Affect-Beacon-Range", true);
        public static final ConfigKey<Double> HUNGER_SYSTEM_FIXED = ConfigKey.globalDouble("MyPet.HungerSystem.Damage.Fixed", 1.0);
        public static final ConfigKey<Double> HUNGER_SYSTEM_FACTOR = ConfigKey.globalDouble("MyPet.HungerSystem.Damage.Factor", 0.0);
        public static final ConfigKey<Double> HUNGER_SYSTEM_TIME_BEFORE_DAMAGE = ConfigKey.globalDouble("MyPet.HungerSystem.Damage.Time-Before-Damage", 5.0);
        public static final ConfigKey<Boolean> HUNGER_SYSTEM_CAN_KILL = ConfigKey.globalBool("MyPet.HungerSystem.Damage.can-kill", false);
        public static final ConfigKey<Boolean> FEED_FROM_INVENTORY = ConfigKey.globalBool("MyPet.HungerSystem.Feed-From-Inventory", true);
    }

    public static final class Skilltree {
        public static final ConfigKey<Integer> SWITCH_FEE_PERCENT = ConfigKey.globalInt("MyPet.Skilltree.SwitchFee.Percent", 5);
        public static final ConfigKey<Double> SWITCH_FEE_FIXED = ConfigKey.globalDouble("MyPet.Skilltree.SwitchFee.Fixed", 0.0);
        public static final ConfigKey<Boolean> SWITCH_FEE_ADMIN = ConfigKey.globalBool("MyPet.Skilltree.SwitchFee.Admin", false);
        public static final ConfigKey<Boolean> AUTOMATIC_SKILLTREE_ASSIGNMENT = ConfigKey.globalBool("MyPet.Skilltree.AutomaticAssignment", false);
        public static final ConfigKey<Boolean> RANDOM_SKILLTREE_ASSIGNMENT = ConfigKey.globalBool("MyPet.Skilltree.RandomAssignment", false);
        public static final ConfigKey<Boolean> CHOOSE_SKILLTREE_ONLY_ONCE = ConfigKey.globalBool("MyPet.Skilltree.ChooseOnce", false);
        public static final ConfigKey<Boolean> FREE_ASCENSION = ConfigKey.globalBool("MyPet.Skilltree.FreeAscension", true);
        public static final ConfigKey<Boolean> PREVENT_LEVELLING_WITHOUT_SKILLTREE = ConfigKey.globalBool("MyPet.Skilltree.PreventLevellingWithout", true);

        public static final class Skill {
            public static final ConfigKey<ConfigItem> CONTROL_ITEM = ConfigKey.globalItem("MyPet.Skill.Control.Item", "lead");

            public static final class Ride {
                public static final ConfigKey<ConfigItem> RIDE_ITEM = ConfigKey.globalItem("MyPet.Skill.Ride.Item", "lead");
                public static final ConfigKey<Double> HUNGER_PER_METER = ConfigKey.globalDouble("MyPet.Skill.Ride.HungerPerMeter", 0.01);
                public static final ConfigKey<Boolean> PREVENT_TELEPORTATION = ConfigKey.globalBool("MyPet.Skill.Ride.Prevent-Teleportation-While-Riding", false);
            }

            public static final class Beacon {
                public static final ConfigKey<Integer> HUNGER_DECREASE_TIME = ConfigKey.globalInt("MyPet.Skill.Beacon.HungerDecreaseTime", 100);
                public static final ConfigKey<Boolean> PARTY_SUPPORT = ConfigKey.globalBool("MyPet.Skill.Beacon.Party-Support", true);
                public static final ConfigKey<Boolean> DISABLE_HEAD_TEXTURE = ConfigKey.globalBool("MyPet.Skill.Beacon.Disable-Head-Textures", false);
                public static final ConfigKey<Boolean> ZONE_MESSAGES = ConfigKey.globalBool("MyPet.Skill.Beacon.Zone-Messages", true);
            }

            public static final class Backpack {
                public static final ConfigKey<Boolean> OPEN_IN_CREATIVE = ConfigKey.globalBool("MyPet.Skill.Backpack.Creative", false);
                public static final ConfigKey<Boolean> DROP_WHEN_OWNER_DIES = ConfigKey.globalBool("MyPet.Skill.Backpack.DropWhenOwnerDies", false);
            }
        }
    }

    private static Set<String> readSpawnReasons(ConfigurationSection config, String path) {
        Set<String> reasons = new HashSet<>();
        if (config.isList(path)) {
            for (String reason : config.getStringList(path)) {
                reason = reason.toUpperCase(Locale.ROOT);
                try {
                    CreatureSpawnEvent.SpawnReason.valueOf(reason);
                    reasons.add(reason);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return reasons;
    }

    // Force every nested section to initialize so its ConfigKey field
    // initializers fire and register with ConfigKeyRegistry. Initializing a
    // nested class does not init its siblings or parent, so each is listed.
    static {
        Class<?>[] sections = {
                Misc.class, Entity.class, Update.class, WebEditor.class,
                Repository.class, Repository.MySQL.class,
                Respawn.class, Name.class, Name.Tag.class, Permissions.class,
                LevelSystem.class, LevelSystem.Curve.class, LevelSystem.Experience.class, LevelSystem.Experience.Modifier.class,
                HungerSystem.class,
                Skilltree.class, Skilltree.Skill.class, Skilltree.Skill.Ride.class,
                Skilltree.Skill.Beacon.class, Skilltree.Skill.Backpack.class
        };
        for (Class<?> section : sections) {
            try {
                Class.forName(section.getName(), true, section.getClassLoader());
            } catch (ClassNotFoundException ignored) {
            }
        }
    }
}
