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

package de.Keyle.MyPet.util;

import com.google.common.collect.Lists;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration.*;
import de.Keyle.MyPet.api.Configuration.MyPet;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
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

        config.options().header("""
                \
                #################################################################
                           This is the main configuration of MyPet              #
                             You can find more info on the wiki:                #
                  https://wiki.mypet-plugin.de/setup/configurations/config.yml  #
                #################################################################
                """);
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
        config.addDefault("MyPet.PetKillsGivePlayerRewards", Misc.PET_KILLS_GIVE_PLAYER_REWARDS);
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
        config.addDefault("MyPet.Recall-Pet-After-Despawn", Misc.RECALL_PET_AFTER_DESPAWN);

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


        config.addDefault("MyPet.Respawn.Time.Disabled", Respawn.DISABLE_AUTO_RESPAWN);
        config.addDefault("MyPet.Respawn.Time.Default.Factor", Respawn.TIME_FACTOR);
        config.addDefault("MyPet.Respawn.Time.Player.Factor", Respawn.TIME_PLAYER_FACTOR);
        config.addDefault("MyPet.Respawn.Time.Default.Fixed", Respawn.TIME_FIXED);
        config.addDefault("MyPet.Respawn.Time.Player.Fixed", Respawn.TIME_PLAYER_FIXED);
        config.addDefault("MyPet.Respawn.EconomyCost.Fixed", Respawn.COSTS_FIXED);
        config.addDefault("MyPet.Respawn.EconomyCost.Factor", Respawn.COSTS_FACTOR);

        config.addDefault("MyPet.Permissions.Extended", Permissions.EXTENDED);

        config.addDefault("MyPet.LevelSystem.CalculationMode", LevelSystem.CALCULATION_MODE);

        config.addDefault("MyPet.HungerSystem.Active", HungerSystem.USE_HUNGER_SYSTEM);
        config.addDefault("MyPet.HungerSystem.Time", HungerSystem.HUNGER_SYSTEM_TIME);
        config.addDefault("MyPet.HungerSystem.SaturationPerFeed", HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED);
        config.addDefault("MyPet.HungerSystem.Affect-Ride-Speed", HungerSystem.AFFECT_RIDE_SPEED);
        config.addDefault("MyPet.HungerSystem.Affect-Beacon-Range", HungerSystem.AFFECT_BEACON_RANGE);
        config.addDefault("MyPet.HungerSystem.Feed-From-Inventory", HungerSystem.FEED_FROM_INVENTORY);
        config.addDefault("MyPet.HungerSystem.Damage.Fixed", HungerSystem.HUNGER_SYSTEM_FIXED);
        config.addDefault("MyPet.HungerSystem.Damage.Factor", HungerSystem.HUNGER_SYSTEM_FACTOR);
        config.addDefault("MyPet.HungerSystem.Damage.Time-Before-Damage", HungerSystem.HUNGER_SYSTEM_TIME_BEFORE_DAMAGE);
        config.addDefault("MyPet.HungerSystem.Damage.can-kill", HungerSystem.HUNGER_SYSTEM_CAN_KILL);

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
        config.options().header("""
                \
                #####################################################################
                              This is the exp configuration of MyPet                #
                                You can find more info on the wiki:                 #
                  https://wiki.mypet-plugin.de/setup/configurations/exp-config.yml  #
                #####################################################################
                """);
        config.options().copyHeader(true);

        if (expConfigFile.exists()) {
            try {
                config.load(expConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                ErrorUtil.reportError("ConfigurationLoader operation failed", e);
            }
        } else {
            config.addDefault("Custom.<red>Big Boss.Max", 300.0);
            config.addDefault("Custom.<red>Big Boss.Min", 150.0);
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
            ErrorUtil.reportError("ConfigurationLoader operation failed", e);
        }

        File petConfigFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "pet-config.yml");
        config = new YamlConfiguration();

        if (petConfigFile.exists()) {
            try {
                config.load(petConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                ErrorUtil.reportError("ConfigurationLoader operation failed", e);
            }
        }

        config.options().header("""
                \
                #####################################################################
                              This is the pet configuration of MyPet                #
                                You can find more info on the wiki:                 #
                  https://wiki.mypet-plugin.de/setup/configurations/pet-config.yml  #
                #####################################################################
                """);
        config.options().copyHeader(true);

        for (PetType petType : PetType.values()) {
            if (!petType.checkMinecraftVersion()) {
                continue;
            }
            DefaultInfo pi = petType.getPetClass().getAnnotation(DefaultInfo.class);
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
            if (PetBaby.class.isAssignableFrom(petType.getPetClass())) {
                config.addDefault("MyPet.Pets." + petType.name() + ".GrowUpItem", pi.growUpItem().name().toLowerCase());
            }
        }


        // Dynamic per-type CanFly / CanSwim / AllowZombification /
        // PreventDaylightBurn / PreventSuffocation rows. Adding a new flying,
        // swimming, zombifiable, sun-sensitive, or water-breathing pet
        // requires no edit here — implement the appropriate marker interface
        // and the YAML row appears. Migration of pre-4.x configs (single
        // CanGlide key on flying pets) is handled by
        // MigrateFlyingPetsCanGlideToCanFly.
        for (PetType type : PetType.values()) {
            String base = "MyPet.Pets." + type.name();
            if (PetFlyingEntity.class.isAssignableFrom(type.getPetClass())) {
                config.addDefault(base + ".CanFly", true);
            }
            if (PetSwimmingEntity.class.isAssignableFrom(type.getPetClass())) {
                config.addDefault(base + ".CanSwim", true);
            }
            if (PetZombifiable.class.isAssignableFrom(type.getPetClass())) {
                config.addDefault(base + ".AllowZombification", false);
            }
            if (PetSunSensitive.class.isAssignableFrom(type.getPetClass())) {
                config.addDefault(base + ".PreventDaylightBurn", true);
            }
            if (PetAquaticEntity.class.isAssignableFrom(type.getPetClass())) {
                config.addDefault(base + ".PreventSuffocation", true);
            }
        }
        config.addDefault("MyPet.Pets.Armadillo.CanShedScute", MyPet.Armadillo.CAN_SHED_SCUTE);
        config.addDefault("MyPet.Pets.Chicken.CanLayEggs", MyPet.Chicken.CAN_LAY_EGGS);
        if (PetType.byNameOrNull("CopperGolem") != null) {
            config.addDefault("MyPet.Pets.CopperGolem.CanOxidize", true);
        }
        config.addDefault("MyPet.Pets.Cow.CanGiveMilk", MyPet.Cow.CAN_GIVE_MILK);
        config.addDefault("MyPet.Pets.Creeper.AllowFlintAndSteelExplode", MyPet.Creeper.ALLOW_FLINT_AND_STEEL_EXPLODE);
        config.addDefault("MyPet.Pets.Creeper.AllowNonOwnerFlintAndSteel", MyPet.Creeper.ALLOW_NON_OWNER_FLINT_AND_STEEL);
        config.addDefault("MyPet.Pets.Creeper.AllowExplosionBlockDamage", MyPet.Creeper.ALLOW_EXPLOSION_BLOCK_DAMAGE);
        config.addDefault("MyPet.Pets.Creeper.AllowExplosionEntityDamage", MyPet.Creeper.ALLOW_EXPLOSION_ENTITY_DAMAGE);
        config.addDefault("MyPet.Pets.EnderDragon.GrantEndAdvancementOnKill", MyPet.EnderDragon.GRANT_END_ADVANCEMENT_ON_KILL);
        config.addDefault("MyPet.Pets.EnderDragon.AllowBlockDamage", MyPet.EnderDragon.ALLOW_BLOCK_DAMAGE);
        config.addDefault("MyPet.Pets.EnderDragon.AllowPlayerContactDamage", MyPet.EnderDragon.ALLOW_PLAYER_CONTACT_DAMAGE);
        config.addDefault("MyPet.Pets.EnderDragon.AllowEntityContactDamage", MyPet.EnderDragon.ALLOW_ENTITY_CONTACT_DAMAGE);
        config.addDefault("MyPet.Pets.Goat.CanDropHorn", MyPet.Goat.CAN_DROP_HORN);
        config.addDefault("MyPet.Pets.Goat.CanGiveMilk", MyPet.Goat.CAN_GIVE_MILK);
        config.addDefault("MyPet.Pets.IronGolem.CanTossUp", MyPet.IronGolem.CAN_TOSS_UP);
        config.addDefault("MyPet.Pets.SnowGolem.DisableSnowTrack", MyPet.SnowGolem.DISABLE_SNOW_TRACK);
        config.addDefault("MyPet.Pets.MagmaCube.CanHurtPlayersOnContact", MyPet.MagmaCube.CAN_HURT_PLAYERS_ON_CONTACT);
        config.addDefault("MyPet.Pets.Mooshroom.CanGiveStew", MyPet.Mooshroom.CAN_GIVE_SOUP);
        config.addDefault("MyPet.Pets.Panda.CanDropSlimeball", MyPet.Panda.CAN_DROP_SLIMEBALL);
        config.addDefault("MyPet.Pets.Sheep.CanBeSheared", MyPet.Sheep.CAN_BE_SHEARED);
        config.addDefault("MyPet.Pets.Sheep.CanRegrowWool", MyPet.Sheep.CAN_REGROW_WOOL);
        config.addDefault("MyPet.Pets.Slime.CanHurtPlayersOnContact", MyPet.Slime.CAN_HURT_PLAYERS_ON_CONTACT);
        config.addDefault("MyPet.Pets.Sniffer.CanDigSeeds", MyPet.Sniffer.CAN_DIG_SEEDS);

        config.options().copyDefaults(true);
        try {
            config.save(petConfigFile);
        } catch (IOException e) {
            ErrorUtil.reportError("ConfigurationLoader operation failed", e);
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
        Misc.RECALL_PET_AFTER_DESPAWN = config.getBoolean("MyPet.Recall-Pet-After-Despawn", Misc.RECALL_PET_AFTER_DESPAWN);

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
        Misc.PET_KILLS_GIVE_PLAYER_REWARDS = config.getBoolean("MyPet.PetKillsGivePlayerRewards", true);
        HungerSystem.USE_HUNGER_SYSTEM = config.getBoolean("MyPet.HungerSystem.Active", true);
        HungerSystem.HUNGER_SYSTEM_TIME = config.getInt("MyPet.HungerSystem.Time", 60);
        HungerSystem.HUNGER_SYSTEM_SATURATION_PER_FEED = config.getDouble("MyPet.HungerSystem.SaturationPerFeed", 6.0);
        HungerSystem.AFFECT_RIDE_SPEED = config.getBoolean("MyPet.HungerSystem.Affect-Ride-Speed", true);
        HungerSystem.AFFECT_BEACON_RANGE = config.getBoolean("MyPet.HungerSystem.Affect-Beacon-Range", true);
        HungerSystem.FEED_FROM_INVENTORY = config.getBoolean("MyPet.HungerSystem.Feed-From-Inventory", true);
        HungerSystem.HUNGER_SYSTEM_FIXED = config.getDouble("MyPet.HungerSystem.Damage.Fixed", 1.);
        HungerSystem.HUNGER_SYSTEM_FACTOR = config.getDouble("MyPet.HungerSystem.Damage.Factor", 0.);
        HungerSystem.HUNGER_SYSTEM_TIME_BEFORE_DAMAGE = config.getDouble("MyPet.HungerSystem.Damage.Time-Before-Damage", 5.);
        HungerSystem.HUNGER_SYSTEM_CAN_KILL = config.getBoolean("MyPet.HungerSystem.Damage.can-kill", false);
        Misc.RETAIN_EQUIPMENT_ON_TAME = config.getBoolean("MyPet.RetainEquipmentOnTame", true);
        Misc.INVISIBLE_LIKE_OWNER = config.getBoolean("MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible", true);
        Misc.THROW_PLAYER_MOVE_EVENT_WHILE_RIDING = config.getBoolean("MyPet.Throw-PlayerMoveEvent-While-Riding", true);
        Misc.DISABLE_ALL_ACTIONBAR_MESSAGES = config.getBoolean("MyPet.Disable-All-Actionbar-Messages", false);
        Misc.OVERWRITE_LANGUAGE = config.getString("MyPet.OverwriteLanguages", "");
        LevelSystem.CALCULATION_MODE = config.getString("MyPet.LevelSystem.CalculationMode", "Default");
        Entity.MYPET_FOLLOW_START_DISTANCE = config.getDouble("MyPet.Entity.FollowStartDistance", 7.0D);

        Log.LEVEL = config.getString("MyPet.Log.Level", Log.LEVEL);

        NameFilter.NAME_FILTER.clear();
        for (Object o : config.getList("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"))) {
            NameFilter.NAME_FILTER.add(String.valueOf(o));
        }
        Name.MAX_LENGTH = config.getInt("MyPet.Name.MaxLength", Name.MAX_LENGTH);
        Name.Tag.SHOW = config.getBoolean("MyPet.Name.Tag.Show", Name.Tag.SHOW);
        Name.Tag.PREFIX = config.getString("MyPet.Name.Tag.Prefix", Name.Tag.PREFIX);
        Name.Tag.SUFFIX = config.getString("MyPet.Name.Tag.Suffix", Name.Tag.SUFFIX);

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


        Misc.WIKI_URL = config.getString("MyPet.Info.Wiki-URL", Misc.WIKI_URL);

        Permissions.EXTENDED = config.getBoolean("MyPet.Permissions.Extended", false);

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
        MyPet.Armadillo.CAN_SHED_SCUTE = config.getBoolean("MyPet.Pets.Armadillo.CanShedScute", true);
        MyPet.Chicken.CAN_LAY_EGGS = config.getBoolean("MyPet.Pets.Chicken.CanLayEggs", true);
        MyPet.CopperGolem.CAN_OXIDIZE = config.getBoolean("MyPet.Pets.CopperGolem.CanOxidize", true);
        MyPet.Cow.CAN_GIVE_MILK = config.getBoolean("MyPet.Pets.Cow.CanGiveMilk", true);
        MyPet.Creeper.ALLOW_FLINT_AND_STEEL_EXPLODE = config.getBoolean("MyPet.Pets.Creeper.AllowFlintAndSteelExplode", false);
        MyPet.Creeper.ALLOW_NON_OWNER_FLINT_AND_STEEL = config.getBoolean("MyPet.Pets.Creeper.AllowNonOwnerFlintAndSteel", false);
        MyPet.Creeper.ALLOW_EXPLOSION_BLOCK_DAMAGE = config.getBoolean("MyPet.Pets.Creeper.AllowExplosionBlockDamage", false);
        MyPet.Creeper.ALLOW_EXPLOSION_ENTITY_DAMAGE = config.getBoolean("MyPet.Pets.Creeper.AllowExplosionEntityDamage", false);
        MyPet.EnderDragon.GRANT_END_ADVANCEMENT_ON_KILL = config.getBoolean("MyPet.Pets.EnderDragon.GrantEndAdvancementOnKill", false);
        MyPet.EnderDragon.ALLOW_BLOCK_DAMAGE = config.getBoolean("MyPet.Pets.EnderDragon.AllowBlockDamage", false);
        MyPet.EnderDragon.ALLOW_PLAYER_CONTACT_DAMAGE = config.getBoolean("MyPet.Pets.EnderDragon.AllowPlayerContactDamage", false);
        MyPet.EnderDragon.ALLOW_ENTITY_CONTACT_DAMAGE = config.getBoolean("MyPet.Pets.EnderDragon.AllowEntityContactDamage", false);
        MyPet.Goat.CAN_DROP_HORN = config.getBoolean("MyPet.Pets.Goat.CanDropHorn", true);
        MyPet.Goat.CAN_GIVE_MILK = config.getBoolean("MyPet.Pets.Goat.CanGiveMilk", true);
        MyPet.Sheep.CAN_BE_SHEARED = config.getBoolean("MyPet.Pets.Sheep.CanBeSheared", true);
        MyPet.Sheep.CAN_REGROW_WOOL = config.getBoolean("MyPet.Pets.Sheep.CanRegrowWool", true);
        MyPet.IronGolem.CAN_TOSS_UP = config.getBoolean("MyPet.Pets.IronGolem.CanTossUp", true);
        MyPet.SnowGolem.DISABLE_SNOW_TRACK = config.getBoolean("MyPet.Pets.SnowGolem.DisableSnowTrack", true);
        MyPet.MagmaCube.CAN_HURT_PLAYERS_ON_CONTACT = config.getBoolean("MyPet.Pets.MagmaCube.CanHurtPlayersOnContact", false);
        MyPet.Mooshroom.CAN_GIVE_SOUP = config.getBoolean("MyPet.Pets.Mooshroom.CanGiveStew", false);
        MyPet.Panda.CAN_DROP_SLIMEBALL = config.getBoolean("MyPet.Pets.Panda.CanDropSlimeball", true);
        MyPet.Slime.CAN_HURT_PLAYERS_ON_CONTACT = config.getBoolean("MyPet.Pets.Slime.CanHurtPlayersOnContact", false);
        MyPet.Sniffer.CAN_DIG_SEEDS = config.getBoolean("MyPet.Pets.Sniffer.CanDigSeeds", true);

        // Dynamic per-type CanFly / CanSwim / AllowZombification /
        // PreventDaylightBurn / PreventSuffocation load. Reads the
        // MyPet.Pets.<Type>.{CanFly,CanSwim,AllowZombification,PreventDaylightBurn,PreventSuffocation}
        // keys populated by setDefault().
        for (PetType type : PetType.values()) {
            String base = "MyPet.Pets." + type.name();
            if (PetFlyingEntity.class.isAssignableFrom(type.getPetClass())) {
                MyPet.setCanFly(type.name(), config.getBoolean(base + ".CanFly", true));
            }
            if (PetSwimmingEntity.class.isAssignableFrom(type.getPetClass())) {
                MyPet.setCanSwim(type.name(), config.getBoolean(base + ".CanSwim", true));
            }
            if (PetZombifiable.class.isAssignableFrom(type.getPetClass())) {
                MyPet.setAllowZombification(type.name(), config.getBoolean(base + ".AllowZombification", false));
            }
            if (PetSunSensitive.class.isAssignableFrom(type.getPetClass())) {
                MyPet.setPreventDaylightBurn(type.name(), config.getBoolean(base + ".PreventDaylightBurn", true));
            }
            if (PetAquaticEntity.class.isAssignableFrom(type.getPetClass())) {
                MyPet.setPreventSuffocation(type.name(), config.getBoolean(base + ".PreventSuffocation", true));
            }
        }
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

        for (PetType petType : PetType.values()) {
            if (!petType.checkMinecraftVersion()) {
                continue;
            }
            DefaultInfo pi = petType.getPetClass().getAnnotation(DefaultInfo.class);
            if (pi == null) {
                continue;
            }

            MyPetApi.getPetInfo().setStartHP(petType, config.getDouble("MyPet.Pets." + petType.name() + ".HP", pi.hp()));
            MyPetApi.getPetInfo().setSpeed(petType, config.getDouble("MyPet.Pets." + petType.name() + ".Speed", pi.walkSpeed()));
            MyPetApi.getPetInfo().clearFood(petType);
            if (config.get("MyPet.Pets." + petType.name() + ".Food") instanceof ArrayList) {
                List<String> foodList = config.getStringList("MyPet.Pets." + petType.name() + ".Food");
                for (String foodString : foodList) {
                    ConfigItem ci = ConfigItem.createConfigItem(foodString);
                    if (ci.getItem() != null && ci.getItem().getType() != Material.AIR) {
                        MyPetApi.getPetInfo().addFood(petType, ci);
                    }
                }
            }
            loadLeashFlags(petType, config.getStringList("MyPet.Pets." + petType + ".LeashRequirements"));
            MyPetApi.getPetInfo().setCustomRespawnTimeFactor(petType, config.getInt("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFactor", 0));
            MyPetApi.getPetInfo().setCustomRespawnTimeFixed(petType, config.getInt("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFixed", 0));
            MyPetApi.getPetInfo().setReleaseOnDeath(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".ReleaseOnDeath", false));
            MyPetApi.getPetInfo().setRemoveAfterRelease(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".RemoveAfterRelease", false));
            MyPetApi.getPetInfo().setLeashItem(petType, ConfigItem.createConfigItem(config.getString("MyPet.Pets." + petType.name() + ".LeashItem", "lead")));
            if (PetBaby.class.isAssignableFrom(petType.getPetClass())) {
                String growUp = config.getString("MyPet.Pets." + petType.name() + ".GrowUpItem", pi.growUpItem().name().toLowerCase());
                MyPet.setGrowUpItem(petType.name(), ConfigItem.createConfigItem(growUp));
            }
        }
    }

    public static void upgradeConfig() {
        // Config key migrations are now handled by the MigrationService via ConfigMigration classes.
    }

    public static List<String> linkFood(Material[] foodTypes) {
        List<String> result = new ArrayList<>(foodTypes.length);
        for (Material m : foodTypes) {
            result.add(m.name().toLowerCase());
        }
        return result;
    }

    public static void loadLeashFlags(PetType type, List<String> leashFlagStrings) {
        MyPetApi.getPetInfo().clearLeashFlagSettings(type);
        for (String leashFlagString : leashFlagStrings) {
            boolean hasParameter = leashFlagString.contains(":");
            String[] data = leashFlagString.split(":", 2);
            Settings settings = new Settings(data[0]);
            if (hasParameter) {
                settings.load(data[1]);
            }
            MyPetApi.getPetInfo().addLeashFlagSetting(type, settings);
        }
    }
}
