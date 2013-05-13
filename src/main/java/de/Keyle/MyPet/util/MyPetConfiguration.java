/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.chatcommands.CommandInfo.PetInfoDisplay;
import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.entity.types.chicken.EntityMyChicken;
import de.Keyle.MyPet.entity.types.cow.EntityMyCow;
import de.Keyle.MyPet.entity.types.irongolem.EntityMyIronGolem;
import de.Keyle.MyPet.entity.types.mooshroom.EntityMyMooshroom;
import de.Keyle.MyPet.entity.types.ocelot.EntityMyOcelot;
import de.Keyle.MyPet.entity.types.pig.EntityMyPig;
import de.Keyle.MyPet.entity.types.sheep.EntityMySheep;
import de.Keyle.MyPet.entity.types.villager.EntityMyVillager;
import de.Keyle.MyPet.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyPet.entity.types.zombie.EntityMyZombie;
import de.Keyle.MyPet.skill.MyPetExperience;
import de.Keyle.MyPet.skill.MyPetMonsterExperience;
import de.Keyle.MyPet.skill.skills.implementation.*;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.List;

public class MyPetConfiguration
{
    public static FileConfiguration config;

    public static Material LEASH_ITEM = Material.STRING;
    public static String PET_INFO_OVERHEAD_PREFIX = "" + ChatColor.AQUA;
    public static String PET_INFO_OVERHEAD_SUFFIX = "";
    public static int PASSIVE_PERCENT_PER_MONSTER = 25;
    public static int RESPAWN_TIME_FACTOR = 5;
    public static int RESPAWN_TIME_PLAYER_FACTOR = 5;
    public static int RESPAWN_TIME_FIXED = 0;
    public static int RESPAWN_TIME_PLAYER_FIXED = 0;
    public static int AUTOSAVE_TIME = 60;
    public static int HUNGER_SYSTEM_TIME = 60;
    public static int HUNGER_SYSTEM_POINTS_PER_FEED = 6;
    public static int SKILLTREE_SWITCH_PENALTY_PERCENT = 5;
    public static float MYPET_FOLLOW_START_DISTANCE = 7.0F;
    public static double SKILLTREE_SWITCH_PENALTY_FIXED = 0.0;
    public static double RESPAWN_COSTS_FACTOR = 1.0;
    public static double RESPAWN_COSTS_FIXED = 0.0;
    public static boolean SKILLTREE_SWITCH_PENALTY_ADMIN = false;
    public static boolean AUTOMATIC_SKILLTREE_ASSIGNMENT = true;
    public static boolean CHOOSE_SKILLTREE_ONLY_ONCE = true;
    public static boolean PREVENT_LEVELLING_WITHOUT_SKILLTREE = true;
    public static boolean OWNER_CAN_ATTACK_PET = false;
    public static boolean USE_LEVEL_SYSTEM = true;
    public static boolean USE_HUNGER_SYSTEM = true;
    public static boolean USE_DEBUG_LOGGER = true;
    public static boolean INHERIT_ALREADY_INHERITED_SKILLS = false;
    public static boolean ENABLE_EVENTS = false;
    public static boolean REMOVE_PETS_AFTER_RELEASE = false;
    public static boolean PET_INFO_OVERHEAD_NAME = true;
    public static boolean STORE_PETS_ON_PLAYER_QUIT = true;
    public static boolean STORE_PETS_ON_PET_LEASH = true;
    public static boolean STORE_PETS_ON_PET_RELEASE = true;

