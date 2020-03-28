/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration.*;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.NameFilter;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigurationLoader {

    public static void setDefault() {
        FileConfiguration config = MyPetApi.getPlugin().getConfig();

        config.options().header("" +
                "#################################################################\n" +
                "           This is the main configuration of MyPet              #\n" +
                "             You can find more info on the wiki:                #\n" +
                "  https://wiki.mypet-plugin.de/setup/configurations/config.yml  #\n" +
                "#################################################################\n");
        config.options().copyHeader(true);

        config.addDefault("MyPet.Update.Check", Update.CHECK);
        config.addDefault("MyPet.Update.Download", Update.DOWNLOAD);
        config.addDefault("MyPet.Update.ReplaceOld", Update.REPLACE_OLD);
        config.addDefault("MyPet.Update.In-Background", Update.ASYNC);
        config.addDefault("MyPet.Update.OP-Notification", Update.SHOW_OP);

        config.addDefault("MyPet.Leash.Consume", Misc.CONSUME_LEASH_ITEM);
        config.addDefault("MyPet.Leash.AllowRanged", Misc.ALLOW_RANGED_LEASHING);
        config.addDefault("MyPet.OwnerCanAttackPet", Misc.OWNER_CAN_ATTACK_PET);
        config.addDefault("MyPet.DisablePetVersusPlayer", Misc.DISABLE_PET_VS_PLAYER);
        config.addDefault("MyPet.RetainEquipmentOnTame", Misc.RETAIN_EQUIPMENT_ON_TAME);
        config.addDefault("MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible", Misc.INVISIBLE_LIKE_OWNER);
        config.addDefault("MyPet.Log.Level", Log.LEVEL);
        config.addDefault("MyPet.Log.Report-Errors", true);
        config.addDefault("MyPet.Log.Unique-ID", SentryErrorReporter.getServerUUID().toString());
        config.addDefault("MyPet.Max-Stored-Pet-Count", Misc.MAX_STORED_PET_COUNT);
        config.addDefault("MyPet.Throw-PlayerMoveEvent-While-Riding", Misc.THROW_PLAYER_MOVE_EVENT_WHILE_RIDING);
        config.addDefault("MyPet.Disable-All-Actionbar-Messages", Misc.DISABLE_ALL_ACTIONBAR_MESSAGES);
        config.addDefault("MyPet.OverwriteLanguages", Misc.OVERWRITE_LANGUAGE);
        config.addDefault("MyPet.Right-Click-Command", Misc.RIGHT_CLICK_COMMAND);

        config.addDefault("MyPet.Entity.Skip-Target-AI-Ticks", Entity.SKIP_TARGET_AI_TICKS);
        config.addDefault("MyPet.Entity.FollowStartDistance", Entity.MYPET_FOLLOW_START_DISTANCE);

        config.addDefault("MyPet.Repository.Type", Repository.REPOSITORY_TYPE);
        config.addDefault("MyPet.Repository.ConvertFrom", Repository.CONVERT_FROM);
        config.addDefault("MyPet.Repository.LoadDelay", Repository.EXTERNAL_LOAD_DELAY);

        config.addDefault("MyPet.Repository.MySQL.Database", Repository.MySQL.DATABASE);
        config.addDefault("MyPet.Repository.MySQL.TablePrefix", Repository.MySQL.PREFIX);
        config.addDefault("MyPet.Repository.MySQL.Host", Repository.MySQL.HOST);
        config.addDefault("MyPet.Repository.MySQL.Password", Repository.MySQL.PASSWORD);
        config.addDefault("MyPet.Repository.MySQL.User", Repository.MySQL.USER);
        config.addDefault("MyPet.Repository.MySQL.Port", Repository.MySQL.PORT);
        config.addDefault("MyPet.Repository.MySQL.MaxConnections", Repository.MySQL.POOL_SIZE);
        config.addDefault("MyPet.Repository.MySQL.CharacterEncoding", Repository.MySQL.CHARACTER_ENCODING);

        config.addDefault("MyPet.Repository.MongoDB.Database", Repository.MongoDB.DATABASE);
        config.addDefault("MyPet.Repository.MongoDB.CollectionPrefix", Repository.MongoDB.PREFIX);
        config.addDefault("MyPet.Repository.MongoDB.Host", Repository.MongoDB.HOST);
        config.addDefault("MyPet.Repository.MongoDB.Password", Repository.MongoDB.PASSWORD);
        config.addDefault("MyPet.Repository.MongoDB.User", Repository.MongoDB.USER);
        config.addDefault("MyPet.Repository.MongoDB.Port", Repository.MongoDB.PORT);

        config.addDefault("MyPet.Respawn.Time.Disabled", Respawn.DISABLE_AUTO_RESPAWN);
        config.addDefault("MyPet.Respawn.Time.Default.Factor", Respawn.TIME_FACTOR);
        config.addDefault("MyPet.Respawn.Time.Player.Factor", Respawn.TIME_PLAYER_FACTOR);
        config.addDefault("MyPet.Respawn.Time.Default.Fixed", Respawn.TIME_FIXED);
        config.addDefault("MyPet.Respawn.Time.Player.Fixed", Respawn.TIME_PLAYER_FIXED);
        config.addDefault("MyPet.Respawn.EconomyCost.Fixed", Respawn.COSTS_FIXED);
        config.addDefault("MyPet.Respawn.EconomyCost.Factor", Respawn.COSTS_FACTOR);

        config.addDefault("MyPet.Permissions.Enabled", Permissions.ENABLED);
        config.addDefault("MyPet.Permissions.Extended", Permissions.EXTENDED);
        config.addDefault("MyPet.Permissions.Legacy", Permissions.LEGACY);

        config.addDefault("MyPet.LevelSystem.CalculationMode", LevelSystem.CALCULATION_MODE);

        config.addDefault("MyPet.HungerSystem.Active", HungerSystem.USE_HUNGER_SYSTEM);
        config.addDefault("MyPet.HungerSystem.Time", HungerSystem.HUNGER_SYSTEM_TIME);
        config.addDefault("MyPet.HungerSystem.SaturationPerFeed", HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED);
        config.addDefault("MyPet.HungerSystem.Affect-Ride-Speed", HungerSystem.AFFECT_RIDE_SPEED);
        config.addDefault("MyPet.HungerSystem.Affect-Beacon-Range", HungerSystem.AFFECT_BEACON_RANGE);

        config.addDefault("MyPet.Skilltree.AutomaticAssignment", Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT);
        config.addDefault("MyPet.Skilltree.RandomAssignment", Skilltree.RANDOM_SKILLTREE_ASSIGNMENT);
        config.addDefault("MyPet.Skilltree.ChooseOnce", Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE);
        config.addDefault("MyPet.Skilltree.PreventLevellingWithout", Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE);
        config.addDefault("MyPet.Skilltree.SwitchFee.Fixed", Skilltree.SWITCH_FEE_FIXED);
        config.addDefault("MyPet.Skilltree.SwitchFee.Percent", Skilltree.SWITCH_FEE_PERCENT);
        config.addDefault("MyPet.Skilltree.SwitchFee.Admin", Skilltree.SWITCH_FEE_ADMIN);

        config.addDefault("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"));
        config.addDefault("MyPet.Name.MaxLength", Name.MAX_LENGTH);
        config.addDefault("MyPet.Name.Tag.Show", Name.Tag.SHOW);
        config.addDefault("MyPet.Name.Tag.Prefix", Name.Tag.PREFIX);
        config.addDefault("MyPet.Name.Tag.Suffix", Name.Tag.SUFFIX);

        config.addDefault("MyPet.Exp.DamageWeightedExperienceDistribution", LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION);
        config.addDefault("MyPet.Exp.Passive.Always-Grant-Passive-XP", LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP);
        config.addDefault("MyPet.Exp.Passive.PercentPerMonster", LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER);
        config.addDefault("MyPet.Exp.Loss.Percent", LevelSystem.Experience.LOSS_PERCENT);
        config.addDefault("MyPet.Exp.Loss.Fixed", LevelSystem.Experience.LOSS_FIXED);
        config.addDefault("MyPet.Exp.Loss.Drop", LevelSystem.Experience.DROP_LOST_EXP);
        config.addDefault("MyPet.Exp.Loss.Allow-Level-Drowngrade", LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE);
        config.addDefault("MyPet.Exp.Gain.PreventFromSpawnReason", new ArrayList<>());
        config.addDefault("MyPet.Exp.LevelCap", LevelSystem.Experience.LEVEL_CAP);
        config.addDefault("MyPet.Exp.Disabled-Worlds", new String[0]);
        config.addDefault("MyPet.Exp.Modifier.Global", LevelSystem.Experience.Modifier.GLOBAL);
        config.addDefault("MyPet.Exp.Modifier.Use-Permissions", LevelSystem.Experience.Modifier.PERMISSION);

        config.addDefault("MyPet.Skill.Control.Item", "lead");
        config.addDefault("MyPet.Skill.Backpack.Creative", Skilltree.Skill.Backpack.OPEN_IN_CREATIVE);
        config.addDefault("MyPet.Skill.Backpack.DropWhenOwnerDies", Skilltree.Skill.Backpack.DROP_WHEN_OWNER_DIES);
        config.addDefault("MyPet.Skill.Beacon.HungerDecreaseTime", Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME);
        config.addDefault("MyPet.Skill.Beacon.Disable-Head-Textures", Skilltree.Skill.Beacon.DISABLE_HEAD_TEXTURE);
        config.addDefault("MyPet.Skill.Beacon.Party-Support", Skilltree.Skill.Beacon.PARTY_SUPPORT);
        config.addDefault("MyPet.Skill.Ride.Item", "lead");
        config.addDefault("MyPet.Skill.Ride.HungerPerMeter", Skilltree.Skill.Ride.HUNGER_PER_METER);
        config.addDefault("MyPet.Skill.Ride.Prevent-Teleportation-While-Riding", Skilltree.Skill.Ride.PREVENT_TELEPORTATION);

        config.addDefault("MyPet.Info.Wiki-URL", Misc.WIKI_URL);


        config.options().copyDefaults(true);
        MyPetApi.getPlugin().saveConfig();

        File expConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "exp-config.yml");
        config = new YamlConfiguration();
        config.options().header("" +
                "#####################################################################\n" +
                "              This is the exp configuration of MyPet                #\n" +
                "                You can find more info on the wiki:                 #\n" +
                "  https://wiki.mypet-plugin.de/setup/configurations/exp-config.yml  #\n" +
                "#####################################################################\n");
        config.options().copyHeader(true);

        if (expConfigFile.exists()) {
            try {
                config.load(expConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            config.addDefault("Custom." + ChatColor.RED + "Big Boss.Max", 300.0);
            config.addDefault("Custom." + ChatColor.RED + "Big Boss.Min", 150.0);
        }

        for (EntityType entityType : EntityType.values()) {
            if (MonsterExperience.mobExp.containsKey(entityType.name())) {
                config.addDefault("Default." + entityType.name() + ".Min", MonsterExperience.getMonsterExperience(entityType).getMin());
                config.addDefault("Default." + entityType.name() + ".Max", MonsterExperience.getMonsterExperience(entityType).getMax());
            }
        }

        config.options().copyDefaults(true);
        try {
            config.save(expConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File petConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "pet-config.yml");
        config = new YamlConfiguration();

        if (petConfigFile.exists()) {
            try {
                config.load(petConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        config.options().header("" +
                "#####################################################################\n" +
                "              This is the pet configuration of MyPet                #\n" +
                "                You can find more info on the wiki:                 #\n" +
                "  https://wiki.mypet-plugin.de/setup/configurations/pet-config.yml  #\n" +
                "#####################################################################\n");
        config.options().copyHeader(true);

        for (MyPetType petType : MyPetType.values()) {
            if (!petType.checkMinecraftVersion()) {
                continue;
            }
            DefaultInfo pi = petType.getMyPetClass().getAnnotation(DefaultInfo.class);
            if (pi == null) {
                continue;
            }

            config.addDefault("MyPet.Pets." + petType.name() + ".HP", pi.hp());
            config.addDefault("MyPet.Pets." + petType.name() + ".Speed", pi.walkSpeed());
            config.addDefault("MyPet.Pets." + petType.name() + ".Food", linkFood(pi.food()));
            config.addDefault("MyPet.Pets." + petType.name() + ".LeashRequirements", pi.leashFlags());
            config.addDefault("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFactor", 0);
            config.addDefault("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFixed", 0);
            config.addDefault("MyPet.Pets." + petType.name() + ".LeashItem", "lead");
            config.addDefault("MyPet.Pets." + petType.name() + ".ReleaseOnDeath", false);
            config.addDefault("MyPet.Pets." + petType.name() + ".RemoveAfterRelease", false);
        }

        config.addDefault("MyPet.Pets.Bat.CanGlide", MyPet.Bat.CAN_GLIDE);
        config.addDefault("MyPet.Pets.Bee.CanGlide", MyPet.Bee.CAN_GLIDE);
        config.addDefault("MyPet.Pets.Blaze.CanGlide", MyPet.Blaze.CAN_GLIDE);
        config.addDefault("MyPet.Pets.Ghast.CanGlide", MyPet.Ghast.CAN_GLIDE);
        config.addDefault("MyPet.Pets.Chicken.CanGlide", MyPet.Chicken.CAN_GLIDE);
        config.addDefault("MyPet.Pets.EnderDragon.CanGlide", MyPet.EnderDragon.CAN_GLIDE);
        config.addDefault("MyPet.Pets.Chicken.CanLayEggs", MyPet.Chicken.CAN_LAY_EGGS);
        config.addDefault("MyPet.Pets.Cow.CanGiveMilk", MyPet.Cow.CAN_GIVE_MILK);
        if (MyPetType.Donkey.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Donkey.GrowUpItem", "experience_bottle");
        }
        config.addDefault("MyPet.Pets.IronGolem.CanTossUp", MyPet.IronGolem.CAN_TOSS_UP);
        config.addDefault("MyPet.Pets.Snowman.FixSnowTrack", MyPet.Snowman.FIX_SNOW_TRACK);
        config.addDefault("MyPet.Pets.Chicken.GrowUpItem", "experience_bottle");
        config.addDefault("MyPet.Pets.Cow.GrowUpItem", "experience_bottle");
        config.addDefault("MyPet.Pets.Horse.GrowUpItem", "bread");
        if (MyPetType.Llama.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Llama.GrowUpItem", "experience_bottle");
        }
        config.addDefault("MyPet.Pets.Mooshroom.GrowUpItem", "experience_bottle");
        config.addDefault("MyPet.Pets.Mooshroom.CanGiveStew", MyPet.Mooshroom.CAN_GIVE_SOUP);
        if (MyPetType.Mule.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Mule.GrowUpItem", "experience_bottle");
        }
        config.addDefault("MyPet.Pets.Ocelot.GrowUpItem", "experience_bottle");
        if (MyPetType.Parrot.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Parrot.CanGlide", MyPet.Parrot.CAN_GLIDE);
        }
        if (MyPetType.Phantom.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Phantom.CanGlide", MyPet.Phantom.CAN_GLIDE);
        }
        config.addDefault("MyPet.Pets.Pig.GrowUpItem", "experience_bottle");
        config.addDefault("MyPet.Pets.PigZombie.GrowUpItem", "experience_bottle");
        if (MyPetType.Rabbit.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Rabbit.GrowUpItem", "experience_bottle");
        }
        config.addDefault("MyPet.Pets.Sheep.CanBeSheared", MyPet.Sheep.CAN_BE_SHEARED);
        config.addDefault("MyPet.Pets.Sheep.CanRegrowWool", MyPet.Sheep.CAN_REGROW_WOOL);
        config.addDefault("MyPet.Pets.Sheep.GrowUpItem", "experience_bottle");
        if (MyPetType.SkeletonHorse.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.SkeletonHorse.GrowUpItem", "experience_bottle");
        }
        if (MyPetType.Vex.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.Vex.CanGlide", MyPet.Vex.CAN_GLIDE);
        }
        config.addDefault("MyPet.Pets.Villager.GrowUpItem", "experience_bottle");
        config.addDefault("MyPet.Pets.Wolf.GrowUpItem", "experience_bottle");
        config.addDefault("MyPet.Pets.Wither.CanGlide", MyPet.Wither.CAN_GLIDE);
        config.addDefault("MyPet.Pets.Zombie.GrowUpItem", "experience_bottle");
        if (MyPetType.ZombieHorse.checkMinecraftVersion()) {
            config.addDefault("MyPet.Pets.ZombieHorse.GrowUpItem", "experience_bottle");
        }

        config.options().copyDefaults(true);
        try {
            config.save(petConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadConfiguration() {
        MyPetApi.getPlugin().reloadConfig();
        FileConfiguration config = MyPetApi.getPlugin().getConfig();

        Misc.CONSUME_LEASH_ITEM = config.getBoolean("MyPet.Leash.Consume", false);
        Misc.ALLOW_RANGED_LEASHING = config.getBoolean("MyPet.Leash.AllowRanged", true);
        Misc.MAX_STORED_PET_COUNT = config.getInt("MyPet.Max-Stored-Pet-Count", Misc.MAX_STORED_PET_COUNT);
        Misc.RIGHT_CLICK_COMMAND = config.getString("MyPet.Right-Click-Command", Misc.RIGHT_CLICK_COMMAND);
        if (Misc.RIGHT_CLICK_COMMAND.startsWith("/")) {
            Misc.RIGHT_CLICK_COMMAND = Misc.RIGHT_CLICK_COMMAND.substring(1);
        }

        Update.ASYNC = config.getBoolean("MyPet.Update.In-Background", Update.ASYNC);
        Update.CHECK = config.getBoolean("MyPet.Update.Check", Update.CHECK);
        Update.DOWNLOAD = config.getBoolean("MyPet.Update.Download", Update.DOWNLOAD);
        Update.REPLACE_OLD = config.getBoolean("MyPet.Update.ReplaceOld", Update.REPLACE_OLD);
        Update.SHOW_OP = config.getBoolean("MyPet.Update.OP-Notification", Update.SHOW_OP);

        Skilltree.Skill.Beacon.HUNGER_DECREASE_TIME = config.getInt("MyPet.Skill.Beacon.HungerDecreaseTime", 100);
        Skilltree.Skill.Beacon.PARTY_SUPPORT = config.getBoolean("MyPet.Skill.Beacon.Party-Support", true);
        Skilltree.Skill.Beacon.DISABLE_HEAD_TEXTURE = config.getBoolean("MyPet.Skill.Beacon.Disable-Head-Textures", false);
        Skilltree.Skill.Backpack.OPEN_IN_CREATIVE = config.getBoolean("MyPet.Skill.Backpack.Creative", true);
        Skilltree.Skill.Backpack.DROP_WHEN_OWNER_DIES = config.getBoolean("MyPet.Skill.Backpack.DropWhenOwnerDies", false);
        Skilltree.Skill.Ride.HUNGER_PER_METER = config.getDouble("MyPet.Skill.Ride.HungerPerMeter", 0.01);
        Skilltree.Skill.Ride.PREVENT_TELEPORTATION = config.getBoolean("MyPet.Skill.Ride.Prevent-Teleportation-While-Riding", false);
        Skilltree.SWITCH_FEE_FIXED = config.getDouble("MyPet.Skilltree.SwitchFee.Fixed", 0.0);
        Skilltree.SWITCH_FEE_PERCENT = config.getInt("MyPet.Skilltree.SwitchFee.Percent", 5);
        Skilltree.SWITCH_FEE_ADMIN = config.getBoolean("MyPet.Skilltree.SwitchFee.Admin", false);
        Respawn.DISABLE_AUTO_RESPAWN = config.getBoolean("MyPet.Respawn.Time.Disabled", false);
        Respawn.TIME_FACTOR = config.getInt("MyPet.Respawn.Time.Default.Factor", 5);
        Respawn.TIME_PLAYER_FACTOR = config.getInt("MyPet.Respawn.Time.Player.Factor", 5);
        Respawn.TIME_FIXED = config.getInt("MyPet.Respawn.Time.Default.Fixed", 0);
        Respawn.TIME_PLAYER_FIXED = config.getInt("MyPet.Respawn.Time.Player.Fixed", 0);
        Respawn.COSTS_FACTOR = config.getDouble("MyPet.Respawn.EconomyCost.Factor", 1.0);
        Respawn.COSTS_FIXED = config.getDouble("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT = config.getBoolean("MyPet.Skilltree.AutomaticAssignment", false);
        Skilltree.RANDOM_SKILLTREE_ASSIGNMENT = config.getBoolean("MyPet.Skilltree.RandomAssignment", false);
        Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE = config.getBoolean("MyPet.Skilltree.ChooseOnce", false);
        Skilltree.PREVENT_LEVELLING_WITHOUT_SKILLTREE = config.getBoolean("MyPet.Skilltree.PreventLevellingWithout", true);
        Misc.OWNER_CAN_ATTACK_PET = config.getBoolean("MyPet.OwnerCanAttackPet", false);
        Misc.DISABLE_PET_VS_PLAYER = config.getBoolean("MyPet.DisablePetVersusPlayer", false);
        HungerSystem.USE_HUNGER_SYSTEM = config.getBoolean("MyPet.HungerSystem.Active", true);
        HungerSystem.HUNGER_SYSTEM_TIME = config.getInt("MyPet.HungerSystem.Time", 60);
        HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED = config.getDouble("MyPet.HungerSystem.SaturationPerFeed", 6.0);
        HungerSystem.AFFECT_RIDE_SPEED = config.getBoolean("MyPet.HungerSystem.Affect-Ride-Speed", true);
        HungerSystem.AFFECT_BEACON_RANGE = config.getBoolean("MyPet.HungerSystem.Affect-Beacon-Range", true);
        Misc.RETAIN_EQUIPMENT_ON_TAME = config.getBoolean("MyPet.RetainEquipmentOnTame", true);
        Misc.INVISIBLE_LIKE_OWNER = config.getBoolean("MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible", true);
        Misc.THROW_PLAYER_MOVE_EVENT_WHILE_RIDING = config.getBoolean("MyPet.Throw-PlayerMoveEvent-While-Riding", true);
        Misc.DISABLE_ALL_ACTIONBAR_MESSAGES = config.getBoolean("MyPet.Disable-All-Actionbar-Messages", false);
        Misc.OVERWRITE_LANGUAGE = config.getString("MyPet.OverwriteLanguages", "");
        LevelSystem.CALCULATION_MODE = config.getString("MyPet.LevelSystem.CalculationMode", "Default");
        Entity.MYPET_FOLLOW_START_DISTANCE = config.getDouble("MyPet.Entity.FollowStartDistance", 7.0D);
        Entity.SKIP_TARGET_AI_TICKS = config.getInt("MyPet.Entity.Skip-Target-AI-Ticks", 0);

        Log.LEVEL = config.getString("MyPet.Log.Level", Log.LEVEL);

        NameFilter.NAME_FILTER.clear();
        for (Object o : config.getList("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"))) {
            NameFilter.NAME_FILTER.add(String.valueOf(o));
        }
        Name.MAX_LENGTH = config.getInt("MyPet.Name.MaxLength", Name.MAX_LENGTH);
        Name.Tag.SHOW = config.getBoolean("MyPet.Name.Tag.Show", Name.Tag.SHOW);
        Name.Tag.PREFIX = Colorizer.setColors(config.getString("MyPet.Name.Tag.Prefix", Name.Tag.PREFIX));
        Name.Tag.SUFFIX = Colorizer.setColors(config.getString("MyPet.Name.Tag.Suffix", Name.Tag.SUFFIX));

        Repository.REPOSITORY_TYPE = config.getString("MyPet.Repository.Type", Repository.REPOSITORY_TYPE);
        Repository.CONVERT_FROM = config.getString("MyPet.Repository.ConvertFrom", Repository.CONVERT_FROM);
        Repository.EXTERNAL_LOAD_DELAY = config.getLong("MyPet.Repository.LoadDelay", Repository.EXTERNAL_LOAD_DELAY);

        Repository.MySQL.DATABASE = config.getString("MyPet.Repository.MySQL.Database", Repository.MySQL.DATABASE);
        Repository.MySQL.PREFIX = config.getString("MyPet.Repository.MySQL.TablePrefix", Repository.MySQL.PREFIX);
        Repository.MySQL.HOST = config.getString("MyPet.Repository.MySQL.Host", Repository.MySQL.HOST);
        Repository.MySQL.PASSWORD = config.getString("MyPet.Repository.MySQL.Password", Repository.MySQL.PASSWORD);
        Repository.MySQL.USER = config.getString("MyPet.Repository.MySQL.User", Repository.MySQL.USER);
        Repository.MySQL.PORT = config.getInt("MyPet.Repository.MySQL.Port", Repository.MySQL.PORT);
        Repository.MySQL.POOL_SIZE = config.getInt("MyPet.Repository.MySQL.MaxConnections", Repository.MySQL.POOL_SIZE);
        Repository.MySQL.CHARACTER_ENCODING = config.getString("MyPet.Repository.MySQL.CharacterEncoding", Repository.MySQL.CHARACTER_ENCODING);

        Repository.MongoDB.DATABASE = config.getString("MyPet.Repository.MongoDB.Database", Repository.MongoDB.DATABASE);
        Repository.MongoDB.PREFIX = config.getString("MyPet.Repository.MongoDB.CollectionPrefix", Repository.MongoDB.PREFIX);
        Repository.MongoDB.HOST = config.getString("MyPet.Repository.MongoDB.Host", Repository.MongoDB.HOST);
        Repository.MongoDB.PASSWORD = config.getString("MyPet.Repository.MongoDB.Password", Repository.MongoDB.PASSWORD);
        Repository.MongoDB.USER = config.getString("MyPet.Repository.MongoDB.User", Repository.MongoDB.USER);
        Repository.MongoDB.PORT = config.getInt("MyPet.Repository.MongoDB.Port", Repository.MongoDB.PORT);

        Misc.WIKI_URL = config.getString("MyPet.Info.Wiki-URL", Misc.WIKI_URL);

        Permissions.EXTENDED = config.getBoolean("MyPet.Permissions.Extended", false);
        Permissions.ENABLED = config.getBoolean("MyPet.Permissions.Enabled", true);
        Permissions.LEGACY = config.getBoolean("MyPet.Permissions.Legacy", Permissions.LEGACY);

        LevelSystem.Experience.LEVEL_CAP = config.getInt("MyPet.Exp.LevelCap", LevelSystem.Experience.LEVEL_CAP);
        LevelSystem.Experience.LOSS_PERCENT = config.getInt("MyPet.Exp.Loss.Percent", 0);
        LevelSystem.Experience.LOSS_FIXED = config.getDouble("MyPet.Exp.Loss.Fixed", 0.0);
        LevelSystem.Experience.DROP_LOST_EXP = config.getBoolean("MyPet.Exp.Loss.Drop", true);
        LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE = config.getBoolean("MyPet.Exp.Loss.Allow-Level-Drowngrade", false);
        LevelSystem.Experience.PASSIVE_PERCENT_PER_MONSTER = config.getInt("MyPet.Exp.Passive.PercentPerMonster", 25);
        LevelSystem.Experience.ALWAYS_GRANT_PASSIVE_XP = config.getBoolean("MyPet.Exp.Passive.Always-Grant-Passive-XP", true);
        LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = config.getBoolean("MyPet.Exp.DamageWeightedExperienceDistribution", true);
        LevelSystem.Experience.DISABLED_WORLDS.clear();
        LevelSystem.Experience.DISABLED_WORLDS.addAll(config.getStringList("MyPet.Exp.Disabled-Worlds"));
        LevelSystem.Experience.Modifier.GLOBAL = config.getDouble("MyPet.Exp.Modifier.Global", 1D);
        LevelSystem.Experience.Modifier.PERMISSION = config.getBoolean("MyPet.Exp.Modifier.Use-Permissions", false);

        if (config.contains("MyPet.Exp.Gain.PreventFromSpawnReason")) {
            LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.clear();
            if (config.isList("MyPet.Exp.Gain.PreventFromSpawnReason")) {
                for (String reason : config.getStringList("MyPet.Exp.Gain.PreventFromSpawnReason")) {
                    reason = reason.toUpperCase();
                    try {
                        CreatureSpawnEvent.SpawnReason.valueOf(reason);
                        LevelSystem.Experience.PREVENT_FROM_SPAWN_REASON.add(reason);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        File expConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath(), "exp-config.yml");
        if (expConfigFile.exists()) {
            YamlConfiguration ymlcnf = new YamlConfiguration();
            try {
                ymlcnf.load(expConfigFile);
                config = ymlcnf;
            } catch (IOException | InvalidConfigurationException e) {
                MyPetApi.getLogger().warning("There was an error while loading exp-config.yml");
            }
        }

        for (EntityType entityType : EntityType.values()) {
            if (MonsterExperience.mobExp.containsKey(entityType.name())) {
                double max = config.getDouble("Default." + entityType.name() + ".Max", 0.);
                double min = config.getDouble("Default." + entityType.name() + ".Min", 0.);
                if (min == max) {
                    MonsterExperience.getMonsterExperience(entityType.name()).setExp(max);
                } else {
                    MonsterExperience.getMonsterExperience(entityType).setMin(min);
                    MonsterExperience.getMonsterExperience(entityType).setMax(max);
                }
            }
        }
        ConfigurationSection customExpSection = config.getConfigurationSection("Custom");
        MonsterExperience.CUSTOM_MOB_EXP.clear();
        if (customExpSection != null) {
            for (String name : customExpSection.getKeys(false)) {
                MonsterExperience exp = new MonsterExperience(0, 0, name);
                double max = config.getDouble("Custom." + name + ".Max", 0.);
                double min = config.getDouble("Custom." + name + ".Min", 0.);
                if (min == max) {
                    exp.setExp(max);
                } else {
                    exp.setMin(min);
                    exp.setMax(max);
                }
                MonsterExperience.addCustomExperience(exp);
            }
        }

        File petConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath(), "pet-config.yml");
        if (petConfigFile.exists()) {
            YamlConfiguration ymlcnf = new YamlConfiguration();
            try {
                ymlcnf.load(petConfigFile);
                config = ymlcnf;
            } catch (IOException | InvalidConfigurationException e) {
                MyPetApi.getLogger().warning("There was an error while loading pet-config.yml");
            }
        }
        MyPet.Chicken.CAN_LAY_EGGS = config.getBoolean("MyPet.Pets.Chicken.CanLayEggs", true);
        MyPet.Cow.CAN_GIVE_MILK = config.getBoolean("MyPet.Pets.Cow.CanGiveMilk", true);
        MyPet.Sheep.CAN_BE_SHEARED = config.getBoolean("MyPet.Pets.Sheep.CanBeSheared", true);
        MyPet.Sheep.CAN_REGROW_WOOL = config.getBoolean("MyPet.Pets.Sheep.CanRegrowWool", true);
        MyPet.IronGolem.CAN_TOSS_UP = config.getBoolean("MyPet.Pets.IronGolem.CanTossUp", true);
        MyPet.Snowman.FIX_SNOW_TRACK = config.getBoolean("MyPet.Pets.Snowman.FixSnowTrack", true);
        MyPet.Mooshroom.CAN_GIVE_SOUP = config.getBoolean("MyPet.Pets.Mooshroom.CanGiveStew", false);
        MyPet.Bee.CAN_GLIDE = config.getBoolean("MyPet.Pets.Bee.CanGlide", true);
        MyPet.Bat.CAN_GLIDE = config.getBoolean("MyPet.Pets.Bat.CanGlide", true);
        MyPet.Blaze.CAN_GLIDE = config.getBoolean("MyPet.Pets.Blaze.CanGlide", true);
        MyPet.Ghast.CAN_GLIDE = config.getBoolean("MyPet.Pets.Ghast.CanGlide", true);
        MyPet.Chicken.CAN_GLIDE = config.getBoolean("MyPet.Pets.Chicken.CanGlide", true);
        MyPet.EnderDragon.CAN_GLIDE = config.getBoolean("MyPet.Pets.EnderDragon.CanGlide", true);
        MyPet.Wither.CAN_GLIDE = config.getBoolean("MyPet.Pets.Wither.CanGlide", true);
        MyPet.Vex.CAN_GLIDE = config.getBoolean("MyPet.Pets.Vex.CanGlide", true);
        MyPet.Parrot.CAN_GLIDE = config.getBoolean("MyPet.Pets.Parrot.CanGlide", true);
        MyPet.Phantom.CAN_GLIDE = config.getBoolean("MyPet.Pets.Phantom.CanGlide", true);
    }

    public static void loadCompatConfiguration() {
        FileConfiguration config = MyPetApi.getPlugin().getConfig();

        Skilltree.Skill.CONTROL_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Skill.Control.Item", "lead"));
        Skilltree.Skill.Ride.RIDE_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Skill.Ride.Item", "lead"));

        File petConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath(), "pet-config.yml");
        if (petConfigFile.exists()) {
            YamlConfiguration ymlcnf = new YamlConfiguration();
            try {
                ymlcnf.load(petConfigFile);
                config = ymlcnf;
            } catch (IOException | InvalidConfigurationException e) {
                MyPetApi.getLogger().warning("There was an error while loading pet-config.yml");
            }
        }

        MyPet.Chicken.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Chicken.GrowUpItem", "experience_bottle"));
        MyPet.Cow.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Cow.GrowUpItem", "experience_bottle"));
        MyPet.Horse.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Horse.GrowUpItem", "" + "bread"));
        MyPet.Llama.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Llama.GrowUpItem", "experience_bottle"));
        MyPet.Mooshroom.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Mooshroom.GrowUpItem", "experience_bottle"));
        MyPet.Ocelot.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Ocelot.GrowUpItem", "experience_bottle"));
        MyPet.Pig.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Pig.GrowUpItem", "experience_bottle"));
        MyPet.Sheep.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Sheep.GrowUpItem", "experience_bottle"));
        MyPet.Villager.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Villager.GrowUpItem", "experience_bottle"));
        MyPet.Wolf.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Wolf.GrowUpItem", "experience_bottle"));
        MyPet.Zombie.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Zombie.GrowUpItem", "experience_bottle"));
        MyPet.PigZombie.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.PigZombie.GrowUpItem", "experience_bottle"));
        MyPet.Rabbit.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Rabbit.GrowUpItem", "experience_bottle"));
        MyPet.ZombieHorse.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.ZombieHorse.GrowUpItem", "experience_bottle"));
        MyPet.SkeletonHorse.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.SkeletonHorse.GrowUpItem", "experience_bottle"));
        MyPet.Mule.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Mule.GrowUpItem", "experience_bottle"));
        MyPet.Donkey.GROW_UP_ITEM = ConfigItem.createConfigItem(config.getString("MyPet.Pets.Donkey.GrowUpItem", "experience_bottle"));

        for (MyPetType petType : MyPetType.values()) {
            if (!petType.checkMinecraftVersion()) {
                continue;
            }
            DefaultInfo pi = petType.getMyPetClass().getAnnotation(DefaultInfo.class);
            if (pi == null) {
                continue;
            }

            MyPetApi.getMyPetInfo().setStartHP(petType, config.getDouble("MyPet.Pets." + petType.name() + ".HP", pi.hp()));
            MyPetApi.getMyPetInfo().setSpeed(petType, config.getDouble("MyPet.Pets." + petType.name() + ".Speed", pi.walkSpeed()));
            MyPetApi.getMyPetInfo().clearFood(petType);
            if (config.get("MyPet.Pets." + petType.name() + ".Food") instanceof ArrayList) {
                List<String> foodList = config.getStringList("MyPet.Pets." + petType.name() + ".Food");
                for (String foodString : foodList) {
                    ConfigItem ci = ConfigItem.createConfigItem(foodString);
                    if (ci.getItem() != null && ci.getItem().getType() != Material.AIR) {
                        MyPetApi.getMyPetInfo().addFood(petType, ci);
                    }
                }
            }
            loadLeashFlags(petType, config.getStringList("MyPet.Pets." + petType + ".LeashRequirements"));
            MyPetApi.getMyPetInfo().setCustomRespawnTimeFactor(petType, config.getInt("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFactor", 0));
            MyPetApi.getMyPetInfo().setCustomRespawnTimeFixed(petType, config.getInt("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFixed", 0));
            MyPetApi.getMyPetInfo().setReleaseOnDeath(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".ReleaseOnDeath", false));
            MyPetApi.getMyPetInfo().setRemoveAfterRelease(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".RemoveAfterRelease", false));
            MyPetApi.getMyPetInfo().setLeashItem(petType, ConfigItem.createConfigItem(config.getString("MyPet.Pets." + petType.name() + ".LeashItem", "lead")));
        }
    }

    public static void upgradeConfig() {
        FileConfiguration config = MyPetApi.getPlugin().getConfig();

        if (config.contains("MyPet.Skilltree.SwitchPenalty.Fixed")) {
            Skilltree.SWITCH_FEE_FIXED = config.getDouble("MyPet.Skilltree.SwitchPenalty.Fixed", 0.0);
            Skilltree.SWITCH_FEE_PERCENT = config.getInt("MyPet.Skilltree.SwitchPenalty.Percent", 5);
            Skilltree.SWITCH_FEE_ADMIN = config.getBoolean("MyPet.Skilltree.SwitchPenalty.Admin", false);
            config.getConfigurationSection("MyPet.Skilltree").set("SwitchPenalty", null);
        }
        if (config.contains("MyPet.Name.OverHead")) {
            Name.Tag.SHOW = config.getBoolean("MyPet.Name.OverHead.Visible", Name.Tag.SHOW);
            Name.Tag.PREFIX = config.getString("MyPet.Name.OverHead.Prefix", Name.Tag.PREFIX);
            Name.Tag.SUFFIX = config.getString("MyPet.Name.OverHead.Suffix", Name.Tag.SUFFIX);
            config.getConfigurationSection("MyPet.Name").set("OverHead", null);
        }
        if (config.contains("MyPet.Update-Check")) {
            Update.CHECK = config.getBoolean("MyPet.Update-Check", Update.CHECK);
            config.getConfigurationSection("MyPet").set("Update-Check", null);
        }
        if (config.contains("MyPet.Activate-Resourcepack-By-Default")) {
            config.getConfigurationSection("MyPet").set("Activate-Resourcepack-By-Default", null);
        }
        if (config.contains("MyPet.HungerSystem.HungerPointsPerFeed")) {
            HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED = config.getDouble("MyPet.HungerSystem.HungerPointsPerFeed", HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED);
            config.getConfigurationSection("MyPet.HungerSystem").set("HungerPointsPerFeed", null);
        }
        if (config.contains("MyPet.Backup")) {
            config.getConfigurationSection("MyPet").set("Backup", null);
        }
        if (config.contains("MyPet.Skill.Inventory.Creative")) {
            Skilltree.Skill.Backpack.OPEN_IN_CREATIVE = config.getBoolean("MyPet.Skill.Inventory.Creative", true);
            Skilltree.Skill.Backpack.DROP_WHEN_OWNER_DIES = config.getBoolean("MyPet.Skill.Inventory.DropWhenOwnerDies", false);
            config.getConfigurationSection("MyPet.Skill").set("Inventory", null);
        }
        if (config.contains("MyPet.Exp.Active")) {
            for (EntityType entityType : EntityType.values()) {
                if (MonsterExperience.mobExp.containsKey(entityType.name())) {
                    double max = config.getDouble("MyPet.Exp.Active." + entityType.name() + ".Max", 0.);
                    double min = config.getDouble("MyPet.Exp.Active." + entityType.name() + ".Min", 0.);
                    if (min == max) {
                        MonsterExperience.getMonsterExperience(entityType).setExp(max);
                    } else {
                        MonsterExperience.getMonsterExperience(entityType).setMin(min);
                        MonsterExperience.getMonsterExperience(entityType).setMax(max);
                    }
                }
            }
            config.getConfigurationSection("MyPet.Exp").set("Active", null);
        }
        if (config.contains("MyPet.Hooks.PvPManager.Enabled")) {
            config.getConfigurationSection("MyPet.Hooks").set("PvPManager", null);
        }
        if (config.contains("MyPet.Skill.Ride.FlyZones")) {
            config.getConfigurationSection("MyPet.Skill.Ride").set("FlyZones", null);
        }
        if (config.contains("MyPet.Hooks")) {
            MyPetApi.getLogger().warning("The config for all MyPet hooks moved to hooks-config.yml. All settings have been reset!");
            config.getConfigurationSection("MyPet").set("Hooks", null);
        }
        if (config.contains("MyPet.Update.Token")) {
            config.getConfigurationSection("MyPet.Update").set("Token", null);
        }
        if (config.contains("MyPet.FollowStartDistance")) {
            Entity.MYPET_FOLLOW_START_DISTANCE = config.getDouble("MyPet.FollowStartDistance", 7.0D);
            config.getConfigurationSection("MyPet").set("FollowStartDistance", null);
        }
        if (config.contains("MyPet.Entity.Skip-Movement-AI-Ticks")) {
            config.getConfigurationSection("MyPet.Entity").set("Skip-Movement-AI-Ticks", null);
        }

        MyPetApi.getPlugin().saveConfig();

        File petConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "pet-config.yml");
        FileConfiguration petConfig = new YamlConfiguration();
        if (petConfigFile.exists()) {
            try {
                petConfig.load(petConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        if (petConfig != null && config.contains("MyPet.RemovePetsAfterRelease")) {
            boolean removePetsAfterRelease = config.getBoolean("MyPet.RemovePetsAfterRelease", false);
            boolean releasePetsOnDeath = config.getBoolean("MyPet.ReleasePetsOnDeath", false);
            config.getConfigurationSection("MyPet").set("RemovePetsAfterRelease", null);
            config.getConfigurationSection("MyPet").set("ReleasePetsOnDeath", null);
            MyPetApi.getPlugin().saveConfig();

            for (MyPetType petType : MyPetType.values()) {
                if (!petType.checkMinecraftVersion()) {
                    continue;
                }
                DefaultInfo pi = petType.getMyPetClass().getAnnotation(DefaultInfo.class);
                if (pi == null) {
                    continue;
                }

                petConfig.set("MyPet.Pets." + petType.name() + ".ReleaseOnDeath", releasePetsOnDeath);
                petConfig.set("MyPet.Pets." + petType.name() + ".RemoveAfterRelease", removePetsAfterRelease);
            }
        }

        for (MyPetType petType : MyPetType.values()) {
            if (petConfig.contains("MyPet.Pets." + petType.name() + ".LeashFlags")) {
                String[] flagString = petConfig.getString("MyPet.Pets." + petType.name() + ".LeashFlags").split(",");
                Set<String> flags = new HashSet<>(Arrays.asList(flagString));
                flags.remove("None");
                petConfig.set("MyPet.Pets." + petType.name() + ".LeashRequirements", flags.toArray(new String[0]));
                petConfig.getConfigurationSection("MyPet.Pets." + petType).set("LeashFlags", null);
            }
        }
        if (petConfig.contains("MyPet.Pets.IronGolem.CanThrowUp")) {
            MyPet.IronGolem.CAN_TOSS_UP = petConfig.getBoolean("MyPet.Pets.IronGolem.CanThrowUp");
            petConfig.getConfigurationSection("MyPet.Pets.IronGolem").set("CanThrowUp", null);
        }

        try {
            petConfig.save(petConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> linkFood(String[] foodTypes) {
        return new ArrayList<>(Arrays.asList(foodTypes));
    }

    public static void loadLeashFlags(MyPetType type, List<String> leashFlagStrings) {
        MyPetApi.getMyPetInfo().clearLeashFlagSettings(type);
        for (String leashFlagString : leashFlagStrings) {
            boolean hasParameter = leashFlagString.contains(":");
            String[] data = leashFlagString.split(":", 2);
            Settings settings = new Settings(data[0]);
            if (hasParameter) {
                settings.load(data[1]);
            }
            MyPetApi.getMyPetInfo().addLeashFlagSetting(type, settings);
        }
    }
}
