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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.experience.ExperienceCalculatorManager;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.entity.model.BundledModelInstaller;
import de.Keyle.MyPet.entity.model.CustomPetLoader;
import de.Keyle.MyPet.entity.model.PetModelService;
import de.Keyle.MyPet.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.util.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.util.Optional;

/**
 * The reload logic behind {@code /mypet reload} and the web editor's apply.
 *
 * <p>Callers are responsible for user-facing messaging; these methods only log.
 * All three MUST be called on the server main thread — they mutate live plugin
 * state (permissions, services, active pets).
 */
public final class MyPetReloader {

    private MyPetReloader() {
    }

    /** Reload config.yml, pet-config.yml, exp-config.yml, hooks-config.yml and locale, and apply the side effects. */
    public static void reloadConfig() {
        int oldMaxPetCount = MyPetGlobal.Misc.MAX_STORED_PET_COUNT.get();
        // Register any custom creatures added to pet-config.yml since boot BEFORE the per-type
        // loops run, so the new types get their defaults, PetInfo, and model mapping loaded below.
        // (Idempotent — existing types are skipped. Changing an existing type's Host still needs a
        // restart, since its host class is bound at registration.)
        CustomPetLoader.registerCustomTypes();
        ConfigurationLoader.loadConfiguration();
        ConfigurationLoader.loadCompatConfiguration();
        // Wire permission nodes for any newly-registered custom types (no-op for existing ones).
        PetPermissions.registerAll();

        Locale.init();

        if (MyPetGlobal.Misc.MAX_STORED_PET_COUNT.get() > oldMaxPetCount) {
            for (int i = oldMaxPetCount + 1; i <= MyPetGlobal.Misc.MAX_STORED_PET_COUNT.get(); i++) {
                try {
                    Bukkit.getPluginManager().addPermission(new Permission("MyPet.petstorage.limit." + i));
                } catch (Exception ignored) {
                }
            }
        } else if (oldMaxPetCount > MyPetGlobal.Misc.MAX_STORED_PET_COUNT.get()) {
            for (int i = oldMaxPetCount; i > MyPetGlobal.Misc.MAX_STORED_PET_COUNT.get(); i--) {
                try {
                    Bukkit.getPluginManager().removePermission("MyPet.petstorage.limit." + i);
                } catch (Exception ignored) {
                }
            }
        }

        ExperienceCalculatorManager calculatorManager = MyPetApi.getServiceManager().getService(ExperienceCalculatorManager.class).get();
        calculatorManager.switchCalculator(MyPetGlobal.LevelSystem.CALCULATION_MODE.get());

        MyPetApi.getServiceManager().getConfig().loadConfig();

        for (ServiceContainer hook : MyPetApi.getServiceManager().getServices(ServiceContainer.class).stream()
                .filter(s -> s.getClass().isAnnotationPresent(RequiresPlugin.class))
                .toList()) {
            ConfigurationSection pluginSection = MyPetApi.getServiceManager().getConfig().getConfig().getConfigurationSection(hook.getServiceName());
            if (pluginSection != null) {
                hook.loadConfig(pluginSection);
            }
        }
        // The pet-config Model blocks were just re-loaded above (loadCompatConfiguration repopulates
        // the model registry); re-apply them to already-spawned pets so removed/changed models take
        // effect live.
        BundledModelInstaller.installReferencedDefaults();
        PetModelService.reapplyAll();
        MyPetApi.getLogger().info("Config reloaded!");
    }

    /** Reload the skilltrees/ directory and re-point every active pet at its (possibly new) tree. */
    public static void reloadSkilltrees() {
        MyPetApi.getSkilltreeManager().clearSkilltrees();

        SkillTreeLoaderJSON.loadSkilltrees(new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"));

        for (Pet pet : MyPetApi.getPetManager().getAllActivePets()) {
            Skilltree skilltree = pet.getSkilltree();
            if (skilltree != null) {
                String skilltreeName = skilltree.getName();
                if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
                    skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                    if (!skilltree.getMobTypes().contains(pet.getPetType())) {
                        skilltree = null;
                    }
                } else {
                    skilltree = null;
                }
            }
            pet.setSkilltree(skilltree);
        }
        MyPetApi.getLogger().info("Skilltrees reloaded!");
    }

    /** Reload shop definitions. No-op when the shop service isn't registered. */
    public static void reloadShops() {
        Optional<ShopManager> shopManager = MyPetApi.getServiceManager().getService(ShopManager.class);
        if (shopManager.isPresent()) {
            shopManager.get().onEnable(); //TODO reload method?
        }
        MyPetApi.getLogger().info("Shops reloaded!");
    }
}