    public static void setDefault()
    {
        config.addDefault("MyPet.Leash.Item", LEASH_ITEM.getId());
        config.addDefault("MyPet.OwnerCanAttackPet", false);
        config.addDefault("MyPet.CheckForUpdates", false);
        config.addDefault("MyPet.DebugLogger", true);
        config.addDefault("MyPet.EnableEvents", false);
        config.addDefault("MyPet.RemovePetsAfterRelease", false);
        config.addDefault("MyPet.FollowStartDistance", 7.0D);

        config.addDefault("MyPet.Backup.Active", MyPetBackup.MAKE_BACKUPS);
        config.addDefault("MyPet.Backup.SaveInterval", MyPetBackup.SAVE_INTERVAL);
        config.addDefault("MyPet.Backup.DateFormat", MyPetBackup.DATE_FORMAT);

        config.addDefault("MyPet.PetStorage.AutoSaveTime", 60);
        config.addDefault("MyPet.PetStorage.OnPlayerQuit", true);
        config.addDefault("MyPet.PetStorage.OnPetLeash", true);
        config.addDefault("MyPet.PetStorage.OnPetRelease", true);

        config.addDefault("MyPet.Respawn.Time.Default.Factor", 5);
        config.addDefault("MyPet.Respawn.Time.Player.Factor", 5);
        config.addDefault("MyPet.Respawn.Time.Default.Fixed", 0);
        config.addDefault("MyPet.Respawn.Time.Player.Fixed", 0);
        config.addDefault("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        config.addDefault("MyPet.Respawn.EconomyCost.Factor", 1.0);

        config.addDefault("MyPet.Permissions.Enabled", true);
        config.addDefault("MyPet.Permissions.UseExtendedPermissions", false);

        config.addDefault("MyPet.LevelSystem.Active", true);
        config.addDefault("MyPet.LevelSystem.CalculationMode", "Default");
        config.addDefault("MyPet.LevelSystem.Firework", true);

        config.addDefault("MyPet.HungerSystem.Active", true);
        config.addDefault("MyPet.HungerSystem.Time", 60);
        config.addDefault("MyPet.HungerSystem.HungerPointsPerFeed", 6);

        config.addDefault("MyPet.Skilltree.AutomaticAssignment", true);
        config.addDefault("MyPet.Skilltree.InheritAlreadyInheritedSkills", true);
        config.addDefault("MyPet.Skilltree.ChooseOnce", true);
        config.addDefault("MyPet.Skilltree.PreventLevellingWithout", true);
        config.addDefault("MyPet.Skilltree.SwitchPenaltyFixed", 0.0);
        config.addDefault("MyPet.Skilltree.SwitchPenaltyPercent", 5);
        config.addDefault("MyPet.Skilltree.SwitchPenaltyAdmin", false);

        config.addDefault("MyPet.Support.Towny", true);
        config.addDefault("MyPet.Support.Heroes", true);
        config.addDefault("MyPet.Support.Factions", true);
        config.addDefault("MyPet.Support.WorldGuard", true);
        config.addDefault("MyPet.Support.Citizens", true);
        config.addDefault("MyPet.Support.mcMMO", true);
        config.addDefault("MyPet.Support.Regios", true);
        config.addDefault("MyPet.Support.MobArena", true);
        config.addDefault("MyPet.Support.Residence", true);
        config.addDefault("MyPet.Support.AncientRPG", true);
        config.addDefault("MyPet.Support.GriefPrevention", true);
        config.addDefault("MyPet.Support.Vault.Economy", true);

        config.addDefault("MyPet.Exp.DamageWeightedExperienceDistribution", false);
        config.addDefault("MyPet.Exp.Passive.PercentPerMonster", 25);
        config.addDefault("MyPet.Exp.Loss.Percent", 0);
        config.addDefault("MyPet.Exp.Loss.Fixed", 0.0);
        config.addDefault("MyPet.Exp.Loss.Drop", true);
        config.addDefault("MyPet.Exp.Gain.MonsterSpawner", true);

        config.addDefault("MyPet.Skill.Control.Item", Control.ITEM.getId());
        config.addDefault("MyPet.Skill.Ride.Item", Ride.ITEM.getId());
        config.addDefault("MyPet.Skill.Inventory.Creative", true);
        config.addDefault("MyPet.Skill.Inventory.DropWhenOwnerDies", false);
        config.addDefault("MyPet.Skill.Behavior.Aggro", true);
        config.addDefault("MyPet.Skill.Behavior.Farm", true);
        config.addDefault("MyPet.Skill.Behavior.Friendly", true);
        config.addDefault("MyPet.Skill.Behavior.Raid", true);
        config.addDefault("MyPet.Skill.Behavior.Duel", true);
        config.addDefault("MyPet.Skill.Beacon.HungerDecreaseTime", 100);

        config.addDefault("MyPet.Pets.Chicken.CanLayEggs", true);
        config.addDefault("MyPet.Pets.Cow.CanGiveMilk", true);
        config.addDefault("MyPet.Pets.Sheep.CanBeSheared", true);
        config.addDefault("MyPet.Pets.Sheep.CanRegrowWool", true);
        config.addDefault("MyPet.Pets.IronGolem.CanThrowUp", true);
        config.addDefault("MyPet.Pets.Chicken.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Cow.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Mooshroom.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Ocelot.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Pig.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Sheep.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Villager.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Wolf.GrowUpItem", Material.POTION.getId());
        config.addDefault("MyPet.Pets.Zombie.GrowUpItem", Material.POTION.getId());

        config.addDefault("MyPet.Info.AdminOnly.PetName", false);
        config.addDefault("MyPet.Info.AdminOnly.PetOwner", false);
        config.addDefault("MyPet.Info.AdminOnly.PetHP", false);
        config.addDefault("MyPet.Info.AdminOnly.PetDamage", false);
        config.addDefault("MyPet.Info.AdminOnly.PetHunger", true);
        config.addDefault("MyPet.Info.AdminOnly.PetLevel", true);
        config.addDefault("MyPet.Info.AdminOnly.PetEXP", true);
        config.addDefault("MyPet.Info.AdminOnly.PetSkilltree", true);

        config.addDefault("MyPet.Info.OverHead.Name", true);
        config.addDefault("MyPet.Info.OverHead.Prefix", "%aqua%");
        config.addDefault("MyPet.Info.OverHead.Suffix", "");

        for (MyPetType petType : MyPetType.values())
        {
            MyPetInfo pi = petType.getMyPetClass().getAnnotation(MyPetInfo.class);

            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".HP", pi.hp());
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".Speed", pi.walkSpeed());
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".Food", linkFood(pi.food()));
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".LeashFlags", linkLeashFlags(pi.leashFlags()));
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFactor", 0);
            config.addDefault("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFixed", 0);
        }

        for (EntityType entityType : MyPetMonsterExperience.mobExp.keySet())
        {
            config.addDefault("MyPet.Exp.Active." + entityType.getName() + ".Min", MyPetMonsterExperience.getMonsterExperience(entityType).getMin());
            config.addDefault("MyPet.Exp.Active." + entityType.getName() + ".Max", MyPetMonsterExperience.getMonsterExperience(entityType).getMax());
        }

        config.options().copyDefaults(true);
        MyPetPlugin.getPlugin().saveConfig();
    }

    public static void loadConfiguration()
    {
        LEASH_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Leash.Item", 287), Material.STRING);
        Control.ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Skill.Control.Item", 287), Material.STRING);
        Ride.ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Skill.Ride.Item", 287), Material.STRING);
        Beacon.HUNGER_DECREASE_TIME = config.getInt("MyPet.Skill.Beacon.HungerDecreaseTime", 100);
        Inventory.OPEN_IN_CREATIVEMODE = config.getBoolean("MyPet.Skill.Inventory.Creative", true);
        Inventory.DROP_WHEN_OWNER_DIES = config.getBoolean("MyPet.Skill.Inventory.DropWhenOwnerDies", false);
        Behavior.BehaviorState.Aggressive.setActive(config.getBoolean("MyPet.Skill.Behavior.Aggro", true));
        Behavior.BehaviorState.Farm.setActive(config.getBoolean("MyPet.Skill.Behavior.Farm", true));
        Behavior.BehaviorState.Friendly.setActive(config.getBoolean("MyPet.Skill.Behavior.Friendly", true));
        Behavior.BehaviorState.Raid.setActive(config.getBoolean("MyPet.Skill.Behavior.Raid", true));
        Behavior.BehaviorState.Duel.setActive(config.getBoolean("MyPet.Skill.Behavior.Duel", true));

        MyPetBackup.MAKE_BACKUPS = config.getBoolean("MyPet.Backup.Active", MyPetBackup.MAKE_BACKUPS);
        MyPetBackup.SAVE_INTERVAL = config.getInt("MyPet.Backup.SaveInterval", MyPetBackup.SAVE_INTERVAL);
        MyPetBackup.DATE_FORMAT = config.getString("MyPet.Backup.DateFormat", MyPetBackup.DATE_FORMAT);

        SKILLTREE_SWITCH_PENALTY_FIXED = config.getDouble("MyPet.Skilltree.SwitchPenaltyFixed", 0.0);
        SKILLTREE_SWITCH_PENALTY_PERCENT = config.getInt("MyPet.Skilltree.SwitchPenaltyPercent", 5);
        SKILLTREE_SWITCH_PENALTY_ADMIN = config.getBoolean("MyPet.Skilltree.SwitchPenaltyAdmin", false);
        INHERIT_ALREADY_INHERITED_SKILLS = config.getBoolean("MyPet.Skilltree.InheritAlreadyInheritedSkills", false);
        PASSIVE_PERCENT_PER_MONSTER = config.getInt("MyPet.exp.passive.PercentPerMonster", 25);
        RESPAWN_TIME_FACTOR = config.getInt("MyPet.Respawn.Time.Default.Factor", 5);
        RESPAWN_TIME_PLAYER_FACTOR = config.getInt("MyPet.Respawn.Time.Player.Factor", 5);
        RESPAWN_TIME_FIXED = config.getInt("MyPet.Respawn.Time.Default.Fixed", 0);
        RESPAWN_TIME_PLAYER_FIXED = config.getInt("MyPet.Respawn.Time.Player.Fixed", 0);
        RESPAWN_COSTS_FACTOR = config.getDouble("MyPet.Respawn.EconomyCost.Factor", 1.0);
        RESPAWN_COSTS_FIXED = config.getDouble("MyPet.Respawn.EconomyCost.Fixed", 0.0);
        AUTOMATIC_SKILLTREE_ASSIGNMENT = config.getBoolean("MyPet.Skilltree.AutomaticAssignment", true);
        CHOOSE_SKILLTREE_ONLY_ONCE = config.getBoolean("MyPet.Skilltree.ChooseOnce", true);
        PREVENT_LEVELLING_WITHOUT_SKILLTREE = config.getBoolean("MyPet.Skilltree.PreventLevellingWithout", true);
        USE_LEVEL_SYSTEM = config.getBoolean("MyPet.LevelSystem.Active", true);
        OWNER_CAN_ATTACK_PET = config.getBoolean("MyPet.OwnerCanAttackPet", false);
        USE_HUNGER_SYSTEM = config.getBoolean("MyPet.HungerSystem.Active", true);
        HUNGER_SYSTEM_TIME = config.getInt("MyPet.HungerSystem.Time", 60);
        HUNGER_SYSTEM_POINTS_PER_FEED = config.getInt("MyPet.HungerSystem.HungerPointsPerFeed", 6);
        USE_DEBUG_LOGGER = config.getBoolean("MyPet.DebugLogger", true);
        ENABLE_EVENTS = config.getBoolean("MyPet.EnableEvents", false);
        REMOVE_PETS_AFTER_RELEASE = config.getBoolean("MyPet.RemovePetsAfterRelease", false);
        MYPET_FOLLOW_START_DISTANCE = (float) config.getDouble("MyPet.FollowStartDistance", 7.0D);

        AUTOSAVE_TIME = config.getInt("MyPet.PetStorage.AutoSaveTime", 60);
        STORE_PETS_ON_PLAYER_QUIT = config.getBoolean("MyPet.PetStorage.OnPlayerQuit", true);
        STORE_PETS_ON_PET_LEASH = config.getBoolean("MyPet.PetStorage.OnPetLeash", true);
        STORE_PETS_ON_PET_RELEASE = config.getBoolean("MyPet.PetStorage.OnPetRelease", true);

        PetInfoDisplay.Name.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetName", false);
        PetInfoDisplay.HP.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetHP", false);
        PetInfoDisplay.Damage.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetDamage", false);
        PetInfoDisplay.Hunger.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetHunger", false);
        PetInfoDisplay.Level.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetLevel", true);
        PetInfoDisplay.Exp.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetEXP", true);
        PetInfoDisplay.Owner.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetOwner", true);
        PetInfoDisplay.Skilltree.adminOnly = config.getBoolean("MyPet.Info.AdminOnly.PetOwner", true);

        PET_INFO_OVERHEAD_NAME = config.getBoolean("MyPet.Info.OverHead.Name", true);
        PET_INFO_OVERHEAD_PREFIX = MyPetBukkitUtil.setColors(config.getString("MyPet.Info.OverHead.Prefix", "%aqua%"));
        PET_INFO_OVERHEAD_SUFFIX = MyPetBukkitUtil.setColors(config.getString("MyPet.Info.OverHead.Suffix", ""));

        MyPetPermissions.USE_EXTENDET_PERMISSIONS = config.getBoolean("MyPet.Permissions.UseExtendedPermissions", false);
        MyPetPermissions.ENABLED = config.getBoolean("MyPet.Permissions.Enabled", true);

        MyPetEconomy.USE_ECONOMY = config.getBoolean("MyPet.Support.Vault.Economy", true);
        MyPetPvP.USE_Towny = config.getBoolean("MyPet.Support.Towny", true);
        MyPetPvP.USE_Factions = config.getBoolean("MyPet.Support.Factions", true);
        MyPetPvP.USE_WorldGuard = config.getBoolean("MyPet.Support.WorldGuard", true);
        MyPetPvP.USE_Citizens = config.getBoolean("MyPet.Support.Citizens", true);
        MyPetPvP.USE_Heroes = config.getBoolean("MyPet.Support.Heroes", true);
        MyPetPvP.USE_McMMO = config.getBoolean("MyPet.Support.mcMMO", true);
        MyPetPvP.USE_MobArena = config.getBoolean("MyPet.Support.MobArena", true);
        MyPetPvP.USE_Regios = config.getBoolean("MyPet.Support.Regios", true);
        MyPetPvP.USE_Residence = config.getBoolean("MyPet.Support.Residence", true);
        MyPetPvP.USE_AncientRPG = config.getBoolean("MyPet.Support.AncientRPG", true);
        MyPetPvP.USE_GriefPrevention = config.getBoolean("MyPet.Support.GriefPrevention", true);

        MyPetExperience.LOSS_PERCENT = config.getInt("MyPet.Exp.loss.Percent", 0);
        MyPetExperience.LOSS_FIXED = config.getDouble("MyPet.Exp.loss.Fixed", 0.0);
        MyPetExperience.DROP_LOST_EXP = config.getBoolean("MyPet.Exp.Loss.Drop", true);
        MyPetExperience.GAIN_EXP_FROM_MONSTER_SPAWNER_MOBS = config.getBoolean("MyPet.Exp.Gain.MonsterSpawner", true);
        MyPetExperience.CALCULATION_MODE = config.getString("MyPet.LevelSystem.CalculationMode", "Default");
        MyPetExperience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION = config.getBoolean("MyPet.Exp.DamageWeightedExperienceDistribution", false);
        MyPetExperience.FIREWORK_ON_LEVELUP = config.getBoolean("MyPet.LevelSystem.Firework", true);

        EntityMyChicken.CAN_LAY_EGGS = config.getBoolean("MyPet.Pets.Chicken.CanLayEggs", true);
        EntityMyCow.CAN_GIVE_MILK = config.getBoolean("MyPet.Pets.Cow.CanGiveMilk", true);
        EntityMySheep.CAN_BE_SHEARED = config.getBoolean("MyPet.Pets.Sheep.CanBeSheared", true);
        EntityMySheep.CAN_REGROW_WOOL = config.getBoolean("MyPet.Pets.Sheep.CanRegrowWool", true);
        EntityMyIronGolem.CAN_THROW_UP = config.getBoolean("MyPet.Pets.IronGolem.CanThrowUp", true);
        EntityMyChicken.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Chicken.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyCow.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Cow.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyMooshroom.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Mooshroom.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyOcelot.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Ocelot.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyPig.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Pig.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMySheep.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Sheep.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyVillager.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Villager.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyWolf.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Wolf.GrowUpItem", Material.POTION.getId()), Material.POTION);
        EntityMyZombie.GROW_UP_ITEM = MyPetBukkitUtil.checkMaterial(config.getInt("MyPet.Pets.Zombie.GrowUpItem", Material.POTION.getId()), Material.POTION);

        MyPet.resetOptions();
        for (MyPetType petType : MyPetType.values())
        {
            MyPetInfo pi = petType.getMyPetClass().getAnnotation(MyPetInfo.class);

            MyPet.setStartHP(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".HP", pi.hp()));
            MyPet.setStartSpeed(petType.getMyPetClass(), (float) config.getDouble("MyPet.Pets." + petType.getTypeName() + ".Speed", pi.walkSpeed()));
            seperateFood(petType.getMyPetClass(), config.getString("MyPet.Pets." + petType.getTypeName() + ".Food", linkFood(pi.food())));
            seperateLeashFlags(petType.getMyPetClass(), config.getString("MyPet.Pets." + petType.getTypeName() + ".LeashFlags", linkLeashFlags(pi.leashFlags())));
            MyPet.setCustomRespawnTimeFactor(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFactor", 0));
            MyPet.setCustomRespawnTimeFixed(petType.getMyPetClass(), config.getInt("MyPet.Pets." + petType.getTypeName() + ".CustomRespawnTimeFixed", 0));
        }

        if (config.getStringList("MyPet.exp.active") != null)
        {
            double min;
            double max;
            for (EntityType entityType : MyPetMonsterExperience.mobExp.keySet())
            {
                max = config.getDouble("MyPet.Exp.Active." + entityType.getName() + ".Max", 0.);
                min = config.getDouble("MyPet.Exp.Active." + entityType.getName() + ".Min", 0.);
                if (min == max)
                {
                    MyPetMonsterExperience.getMonsterExperience(entityType).setExp(max);
                }
                else
                {
                    MyPetMonsterExperience.getMonsterExperience(entityType).setMin(min);
                    MyPetMonsterExperience.getMonsterExperience(entityType).setMax(max);
                }
            }
        }
    }

    private static String linkFood(Material[] foodTypes)
    {
        String linkedFood = "";
        for (Material foodType : foodTypes)
        {
            if (!linkedFood.equalsIgnoreCase(""))
            {
                linkedFood += ",";
            }
            linkedFood += foodType.getId();
        }
        return linkedFood;
    }

    private static void seperateFood(Class<? extends MyPet> myPetClass, String foodString)
    {
        foodString = foodString.replaceAll("\\s", "");
        if (foodString.contains(","))
        {
            for (String foodIDString : foodString.split(","))
            {
                if (MyPetUtil.isInt(foodIDString))
                {
                    int itemID = Integer.parseInt(foodIDString);
                    if (MyPetBukkitUtil.isValidMaterial(itemID) && itemID != 0)
                    {
                        MyPet.setFood(myPetClass, Material.getMaterial(itemID));
                    }
                }
            }
        }
        else
        {
            if (MyPetUtil.isInt(foodString))
            {
                int itemID = Integer.parseInt(foodString);
                if (MyPetBukkitUtil.isValidMaterial(itemID) && itemID != 0)
                {
                    MyPet.setFood(myPetClass, Material.getMaterial(itemID));
                }
            }
        }
    }

    private static String linkLeashFlags(LeashFlag[] leashFlags)
    {
        String linkedLeashFlags = "";
        for (LeashFlag leashFlag : leashFlags)
        {
            if (!linkedLeashFlags.equalsIgnoreCase(""))
            {
                linkedLeashFlags += ",";
            }
            linkedLeashFlags += leashFlag.name();
        }
        return linkedLeashFlags;
    }

    private static void seperateLeashFlags(Class<? extends MyPet> myPetClass, String leashFlagString)
    {
        leashFlagString = leashFlagString.replaceAll("\\s", "");
        if (leashFlagString.contains(","))
        {
            for (String leashFlagSplit : leashFlagString.split(","))
            {
                if (LeashFlag.getLeashFlagByName(leashFlagSplit) != null)
                {
                    MyPet.setLeashFlags(myPetClass, LeashFlag.getLeashFlagByName(leashFlagSplit));
                }
                else
                {
                    MyPetLogger.write(ChatColor.RED + leashFlagString + " is not a valid LeashFlag!");
                }
            }
        }
        else
        {
            if (LeashFlag.getLeashFlagByName(leashFlagString) != null)
            {
                MyPet.setLeashFlags(myPetClass, LeashFlag.getLeashFlagByName(leashFlagString));
            }
            else
            {
                MyPetLogger.write(ChatColor.RED + leashFlagString + " is not a valid LeashFlag!");
            }
        }
    }

    public static void getConfigOptionList(List<String> list, String startnode)
    {
        ConfigurationSection cs = config.getConfigurationSection(startnode);
        for (String node : cs.getKeys(false))
        {
            if (config.get(startnode + "." + node) instanceof ConfigurationSection)
            {
                getConfigOptionList(list, startnode + "." + node);
            }
            else
            {
                list.add(startnode + "." + node + ": " + config.get(startnode + "." + node));
            }
        }
    }
}