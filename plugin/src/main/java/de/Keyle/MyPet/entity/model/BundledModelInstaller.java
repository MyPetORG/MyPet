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

package de.Keyle.MyPet.entity.model;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.entity.model.PetModelService.ModelConfig;
import de.Keyle.MyPet.util.ResourceUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Installs MyPet's bundled default models (capybara/chameleon) into the chosen
 * ModelEngine/BetterModel provider when a pet-config references them, so the
 * "MyPet default" wizard option (and hand-edited configs) just work. Idempotent:
 * a model already present in the provider folder is left untouched.
 */
public final class BundledModelInstaller {

    private BundledModelInstaller() {}

    private static final Set<String> BUNDLED = Set.of("capybara", "chameleon");

    /**
     * Scans registered model configs; for any whose id is a bundled default and whose
     * provider is ModelEngine/BetterModel, copies the bundled {@code .bbmodel} into the
     * provider's folder if absent, then dispatches that provider's reload once. Fully
     * defensive — a failure to install one model never throws out of this method.
     */
    public static void installReferencedDefaults() {
        try {
            Set<String> reloaded = new HashSet<>();
            for (ModelConfig cfg : PetModelService.configs()) {
                String id = cfg.modelId();
                if (id == null) {
                    continue;
                }
                String idLower = id.toLowerCase(Locale.ROOT);
                if (!BUNDLED.contains(idLower)) {
                    continue;
                }
                String provider = cfg.provider();
                if (provider == null
                        || !(provider.equalsIgnoreCase("ModelEngine") || provider.equalsIgnoreCase("BetterModel"))) {
                    continue;
                }
                File dir = providerModelDir(provider);
                if (dir == null) {
                    continue; // provider not installed → skip
                }
                File target = new File(dir, idLower + ".bbmodel");
                if (target.exists()) {
                    continue; // idempotent
                }
                dir.mkdirs();
                if (ResourceUtil.copyResource(MyPetApi.getPlugin(), "models/" + idLower + ".bbmodel", target)) {
                    reloaded.add(provider);
                    MyPetApi.getLogger().info("Installed bundled model '" + idLower + "' into " + provider + ".");
                }
            }
            for (String provider : reloaded) {
                Bukkit.getGlobalRegionScheduler().run(MyPetApi.getPlugin(), t ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                provider.equalsIgnoreCase("ModelEngine") ? "meg reload" : "bettermodel reload"));
            }
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("Failed to install bundled default models: " + t.getMessage());
        }
    }

    /** Resolves the provider's model folder (ModelEngine → {@code blueprints}, BetterModel → {@code models}), or null if not installed. */
    private static File providerModelDir(String provider) {
        Plugin p = Bukkit.getPluginManager().getPlugin(provider);
        if (p == null) {
            return null;
        }
        String sub = provider.equalsIgnoreCase("ModelEngine") ? "blueprints" : "models";
        return new File(p.getDataFolder(), sub);
    }
}
