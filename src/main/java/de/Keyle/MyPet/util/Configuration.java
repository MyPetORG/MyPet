/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util;

import com.google.common.collect.Lists;
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.commands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.chicken.MyChicken;
import de.Keyle.MyPet.entity.types.cow.MyCow;
import de.Keyle.MyPet.entity.types.horse.MyHorse;
import de.Keyle.MyPet.entity.types.irongolem.MyIronGolem;
import de.Keyle.MyPet.entity.types.mooshroom.MyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.MyOcelot;
import de.Keyle.MyPet.entity.types.pig.MyPig;
import de.Keyle.MyPet.entity.types.pigzombie.MyPigZombie;
import de.Keyle.MyPet.entity.types.sheep.MySheep;
import de.Keyle.MyPet.entity.types.snowman.MySnowman;
import de.Keyle.MyPet.entity.types.villager.MyVillager;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.entity.types.zombie.MyZombie;
import de.Keyle.MyPet.skill.MonsterExperience;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    public static class Misc {
        public static boolean CONSUME_LEASH_ITEM = false;
        public static boolean ALWAYS_SHOW_LEASH_FOR_OWNER = false;
        public static boolean OWNER_CAN_ATTACK_PET = false;
        public static boolean DISABLE_PET_VS_PLAYER = false;
        public static boolean REMOVE_PETS_AFTER_RELEASE = false;
        public static double MYPET_FOLLOW_START_DISTANCE = 7.0F;
        public static boolean RELEASE_PETS_ON_DEATH = false;
        public static boolean RETAIN_EQUIPMENT_ON_TAME = true;
        public static boolean INVISIBLE_LIKE_OWNER = true;
        public static String WIKI_URL = "http://mypet.keyle.de";
    }

    public static class Log {
        public static boolean INFO = true;
        public static boolean ERROR = true;
        public static boolean WARNING = true;
    }

    public static class Repository {
        public static String REPOSITORY_TYPE = "NBT";
        public static String CONVERT_FROM = "-";

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

        public static class MySQL {
            public static String DATABASE = "mypet";
            public static String HOST = "localhost";
            public static String PASSWORD = "";
            public static String USER = "root";
            public static int PORT = 3306;
        }

        public static class MongoDB {
            public static String DATABASE = "mypet";
            public static String HOST = "localhost";
            public static String PASSWORD = "";
            public static String USER = "";
            public static int PORT = 27017;
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
        public static boolean CHOOSE_SKILLTREE_ONLY_ONCE = true;
        public static boolean PREVENT_LEVELLING_WITHOUT_SKILLTREE = true;
        public static boolean INHERIT_ALREADY_INHERITED_SKILLS = false;

        public static class Skill {
            public static ConfigItem CONTROL_ITEM;
            public static ConfigItem RIDE_ITEM;

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

    public static void setDefault() {
        FileConfiguration config = MyPetPlugin.getPlugin().getConfig();

        config.addDefault("MyPet.Leash.Consume", Misc.CONSUME_LEASH_ITEM);
        config.addDefault("MyPet.Leash.ShowAlwaysForOwner", Misc.ALWAYS_SHOW_LEASH_FOR_OWNER);
        config.addDefault("MyPet.OwnerCanAttackPet", Misc.OWNER_CAN_ATTACK_PET);
        config.addDefault("MyPet.DisablePetVersusPlayer", Misc.DISABLE_PET_VS_PLAYER);
        config.addDefault("MyPet.RemovePetsAfterRelease", Misc.REMOVE_PETS_AFTER_RELEASE);
        config.addDefault("MyPet.FollowStartDistance", Misc.MYPET_FOLLOW_START_DISTANCE);
        config.addDefault("MyPet.ReleasePetsOnDeath", Misc.RELEASE_PETS_ON_DEATH);
        config.addDefault("MyPet.RetainEquipmentOnTame", Misc.RETAIN_EQUIPMENT_ON_TAME);
        config.addDefault("MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible", Misc.INVISIBLE_LIKE_OWNER);
        config.addDefault("MyPet.Log.INFO", Log.INFO);
        config.addDefault("MyPet.Log.ERROR", Log.ERROR);
        config.addDefault("MyPet.Log.WARNING", Log.WARNING);

        config.addDefault("MyPet.Repository.Type", Repository.REPOSITORY_TYPE);
        config.addDefault("MyPet.Repository.ConvertFrom", Repository.CONVERT_FROM);

        config.addDefault("MyPet.Repository.NBT.AutoSaveTime", Repository.NBT.AUTOSAVE_TIME);
        config.addDefault("MyPet.Repository.NBT.Pet.SaveOnAdd", Repository.NBT.SAVE_ON_PET_ADD);
        config.addDefault("MyPet.Repository.NBT.Pet.SaveOnUpdate", Repository.NBT.SAVE_ON_PET_UPDATE);
        config.addDefault("MyPet.Repository.NBT.Pet.SaveOnRemove", Repository.NBT.SAVE_ON_PET_REMOVE);
        config.addDefault("MyPet.Repository.NBT.Player.SaveOnAdd", Repository.NBT.SAVE_ON_PLAYER_ADD);
        config.addDefault("MyPet.Repository.NBT.Player.SaveOnUpdate", Repository.NBT.SAVE_ON_PLAYER_UPDATE);
        config.addDefault("MyPet.Repository.NBT.Player.SaveOnRemove", Repository.NBT.SAVE_ON_PLAYER_REMOVE);
        config.addDefault("MyPet.Repository.NBT.Backup.Active", Repository.NBT.MAKE_BACKUPS);
        config.addDefault("MyPet.Repository.NBT.Backup.SaveInterval", Repository.NBT.SAVE_INTERVAL);
        config.addDefault("MyPet.Repository.NBT.Backup.DateFormat", Repository.NBT.DATE_FORMAT);

        config.addDefault("MyPet.Repository.MySQL.Database", Repository.MySQL.DATABASE);
        config.addDefault("MyPet.Repository.MySQL.Host", Repository.MySQL.HOST);
        config.addDefault("MyPet.Repository.MySQL.Password", Repository.MySQL.PASSWORD);
        config.addDefault("MyPet.Repository.MySQL.User", Repository.MySQL.USER);
        config.addDefault("MyPet.Repository.MySQL.Port", Repository.MySQL.PORT);

        config.addDefault("MyPet.Repository.MongoDB.Database", Repository.MongoDB.DATABASE);
        config.addDefault("MyPet.Repository.MongoDB.Host", Repository.MongoDB.HOST);
        config.addDefault("MyPet.Repository.MongoDB.Password", Repository.MongoDB.PASSWORD);
        config.addDefault("MyPet.Repository.MongoDB.User", Repository.MongoDB.USER);
        config.addDefault("MyPet.Repository.MongoDB.Port", Repository.MongoDB.PORT);

        config.addDefault("MyPet.Respawn.Time.Default.Factor", Respawn.TIME_FACTOR);
        config.addDefault("MyPet.Respawn.Time.Player.Factor", Respawn.TIME_PLAYER_FACTOR);
        config.addDefault("MyPet.Respawn.Time.Default.Fixed", Respawn.TIME_FIXED);
        config.addDefault("MyPet.Respawn.Time.Player.Fixed", Respawn.TIME_PLAYER_FIXED);
        config.addDefault("MyPet.Respawn.EconomyCost.Fixed", Respawn.COSTS_FIXED);
        config.addDefault("MyPet.Respawn.EconomyCost.Factor", Respawn.COSTS_FACTOR);

        config.addDefault("MyPet.Permissions.Enabled", Permissions.ENABLED);
        config.addDefault("MyPet.Permissions.UseExtendedPermissions", Permissions.EXTENDED);

        config.addDefault("MyPet.LevelSystem.CalculationMode", LevelSystem.CALCULATION_MODE);
        config.addDefault("MyPet.LevelSystem.Firework.Enabled", LevelSystem.FIREWORK);
        config.addDefault("MyPet.LevelSystem.Firework.Color", "#00FF00");

        config.addDefault("MyPet.HungerSystem.Active", HungerSystem.USE_HUNGER_SYSTEM);
        config.addDefault("MyPet.HungerSystem.Time", HungerSystem.HUNGER_SYSTEM_TIME);
        config.addDefault("MyPet.HungerSystem.HungerPointsPerFeed", HungerSystem.HUNGER_SYSTEM_POINTS_PER_FEED);

        config.addDefault("MyPet.Skilltree.AutomaticAssignment", Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT);
        config.addDefault("MyPet.Skilltree.RandomAssignment", Skilltree.RANDOM_SKILLTREE_ASSIGNMENT);
        config.addDefault("MyPet.Skilltree.InheritAlreadyInheritedSkills", Skilltree.INHERIT_ALREADY_INHERITED_SKILLS);
        config.addDefault("MyPet.Skilltree.ChooseOnce", Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE);
        config.addDefault("MyPet.Skilltree.PreventLevellingWithout", Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE);
        config.addDefault("MyPet.Skilltree.SwitchPenaltyFixed", Skilltree.SWITCH_PENALTY_FIXED);
        config.addDefault("MyPet.Skilltree.SwitchPenaltyPercent", Skilltree.SWITCH_PENALTY_PERCENT);
        config.addDefault("MyPet.Skilltree.SwitchPenaltyAdmin", Skilltree.SWITCH_PENALTY_ADMIN);

        config.addDefault("MyPet.Hooks.BungeeCord.UUID-Mode", Hooks.BUNGEE_MODE);
        config.addDefault("MyPet.Hooks.Towny", Hooks.USE_Towny);
        config.addDefault("MyPet.Hooks.Heroes", Hooks.USE_Heroes);
        config.addDefault("MyPet.Hooks.Factions", Hooks.USE_Factions);
        config.addDefault("MyPet.Hooks.WorldGuard", Hooks.USE_WorldGuard);
        config.addDefault("MyPet.Hooks.Citizens", Hooks.USE_Citizens);
        config.addDefault("MyPet.Hooks.mcMMO", Hooks.USE_McMMO);
        config.addDefault("MyPet.Hooks.Regios", Hooks.USE_Regios);
        config.addDefault("MyPet.Hooks.MobArena.PvP", Hooks.USE_MobArena);
        config.addDefault("MyPet.Hooks.MobArena.DisablePetsInArena", Hooks.DISABLE_PETS_IN_MOB_ARENA);
        config.addDefault("MyPet.Hooks.Residence", Hooks.USE_Residence);
        config.addDefault("MyPet.Hooks.AncientRPG", Hooks.USE_AncientRPG);
        config.addDefault("MyPet.Hooks.GriefPrevention", Hooks.USE_GriefPrevention);
        config.addDefault("MyPet.Hooks.PvPManager", Hooks.USE_PvPManager);
        config.addDefault("MyPet.Hooks.Minigames.DisablePetsInGames", Hooks.DISABLE_PETS_IN_MINIGAMES);
        config.addDefault("MyPet.Hooks.PvPArena.DisablePetsInArena", Hooks.DISABLE_PETS_IN_ARENA);
        config.addDefault("MyPet.Hooks.PvPArena.PvP", Hooks.USE_PvPArena);
        config.addDefault("MyPet.Hooks.SurvivalGames.PvP", Hooks.USE_SurvivalGame);
        config.addDefault("MyPet.Hooks.SurvivalGames.DisablePetsInGames", Hooks.DISABLE_PETS_IN_SURVIVAL_GAMES);
        config.addDefault("MyPet.Hooks.MyHungerGames.DisablePetsInGames", Hooks.DISABLE_PETS_IN_HUNGER_GAMES);
        config.addDefault("MyPet.Hooks.BattleArena.DisablePetsInArena", Hooks.DISABLE_PETS_IN_ARENA);
        config.addDefault("MyPet.Hooks.Vault.Economy", Hooks.USE_ECONOMY);
        config.addDefault("MyPet.Hooks.SkillAPI.GrantExp", Hooks.SkillAPI.GRANT_EXP);
        config.addDefault("MyPet.Hooks.SkillAPI.Disable-Vanilla-Exp", Hooks.SkillAPI.DISABLE_VANILLA_EXP);

        config.addDefault("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"));
        config.addDefault("MyPet.Name.MaxLength", Name.MAX_LENGTH);
        config.addDefault("MyPet.Name.OverHead.Visible", Name.OVERHEAD_NAME);
        config.addDefault("MyPet.Name.OverHead.Prefix", Name.OVERHEAD_PREFIX);
        config.addDefault("MyPet.Name.OverHead.Suffix", Name.OVERHEAD_SUFFIX);

        config.addDefault("MyPet.Exp.DamageWeightedExperienceDistribution", LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION);
        config.addDefault("MyPet.Exp.Passive.Always-Grant-Passive-XP", LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP);
        config.addDefault("MyPet.Exp.Passive.PercentPerMonster", LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER);
        config.addDefault("MyPet.Exp.Loss.Percent", LevelSystem.Experience.LOSS_PERCENT);
        config.addDefault("MyPet.Exp.Loss.Fixed", LevelSystem.Experience.LOSS_FIXED);
        config.addDefault("MyPet.Exp.Loss.Drop", LevelSystem.Experience.DROP_LOST_EXP);
        config.addDefault("MyPet.Exp.Gain.MonsterSpawner", LevelSystem.Experience.FROM_MONSTER_SPAWNER_MOBS);
        config.addDefault("MyPet.Exp.LevelCap", LevelSystem.Experience.LEVEL_CAP);

        config.addDefault("MyPet.Skill.Control.Item", Material.LEASH.getId());
        config.addDefault("MyPet.Skill.Ride.Item", Material.LEASH.getId());
        config.addDefault("MyPet.Skill.Inventory.Creative", Skilltree.Skill.Inventory.OPEN_IN_CREATIVE);
        config.addDefault("MyPet.Skill.Inventory.DropWhenOwnerDies", Skilltree.Skill.Inventory.DROP_WHEN_OWNER_DIES);
        config.addDefault("MyPet.Skill.Beacon.HungerDecreaseTime", Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME);
        config.addDefault("MyPet.Skill.Beacon.Party-Support", Skilltree.Skill.Beacon.PARTY_SUPPORT);

        config.addDefault("MyPet.Info.Wiki-URL", Misc.WIKI_URL);
        config.addDefault("MyPet.Info.AdminOnly.PetName", PetInfoDisplay.Name.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetOwner", PetInfoDisplay.Owner.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetHP", PetInfoDisplay.HP.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetDamage", PetInfoDisplay.Damage.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetHunger", PetInfoDisplay.Hunger.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetLevel", PetInfoDisplay.Level.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetEXP", PetInfoDisplay.Exp.adminOnly);
        config.addDefault("MyPet.Info.AdminOnly.PetSkilltree", PetInfoDisplay.Skilltree.adminOnly);

        for (EntityType entityType : MonsterExperience.mobExp.keySet()) {
            config.addDefault("MyPet.Exp.Active." + entityType.getName() + ".Min", MonsterExperience.getMonsterExperience(entityType).getMin());
            config.addDefault("MyPet.Exp.Active." + entityType.getName() + ".Max", MonsterExperience.getMonsterExperience(entityType).getMax());
        }

        config.options().copyDefaults(true);
        MyPetPlugin.getPlugin().saveConfig();

        File petConfigFile = new File(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "pet-config.yml");
        config = new YamlConfiguration();

        if (petConfigFile.exists()) {
            try {
                config.load(petConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        for (MyPetType petType : MyPetType.values()) {
            MyPetInfo pi = petType.getMyPetClass().getAnnotation(MyPetInfo.class);
            if (pi == null) {
                continue;
            }

            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".HP", pi.hp());
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".Speed", pi.walkSpeed());
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".Food", Util.linkFood(pi.food()));
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".LeashFlags", Util.linkLeashFlags(pi.leashFlags()));
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFactor", 0);
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFixed", 0);
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".LeashItem", Material.LEASH.getId());
        }

        config.addDefault("MyPet.Pets.Chicken.CanLayEggs", MyChicken.CAN_LAY_EGGS);
        config.addDefault("MyPet.Pets.Cow.CanGiveMilk", MyCow.CAN_GIVE_MILK);
        config.addDefault("MyPet.Pets.Sheep.CanBeSheared", MySheep.CAN_BE_SHEARED);
        config.addDefault("MyPet.Pets.Sheep.CanRegrowWool", MySheep.CAN_REGROW_WOOL);
        config.addDefault("MyPet.Pets.IronGolem.CanThrowUp", MyIronGolem.CAN_THROW_UP);
        config.addDefault("MyPet.Pets.Snowman.FixSnowTrack", MySnowman.FIX_SNOW_TRACK);
        config.addDefault("MyPet.Pets.Chicken.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Cow.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Horse.GrowUpItem", Material.BREAD.getId());
        config.addDefault("MyPet.Pets.Mooshroom.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Mooshroom.CanGiveStew", MyMooshroom.CAN_GIVE_SOUP);
        config.addDefault("MyPet.Pets.Ocelot.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Pig.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Sheep.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Villager.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Wolf.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Zombie.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.PigZombie.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Rabbit.GrowUpItem", Material.POTION.getId());

        config.options().copyDefaults(true);
        try {
            config.save(petConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadConfiguration() {
        FileConfiguration config = MyPetPlugin.getPlugin().getConfig();

        Misc.CONSUME_LEASH_ITEM = config.getBoolean("MyPet.Leash.Consume", false);
        Misc.ALWAYS_SHOW_LEASH_FOR_OWNER = config.getBoolean("MyPet.Leash.ShowAlwaysForOwner", false);

        Skilltree.Skill.CONTROL_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Skill.Control.Item", "" + Material.LEASH.getId()));
        Skilltree.Skill.RIDE_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Skill.Ride.Item", "" + Material.LEASH.getId()));
        Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME = config.getInt("MyPet.Skill.Beacon.HungerDecreaseTime", 100);
        Skilltree.Skill.Beacon.PARTY_SUPPORT = config.getBoolean("MyPet.Skill.Beacon.Party-Support", true);
        Skilltree.Skill.Inventory.OPEN_IN_CREATIVE = config.getBoolean("MyPet.Skill.Inventory.Creative", true);
        Skilltree.Skill.Inventory.DROP_WHEN_OWNER_DIES = config.getBoolean("MyPet.Skill.Inventory.DropWhenOwnerDies", false);

        Skilltree.SWITCH_PENALTY_FIXED = config.getDouble("MyPet.Skilltree.SwitchPenaltyFixed", 0.0);
        Skilltree.SWITCH_PENALTY_PERCENT = config.getInt("MyPet.Skilltree.SwitchPenaltyPercent", 5);
        Skilltree.SWITCH_PENALTY_ADMIN = config.getBoolean("MyPet.Skilltree.SwitchPenaltyAdmin", false);
        Skilltree.INHERIT_ALREADY_INHERITED_SKILLS = config.getBoolean("MyPet.Skilltree.InheritAlreadyInheritedSkills", false);
        Respawn.TIME_FACTOR = config.getInt("MyPet.Respawn.Time.Default.Factor", 5);
        Respawn.TIME_PLAYER_FACTOR = config.getInt("MyPet.Respawn.Time.Player.Factor", 5);
        Respawn.TIME_FIXED = config.getInt("MyPet.Respawn.Time.Default.Fixed", 0);
        Respawn.TIME_PLAYER_FIXED = config.getInt("MyPet.Respawn.Time.Player.Fixed", 0);
        Respawn.COSTS_FACTOR = config.getDouble("MyPet.Respawn.EconomyCost.Factor", 1.0);
        Respawn.COSTS_FIXED = config.getDouble("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT = config.getBoolean("MyPet.Skilltree.AutomaticAssignment", false);
        Skilltree.RANDOM_SKILLTREE_ASSIGNMENT = config.getBoolean("MyPet.Skilltree.RandomAssignment", false);
        Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE = config.getBoolean("MyPet.Skilltree.ChooseOnce", true);
        Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE = config.getBoolean("MyPet.Skilltree.PreventLevellingWithout", true);
        Misc.OWNER_CAN_ATTACK_PET = config.getBoolean("MyPet.OwnerCanAttackPet", false);
        Misc.DISABLE_PET_VS_PLAYER = config.getBoolean("MyPet.DisablePetVersusPlayer", false);
        HungerSystem.USE_HUNGER_SYSTEM = config.getBoolean("MyPet.HungerSystem.Active", true);
        HungerSystem.HUNGER_SYSTEM_TIME = config.getInt("MyPet.HungerSystem.Time", 60);
        HungerSystem.HUNGER_SYSTEM_POINTS_PER_FEED = config.getInt("MyPet.HungerSystem.HungerPointsPerFeed", 6);
        Misc.RELEASE_PETS_ON_DEATH = config.getBoolean("MyPet.ReleasePetsOnDeath", false);
        Misc.REMOVE_PETS_AFTER_RELEASE = config.getBoolean("MyPet.RemovePetsAfterRelease", false);
        Misc.RETAIN_EQUIPMENT_ON_TAME = config.getBoolean("MyPet.RetainEquipmentOnTame", true);
        Misc.INVISIBLE_LIKE_OWNER = config.getBoolean("MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible", true);
        Misc.MYPET_FOLLOW_START_DISTANCE = config.getDouble("MyPet.FollowStartDistance", 7.0D);
        LevelSystem.FIREWORK_COLOR = Integer.decode(config.getString("MyPet.LevelSystem.Firework.Color", "#00FF00"));
        LevelSystem.CALCULATION_MODE = config.getString("MyPet.LevelSystem.CalculationMode", "Default");
        LevelSystem.FIREWORK = config.getBoolean("MyPet.LevelSystem.Firework", true);

        Log.INFO = config.getBoolean("MyPet.Log.INFO", Log.INFO);
        Log.ERROR = config.getBoolean("MyPet.Log.ERROR", Log.ERROR);
        Log.WARNING = config.getBoolean("MyPet.Log.WARNING", Log.WARNING);

        NameFilter.NAME_FILTER = new ArrayList<>();
        for (Object o : config.getList("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"))) {
            NameFilter.NAME_FILTER.add(o.toString());
        }
        Name.MAX_LENGTH = config.getInt("MyPet.Name.MaxLength", 64);
        Name.OVERHEAD_NAME = config.getBoolean("MyPet.Name.OverHead.Visible", true);
        Name.OVERHEAD_PREFIX = Colorizer.setColors(config.getString("MyPet.Name.OverHead.Prefix", "<aqua>"));
        Name.OVERHEAD_SUFFIX = Colorizer.setColors(config.getString("MyPet.Name.OverHead.Suffix", ""));

        Repository.REPOSITORY_TYPE = config.getString("MyPet.Repository.Type", Repository.REPOSITORY_TYPE);
        Repository.CONVERT_FROM = config.getString("MyPet.Repository.ConvertFrom", Repository.CONVERT_FROM);

        Repository.NBT.AUTOSAVE_TIME = config.getInt("MyPet.Repository.NBT.AutoSaveTime", Repository.NBT.AUTOSAVE_TIME);
        Repository.NBT.SAVE_ON_PET_UPDATE = config.getBoolean("MyPet.Repository.NBT.Pet.SaveOnUpdate", Repository.NBT.SAVE_ON_PET_UPDATE);
        Repository.NBT.SAVE_ON_PET_REMOVE = config.getBoolean("MyPet.Repository.NBT.Pet.SaveOnRemove", Repository.NBT.SAVE_ON_PET_REMOVE);
        Repository.NBT.SAVE_ON_PET_ADD = config.getBoolean("MyPet.Repository.NBT.Pet.SaveOnAdd", Repository.NBT.SAVE_ON_PET_ADD);
        Repository.NBT.SAVE_ON_PLAYER_ADD = config.getBoolean("MyPet.Repository.NBT.Player.SaveOnAdd", Repository.NBT.SAVE_ON_PLAYER_ADD);
        Repository.NBT.SAVE_ON_PLAYER_UPDATE = config.getBoolean("MyPet.Repository.NBT.Player.SaveOnUpdate", Repository.NBT.SAVE_ON_PLAYER_UPDATE);
        Repository.NBT.SAVE_ON_PLAYER_REMOVE = config.getBoolean("MyPet.Repository.NBT.Player.SaveOnRemove", Repository.NBT.SAVE_ON_PLAYER_REMOVE);
        Repository.NBT.MAKE_BACKUPS = config.getBoolean("MyPet.Repository.NBT.Backup.Active", Repository.NBT.MAKE_BACKUPS);
        Repository.NBT.SAVE_INTERVAL = config.getInt("MyPet.Repository.NBT.Backup.SaveInterval", Repository.NBT.SAVE_INTERVAL);
        Repository.NBT.DATE_FORMAT = config.getString("MyPet.Repository.NBT.Backup.DateFormat", Repository.NBT.DATE_FORMAT);

        Repository.MySQL.DATABASE = config.getString("MyPet.Repository.MySQL.Database", Repository.MySQL.DATABASE);
        Repository.MySQL.HOST = config.getString("MyPet.Repository.MySQL.Host", Repository.MySQL.HOST);
        Repository.MySQL.PASSWORD = config.getString("MyPet.Repository.MySQL.Password", Repository.MySQL.PASSWORD);
        Repository.MySQL.USER = config.getString("MyPet.Repository.MySQL.User", Repository.MySQL.USER);
        Repository.MySQL.PORT = config.getInt("MyPet.Repository.MySQL.Database", Repository.MySQL.PORT);

        Repository.MongoDB.DATABASE = config.getString("MyPet.Repository.MongoDB.Database", Repository.MongoDB.DATABASE);
        Repository.MongoDB.HOST = config.getString("MyPet.Repository.MongoDB.Host", Repository.MongoDB.HOST);
        Repository.MongoDB.PASSWORD = config.getString("MyPet.Repository.MongoDB.Password", Repository.MongoDB.PASSWORD);
        Repository.MongoDB.USER = config.getString("MyPet.Repository.MongoDB.User", Repository.MongoDB.USER);
        Repository.MongoDB.PORT = config.getInt("MyPet.Repository.MongoDB.Database", Repository.MongoDB.PORT);

        Misc.WIKI_URL = config.getString("MyPet.Info.Wiki-URL", Misc.WIKI_URL);
        PetInfoDisplay.Name.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetName", false);
        PetInfoDisplay.HP.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetHP", false);
        PetInfoDisplay.Damage.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetDamage", false);
        PetInfoDisplay.Hunger.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetHunger", false);
        PetInfoDisplay.Level.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetLevel", true);
        PetInfoDisplay.Exp.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetEXP", true);
        PetInfoDisplay.Owner.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetOwner", true);
        PetInfoDisplay.Skilltree.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetOwner", true);

        Permissions.EXTENDED = config.getBoolean("MyPet.Permissions.UseExtendedPermissions", false);
        Permissions.ENABLED = config.getBoolean("MyPet.Permissions.Enabled", true);

        Hooks.BUNGEE_MODE = config.getString("MyPet.Hooks.BungeeCord.UUID-Mode", "online");
        Hooks.USE_ECONOMY = config.getBoolean("MyPet.Hooks.Vault.Economy", true);
        Hooks.DISABLE_PETS_IN_MINIGAMES = config.getBoolean("MyPet.Hooks.Minigames.DisablePetsInGames", true);
        Hooks.DISABLE_PETS_IN_ARENA = config.getBoolean("MyPet.Hooks.PvPArena.DisablePetsInArena", true);
        Hooks.DISABLE_PETS_IN_SURVIVAL_GAMES = config.getBoolean("MyPet.Hooks.SurvivalGames.DisablePetsInGames", true);
        Hooks.DISABLE_PETS_IN_HUNGER_GAMES = config.getBoolean("MyPet.Hooks.MyHungerGames.DisablePetsInGames", true);
        Hooks.DISABLE_PETS_IN_MOB_ARENA = config.getBoolean("MyPet.Hooks.MobArena.DisablePetsInArena", false);
        Hooks.SkillAPI.GRANT_EXP = config.getBoolean("MyPet.Hooks.SkillAPI.GrantExp", true);
        Hooks.SkillAPI.DISABLE_VANILLA_EXP = config.getBoolean("MyPet.Hooks.SkillAPI.Disable-Vanilla-Exp", false);
        Hooks.SkillAPI.EXP_PERCENT = config.getInt("MyPet.Hooks.SkillAPI.ExpPercent", 100);
        Hooks.USE_PvPArena = config.getBoolean("MyPet.Hooks.PvPArena.PvP", true);
        Hooks.USE_Towny = config.getBoolean("MyPet.Hooks.Towny", true);
        Hooks.USE_Factions = config.getBoolean("MyPet.Hooks.Factions", true);
        Hooks.USE_WorldGuard = config.getBoolean("MyPet.Hooks.WorldGuard", true);
        Hooks.USE_Citizens = config.getBoolean("MyPet.Hooks.Citizens", true);
        Hooks.USE_Heroes = config.getBoolean("MyPet.Hooks.Heroes", true);
        Hooks.USE_McMMO = config.getBoolean("MyPet.Hooks.mcMMO", true);
        Hooks.USE_MobArena = config.getBoolean("MyPet.Hooks.MobArena.PvP", true);
        Hooks.USE_SurvivalGame = config.getBoolean("MyPet.Hooks.SurvivalGames.PvP", true);
        Hooks.USE_Regios = config.getBoolean("MyPet.Hooks.Regios", true);
        Hooks.USE_Residence = config.getBoolean("MyPet.Hooks.Residence", true);
        Hooks.USE_AncientRPG = config.getBoolean("MyPet.Hooks.AncientRPG", true);
        Hooks.USE_GriefPrevention = config.getBoolean("MyPet.Hooks.GriefPrevention", true);
        Hooks.USE_PvPManager = config.getBoolean("MyPet.Hooks.PvPManager", true);

        LevelSystem.Experience.LEVEL_CAP = config.getInt("MyPet.Exp.LevelCap", LevelSystem.Experience.LEVEL_CAP);
        LevelSystem.Experience.LOSS_PERCENT = config.getInt("MyPet.Exp.Loss.Percent", 0);
        LevelSystem.Experience.LOSS_FIXED = config.getDouble("MyPet.Exp.Loss.Fixed", 0.0);
        LevelSystem.Experience.DROP_LOST_EXP = config.getBoolean("MyPet.Exp.Loss.Drop", true);
        LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER = config.getInt("MyPet.Exp.Passive.PercentPerMonster", 25);
        LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP = config.getBoolean("MyPet.Exp.Passive.Always-Grant-Passive-XP", true);
        LevelSystem.Experience.FROM_MONSTER_SPAWNER_MOBS = config.getBoolean("MyPet.Exp.Gain.MonsterSpawner", true);
        LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = config.getBoolean("MyPet.Exp.DamageWeightedExperienceDistribution", true);

        if (config.getStringList("MyPet.exp.active") != null) {
            double min;
            double max;
            for (EntityType entityType : MonsterExperience.mobExp.keySet()) {
                max = config.getDouble("MyPet.Exp.Active." + entityType.getName() + ".Max", 0.);
                min = config.getDouble("MyPet.Exp.Active." + entityType.getName() + ".Min", 0.);
                if (min == max) {
                    MonsterExperience.getMonsterExperience(entityType).setExp(max);
                } else {
                    MonsterExperience.getMonsterExperience(entityType).setMin(min);
                    MonsterExperience.getMonsterExperience(entityType).setMax(max);
                }
            }
        }

        File petConfigFile = new File(MyPetPlugin.getPlugin().getDataFolder().getPath(), "pet-config.yml");
        if (petConfigFile.exists()) {
            YamlConfiguration ymlcnf = new YamlConfiguration();
            try {
                ymlcnf.load(petConfigFile);
                config = ymlcnf;
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
        MyChicken.CAN_LAY_EGGS = config.getBoolean("MyPet.Pets.Chicken.CanLayEggs", true);
        MyCow.CAN_GIVE_MILK = config.getBoolean("MyPet.Pets.Cow.CanGiveMilk", true);
        MySheep.CAN_BE_SHEARED = config.getBoolean("MyPet.Pets.Sheep.CanBeSheared", true);
        MySheep.CAN_REGROW_WOOL = config.getBoolean("MyPet.Pets.Sheep.CanRegrowWool", true);
        MyIronGolem.CAN_THROW_UP = config.getBoolean("MyPet.Pets.IronGolem.CanThrowUp", true);
        MySnowman.FIX_SNOW_TRACK = config.getBoolean("MyPet.Pets.Snowman.FixSnowTrack", true);
        MyChicken.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Chicken.GrowUpItem", "" + Material.POTION.getId()));
        MyCow.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Cow.GrowUpItem", "" + Material.POTION.getId()));
        MyHorse.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Horse.GrowUpItem", "" + Material.BREAD.getId()));
        MyMooshroom.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Mooshroom.GrowUpItem", "" + Material.POTION.getId()));
        MyMooshroom.CAN_GIVE_SOUP = config.getBoolean("MyPet.Pets.Mooshroom.CanGiveStew", false);
        MyOcelot.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Ocelot.GrowUpItem", "" + Material.POTION.getId()));
        MyPig.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Pig.GrowUpItem", "" + Material.POTION.getId()));
        MySheep.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Sheep.GrowUpItem", "" + Material.POTION.getId()));
        MyVillager.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Villager.GrowUpItem", "" + Material.POTION.getId()));
        MyWolf.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Wolf.GrowUpItem", "" + Material.POTION.getId()));
        MyZombie.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Zombie.GrowUpItem", "" + Material.POTION.getId()));
        MyPigZombie.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.PigZombie.GrowUpItem", "" + Material.POTION.getId()));

        MyPet.resetOptions();
        for (MyPetType petType : MyPetType.values()) {
            MyPetInfo pi = petType.getMyPetClass().getAnnotation(MyPetInfo.class);

            MyPet.setStartHP(petType.getMyPetClass(), config.getDouble("MyPet.Pets." + petType.getTypeName() + ".HP", pi.hp()));
            MyPet.setStartSpeed(petType.getMyPetClass(), config.getDouble("MyPet.Pets." + petType.getTypeName() + ".Speed", pi.walkSpeed()));
            if (config.get("MyPet.Pets." + petType.getTypeName() + ".Food") instanceof ArrayList) {
                List<String> foodList = config.getStringList("MyPet.Pets." + petType.getTypeName() + ".Food");
                for (String foodString : foodList) {
                    ConfigItem ci = ConfigItem.createConfigItem(foodString);
                    if (ci.getItem().getType() != Material.AIR) {
                        MyPet.setFood(petType.getMyPetClass(), ci);
                    }
                }
            } else {
                Util.seperateFood(petType.getMyPetClass(), config.getString("MyPet.Pets." + petType.getTypeName() + ".Food", "0"));
            }
            Util.seperateLeashFlags(petType.getMyPetClass(), config.getString("MyPet.Pets." + petType.getTypeName() + ".LeashFlags", Util.linkLeashFlags(pi.leashFlags())));
            MyPet.setCustomRespawnTimeFactor(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFactor", 0));
            MyPet.setCustomRespawnTimeFixed(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFixed", 0));
            MyPet.setLeashItem(petType.getMyPetClass(), ConfigItem.createConfigItem(config.getString("MyPet.Pets." + petType.getTypeName() + ".LeashItem", "" + Material.LEASH.getId())));
        }
    }
}