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
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.entity.types.ModelPet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CustomPetLoader {

    private CustomPetLoader() {}

    /**
     * Reads pet-config.yml from disk and registers a {@link ModelPet} type for every
     * {@code MyPet.Pets.<Name>} section that has a non-empty {@code Host} key and is
     * not already a known {@link PetType}. Returns immediately if the file does not
     * exist (first boot — no admin-authored custom types yet).
     */
    public static void registerCustomTypes() {
        File file = new File(MyPetApi.getPlugin().getDataFolder(), "pet-config.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection pets = config.getConfigurationSection("MyPet.Pets");
        if (pets == null) {
            return;
        }
        // A model is a custom creature's identity: leashing a wild modeled mob resolves back to its
        // pet type by (Provider, Id), so two types sharing the same model would make that ambiguous.
        // Reject the later duplicate (first-in-file wins). Keyed on the lower-cased Provider+Id, so
        // it also covers source-driven types (Provider: MythicMobs, Id: <mob>).
        Map<String, String> claimedModels = new HashMap<>();
        // Type names differing only in case (e.g. FrostDragon / Frostdragon) normalise to the same
        // registered type, so the later one would be silently dropped. Track case-normalised names
        // seen in THIS pass and warn on an in-pass collision (distinct from the harmless reload
        // re-encounter of the same section, which the existing-type branch below already handles).
        Set<String> seenNames = new HashSet<>();

        // Pre-seed claimedModels from types already registered (a prior load pass) so an existing
        // owner always claims its (Provider, Id) first, regardless of where a NEW duplicate section
        // happens to fall in this file's iteration order.
        for (String name : pets.getKeys(false)) {
            PetType existing = PetType.byNameOrNull(name);
            if (existing == null || existing.getPetClass() != ModelPet.class) {
                continue;
            }
            String modelKey = modelKeyOf(pets.getConfigurationSection(name));
            if (modelKey != null) {
                claimedModels.putIfAbsent(modelKey, name);
            }
        }

        for (String name : pets.getKeys(false)) {
            ConfigurationSection section = pets.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            String hostName = section.getString("Host", "");
            if (hostName.isEmpty()) {
                continue;
            }
            if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
                MyPetApi.getLogger().warning("custom-pets: '" + name
                        + "' duplicates another type's name (case-insensitive) — skipped");
                continue;
            }
            String provider = section.getString("Model.Provider", "");
            String modelId = section.getString("Model.Id", "");
            String modelKey = modelKeyOf(section);

            PetType existing = PetType.byNameOrNull(name);
            if (existing != null) {
                if (existing.getPetClass() != ModelPet.class) {
                    MyPetApi.getLogger().warning("custom-pets: '" + name + "' collides with an existing non-custom pet type — skipped");
                } else if (modelKey != null) {
                    // Already-registered custom type owns its model, so a later duplicate is rejected.
                    claimedModels.putIfAbsent(modelKey, name);
                }
                continue;
            }
            Class<? extends Mob> hostClass = resolveHost(hostName);
            if (hostClass == null) {
                MyPetApi.getLogger().warning("custom-pets: '" + name + "' has invalid host '" + hostName + "' — skipped");
                continue;
            }
            if (modelKey != null) {
                String owner = claimedModels.get(modelKey);
                if (owner != null) {
                    MyPetApi.getLogger().warning("custom-pets: '" + name + "' duplicates the model of '" + owner
                            + "' (" + provider + ":" + modelId + ") — skipped. Give each custom type a unique Model.Id.");
                    continue;
                }
                claimedModels.put(modelKey, name);
            }
            PetType.register(name, ModelPet.class, hostClass);
        }
    }

    /** {@code Provider|Id} dedup key for a section's {@code Model} block, or null if either is unset. */
    private static String modelKeyOf(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String provider = section.getString("Model.Provider", "");
        String modelId = section.getString("Model.Id", "");
        return (!provider.isEmpty() && !modelId.isEmpty())
                ? provider.toLowerCase(Locale.ROOT) + "|" + modelId.toLowerCase(Locale.ROOT)
                : null;
    }

    private static Class<? extends Mob> resolveHost(String hostName) {
        try {
            Class<?> cls = EntityType.valueOf(hostName.toUpperCase(Locale.ROOT)).getEntityClass();
            if (cls != null && Mob.class.isAssignableFrom(cls)) {
                return cls.asSubclass(Mob.class);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }
}
