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
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.config.ConfigKeyRegistry;
import de.Keyle.MyPet.api.entity.*;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.api.entity.PetBaby;
import de.Keyle.MyPet.api.skill.experience.MonsterExperience;
import de.Keyle.MyPet.entity.model.PetModelAnimation;
import de.Keyle.MyPet.entity.model.PetModelService;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import de.Keyle.MyPet.util.sentry.SentryErrorReporter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import de.Keyle.MyPet.entity.PetAttributes;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigurationLoader {

    /**
     * Whether config.yml was absent the first time {@link #setDefault()} ran — i.e. this boot
     * materialized MyPet's configuration from scratch, so the admin has never tuned anything.
     *
     * <p>config.yml is the marker because nothing else creates it: MyPet ships no config.yml
     * resource and never calls {@code saveDefaultConfig()}, so the file exists only once
     * {@link #setDefault()} has saved it at least once.</p>
     *
     * <p>Latched on the first call and never reassigned: {@code setDefault()} also runs on
     * {@code /mypet reload}, by which point the file always exists, and a reload must not
     * retroactively turn a fresh install stale.</p>
     */
    private static Boolean freshInstall = null;

    /**
     * True when this boot created the plugin's configuration from scratch.
     * False on every later boot, and before {@link #setDefault()} has run.
     */
    public static boolean isFreshInstall() {
        return freshInstall != null && freshInstall;
    }

    /**
     * Materializes every default row into config.yml, exp-config.yml and pet-config.yml.
     * Runs on boot and on every reload, so a custom type added to pet-config.yml since boot
     * gets its rows written without waiting for a restart. Idempotent — existing values survive.
     */
    public static void setDefault() {
        // Probe before anything below writes config.yml: no file means nobody has ever
        // configured this server. Only the first call decides — see the field's javadoc.
        if (freshInstall == null) {
            freshInstall = !new File(MyPetApi.getPlugin().getDataFolder(), "config.yml").exists();
        }

        // Re-read config.yml from disk first: this method saves it below, and on a reload the
        // cached copy would be stale, so saving it would clobber edits made since boot.
        MyPetApi.getPlugin().reloadConfig();
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

        // Global config.yml keys are now declared as ConfigKey fields on
        // MyPetGlobal; writeGlobalDefaults materializes every one. The keys
        // below have no MyPetGlobal field (Log.* is read directly by the Sentry
        // reporter, Name.Filter feeds NameFilter) so they stay inline.
        config.addDefault("MyPet.Log.Report-Errors", true);
        config.addDefault("MyPet.Log.Unique-ID", SentryErrorReporter.getServerUUID().toString());
        config.addDefault("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"));

        ConfigKeyRegistry.writeGlobalDefaults(config);

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
            // Custom (third-party) pet types have no @DefaultInfo annotation
            // (pi == null). They still need HP/Speed/Food/Leash/respawn rows
            // written, so resolve what the Host mob they spawn would use, and fall
            // back to hardcoded defaults for the rest, instead of skipping the type.
            // Vanilla types (pi != null) keep their exact annotation-derived defaults.
            DefaultInfo pi = petType.getPetClass().getAnnotation(DefaultInfo.class);

            config.addDefault("MyPet.Pets." + petType.name() + ".HP", resolveDefaultHp(petType, pi));
            config.addDefault("MyPet.Pets." + petType.name() + ".Speed", pi != null ? pi.walkSpeed() : 0.3);
            config.addDefault("MyPet.Pets." + petType.name() + ".Food", resolveDefaultFood(petType, pi));
            config.addDefault("MyPet.Pets." + petType.name() + ".LeashRequirements", pi != null ? pi.leashFlags() : new String[0]);
            config.addDefault("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFactor", 0);
            config.addDefault("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFixed", 0);
            config.addDefault("MyPet.Pets." + petType.name() + ".LeashItem", "lead");
            config.addDefault("MyPet.Pets." + petType.name() + ".ReleaseOnDeath", false);
            config.addDefault("MyPet.Pets." + petType.name() + ".RemoveAfterRelease", false);
        }

        // Rideable-pet config flags — register one ConfigKey per (pet type, flag)
        // for every Pet class that implements the matching marker. Materializes
        // YAML rows under MyPet.Pets.<Type>.<Flag> on first boot.
        //
        // PetNaturallyRideable -> RequireRideSkill (true), AllowNonOwnerPrimaryMount (false)
        // PetMultiPassenger    -> AllowNonOwnerSecondaryMount (true)
        // PetSaddleable        -> RequireSaddle (false), AllowNonOwnerSaddle (false)
        // PetBaby              -> PreventNaturalGrowup (true)
        // PetEquipment         -> RetainEquipmentOnTame (true)
        //
        // ConfigKey.bool self-registers with ConfigKeyRegistry on each call, and setDefault()
        // re-runs on every reload, so these go through registerFlagIfAbsent — registering a
        // second time would trip the registry's duplicate-registration warning for every type.
        for (PetType petType : PetType.values()) {
            if (!petType.checkMinecraftVersion()) {
                continue;
            }
            Class<?> petClass = petType.getPetClass();
            String name = petType.name();

            if (PetNaturallyRideable.class.isAssignableFrom(petClass)) {
                registerFlagIfAbsent(name, "RequireRideSkill", true);
                registerFlagIfAbsent(name, "RequireRideItem", true);
                registerFlagIfAbsent(name, "AllowNonOwnerPrimaryMount", false);
            }
            if (PetMultiPassenger.class.isAssignableFrom(petClass)) {
                registerFlagIfAbsent(name, "AllowNonOwnerSecondaryMount", true);
            }
            if (PetSaddleable.class.isAssignableFrom(petClass)) {
                registerFlagIfAbsent(name, "RequireSaddle", false);
                registerFlagIfAbsent(name, "AllowNonOwnerSaddle", false);
            }
            if (PetBaby.class.isAssignableFrom(petClass)) {
                registerFlagIfAbsent(name, "PreventNaturalGrowup", true);
            }
            if (PetEquipment.class.isAssignableFrom(petClass)) {
                registerFlagIfAbsent(name, "RetainEquipmentOnTame", true);
            }
        }

        // Per-pet ConfigKey defaults — every static ConfigKey<?> field declared
        // a PetXxx class (PetCreeper.ALLOW_LIGHTNING_POWER, etc.) self-registers with
        // ConfigKeyRegistry on first reference. ensureLoaded() forces all
        // nested classes to initialize so the registry walk below sees every
        // key. Covers CanFly, CanSwim, AllowZombification,
        // AllowLightningConversion, AllowMetamorphosis, PreventDaylightBurn, PreventSuffocation,
        // GrowUpItem, and every per-pet feature flag.
        // ConfigKeyRegistry methods force-load every pet class on first call,
        // so all per-pet ConfigKey static fields are registered before iteration.
        ConfigKeyRegistry.writeDefaults(config);

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

        // All global config.yml settings (Misc, Update, Repository, Respawn,
        // Name, Permissions, LevelSystem, HungerSystem, Skilltree, Exp, Skill)
        // are ConfigKey fields on MyPetGlobal; loadGlobalsFromYaml publishes
        // every value via volatile write. Hot-reloadable. Must run while
        // config still points at config.yml (before the exp/pet file switch).
        ConfigKeyRegistry.loadGlobalsFromYaml(config);

        NameFilter.NAME_FILTER.clear();
        for (Object o : config.getList("MyPet.Name.Filter", Lists.newArrayList("whore", "fuck"))) {
            NameFilter.NAME_FILTER.add(String.valueOf(o));
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
        // Every per-pet ConfigKey reads its MyPet.Pets.<Type>.<Key> entry and
        // publishes the new value via volatile write — covers all per-pet
        // feature flags plus the marker-derived keys (CanFly, CanSwim,
        // AllowZombification, AllowLightningConversion, PreventDaylightBurn,
        // PreventSuffocation, GrowUpItem). Hot-reloadable.
        // ConfigKeyRegistry methods force-load every pet class on first call,
        // so all per-pet ConfigKey static fields are registered before iteration.
        ConfigKeyRegistry.loadFromYaml(config);
    }

    public static void loadCompatConfiguration() {
        FileConfiguration config = MyPetApi.getPlugin().getConfig();

        // Drop all prior model mappings so a (re)load picks up edits and
        // removals; the loop below repopulates from each type's Model block.
        PetModelService.clearModels();
        PetModelService.clearAnimationOverrides();

        // CONTROL_ITEM / RIDE_ITEM are now MyPetGlobal ConfigKeys loaded by
        // ConfigKeyRegistry.loadGlobalsFromYaml in loadConfiguration().

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
            // Custom (third-party) pet types have no @DefaultInfo annotation
            // (pi == null); load their values with hardcoded fallbacks instead
            // of skipping. Vanilla types (pi != null) read exactly as before.
            DefaultInfo pi = petType.getPetClass().getAnnotation(DefaultInfo.class);

            MyPetApi.getPetInfo().setStartHP(petType, config.getDouble("MyPet.Pets." + petType.name() + ".HP", resolveDefaultHp(petType, pi)));
            MyPetApi.getPetInfo().setSpeed(petType, config.getDouble("MyPet.Pets." + petType.name() + ".Speed", pi != null ? pi.walkSpeed() : 0.3));
            MyPetApi.getPetInfo().setOverrideFlySpeed(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".OverrideFlySpeed", pi != null ? pi.overrideFlySpeed() : false));
            MyPetApi.getPetInfo().setFlySpeed(petType, config.getDouble("MyPet.Pets." + petType.name() + ".FlySpeed", pi != null ? pi.flySpeed() : 0.4));
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
            loadLeashFlags(petType, config.getStringList("MyPet.Pets." + petType.name() + ".LeashRequirements"));
            MyPetApi.getPetInfo().setCustomRespawnTimeFactor(petType, config.getInt("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFactor", 0));
            MyPetApi.getPetInfo().setCustomRespawnTimeFixed(petType, config.getInt("MyPet.Pets." + petType.name() + ".CustomRespawnTimeFixed", 0));
            MyPetApi.getPetInfo().setReleaseOnDeath(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".ReleaseOnDeath", false));
            MyPetApi.getPetInfo().setRemoveAfterRelease(petType, config.getBoolean("MyPet.Pets." + petType.name() + ".RemoveAfterRelease", false));
            MyPetApi.getPetInfo().setLeashItem(petType, ConfigItem.createConfigItem(config.getString("MyPet.Pets." + petType.name() + ".LeashItem", "lead")));

            // Record this type's Model sub-block (re-skins for vanilla types and
            // the renderer mapping for custom types). PascalCase keys per the
            // pet-config custom-pet key invariant. nameHeight is optional.
            String base = "MyPet.Pets." + petType.name();
            if (config.isConfigurationSection(base + ".Model")) {
                // Uniform shape for every custom creature: Provider + Id. Whether MyPet renders the
                // model (ModelEngine/BetterModel/ItemsAdder) or the model rides in from an adopted
                // source creature (MythicMobs) is decided at spawn time from the provider's hook
                // type — not here. See PetModelService.isSourceDriven / resolve.
                String provider = config.getString(base + ".Model.Provider");
                String id = config.getString(base + ".Model.Id");
                Double nameHeight = config.contains(base + ".Model.NameHeight")
                        ? config.getDouble(base + ".Model.NameHeight") : null;
                PetModelService.registerModel(petType.name(),
                        new PetModelService.ModelConfig(provider, id, nameHeight));
                // Optional per-event animation-name overrides (rendered and source alike).
                ConfigurationSection anims = config.getConfigurationSection(base + ".Model.Animations");
                if (anims != null) {
                    Map<PetModelAnimation, String> overrides = new EnumMap<>(PetModelAnimation.class);
                    for (PetModelAnimation a : PetModelAnimation.values()) {
                        String name = anims.getString(a.defaultName());
                        if (name != null && !name.isBlank()) {
                            overrides.put(a, name);
                        }
                    }
                    PetModelService.registerAnimationOverrides(petType.name(), overrides);
                }
            }
        }
        // GrowUpItem is now loaded by ConfigKeyRegistry.loadFromYaml in loadConfiguration().
    }

    public static void upgradeConfig() {
        // Config key migrations are now handled by the MigrationService via ConfigMigration classes.
    }

    /** Registers a per-pet flag key, unless one is already registered for {@code (petType, key)}. */
    private static void registerFlagIfAbsent(String petType, String key, boolean defaultValue) {
        if (ConfigKeyRegistry.lookup(petType, key) == null) {
            ConfigKey.bool(petType, key, defaultValue);
        }
    }

    /**
     * The Bukkit entity type a pet type's defaults come from: itself for a vanilla type, and
     * for a custom type — whose name is not a Bukkit entity — the Host mob it spawns.
     * Null when neither resolves.
     */
    private static EntityType defaultsEntityType(PetType petType) {
        // An explicit Host always wins: a custom type's own name may coincidentally match a
        // Bukkit entity, and its Host is what actually spawns.
        Class<?> host = petType.getHostOverride();
        if (host != null) {
            for (EntityType candidate : EntityType.values()) {
                if (candidate.getEntityClass() == host) {
                    return candidate;
                }
            }
            return null;
        }
        try {
            return EntityType.valueOf(petType.getBukkitName());
        } catch (IllegalArgumentException noSuchEntity) {
            return null;
        }
    }

    /**
     * Default starting HP for a pet type: an explicit {@code @DefaultInfo(hp=…)} override
     * if one is set (a non-negative value), otherwise the natural max-health of the entity it
     * spawns (Wolf 8, Cow 10, …) — for a custom type that means its Host mob. Falls back to 20
     * when the entity has no default attributes.
     */
    public static double resolveDefaultHp(PetType petType, DefaultInfo pi) {
        if (pi != null && pi.hp() >= 0) {
            return pi.hp();
        }
        EntityType type = defaultsEntityType(petType);
        if (type != null && type.hasDefaultAttributes()) {
            AttributeInstance health = type.getDefaultAttributes().getAttribute(PetAttributes.MAX_HEALTH);
            if (health != null) {
                return health.getBaseValue();
            }
        }
        return 20.0;
    }

    /**
     * Default food list for a pet type: its own {@code @DefaultInfo(food=…)}, or — for a custom
     * type, which carries no annotation — the food of the MyPet type its Host mob maps to, so a
     * Mooshroom-hosted creature eats what a Mooshroom pet eats. Empty when neither resolves.
     */
    private static List<String> resolveDefaultFood(PetType petType, DefaultInfo pi) {
        if (pi != null) {
            return linkFood(pi.food());
        }
        EntityType host = defaultsEntityType(petType);
        PetType hostType = host == null ? null : PetType.byEntityTypeNameOrNull(host.name());
        if (hostType == null || hostType == petType) {
            return new ArrayList<>();
        }
        DefaultInfo hostInfo = hostType.getPetClass().getAnnotation(DefaultInfo.class);
        return hostInfo != null ? linkFood(hostInfo.food()) : new ArrayList<>();
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
