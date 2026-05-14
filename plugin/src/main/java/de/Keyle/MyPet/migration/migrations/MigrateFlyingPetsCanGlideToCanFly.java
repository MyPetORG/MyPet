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

package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.migration.ConfigMigration;
import de.Keyle.MyPet.migration.ConfigMigrationContext;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

/**
 * Renames the pre-4.x {@code CanGlide} key on flying pets to {@code CanFly}.
 *
 * <p>Old configs used one boolean per flying pet — labelled {@code CanGlide}
 * — to mean "can fly". v4 renamed that key to {@code CanFly} for clarity. For
 * each pet that now implements {@link PetFlyingEntity}, this migration:
 *
 * <ol>
 *   <li>Reads the legacy {@code CanGlide} value (if present) from
 *       {@code pet-config.yml}</li>
 *   <li>Writes it as {@code CanFly} (preserving the admin's "no flight" intent)</li>
 *   <li>Deletes the legacy {@code CanGlide} entry</li>
 *   <li>Updates the in-memory {@link Configuration.MyPet} CAN_FLY map so the
 *       current boot reflects the migrated state without requiring a restart</li>
 * </ol>
 *
 * <p>Idempotent: once the legacy key is gone for a flying pet, this migration
 * is a no-op for that pet on subsequent runs. Non-flying pets that previously
 * had a {@code CanGlide} row (e.g., Chicken) keep that orphan key in their
 * YAML — Bukkit ignores unknown keys, and removing it would be a pure
 * deletion that doesn't warrant a migration.
 */
@Migration(
        version = "4.0.0",
        description = "Split pre-4.x CanGlide on flying pets into CanFly + CanGlide"
)
public class MigrateFlyingPetsCanGlideToCanFly implements ConfigMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");
    private static final String PET_CONFIG = "pet-config.yml";

    @Override
    public void migrate(ConfigMigrationContext ctx) throws MigrationException {
        if (!new File(ctx.getDataFolder(), PET_CONFIG).exists()) {
            return;
        }
        YamlConfiguration config = ctx.getConfig(PET_CONFIG);

        boolean changed = false;
        int converted = 0;
        for (PetType type : PetType.values()) {
            if (!PetFlyingEntity.class.isAssignableFrom(type.getPetClass())) {
                continue;
            }
            String base = "MyPet.Pets." + type.name();
            String legacyKey = base + ".CanGlide";
            String newKey = base + ".CanFly";

            if (!config.contains(legacyKey)) {
                continue;
            }

            boolean legacyValue = config.getBoolean(legacyKey);
            config.set(newKey, legacyValue);
            config.set(legacyKey, null);

            // Sync the runtime map so the change takes effect without a restart.
            // setDefault/loadConfiguration already populated this from the pre-migration
            // file state; overwrite with the authoritative migrated value.
            Configuration.MyPet.setCanFly(type.name(), legacyValue);

            changed = true;
            converted++;
            LOG.info("Migrated " + base + ".CanGlide → CanFly = " + legacyValue);
        }

        if (changed) {
            ctx.saveConfig(PET_CONFIG);
        }
        LOG.info("Flying-pet CanGlide → CanFly migration complete (" + converted + " converted).");
    }
}
