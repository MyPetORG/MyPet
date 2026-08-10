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

import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.config.ConfigKeyRegistry;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetType;
import de.Keyle.MyPet.migration.ConfigMigration;
import de.Keyle.MyPet.migration.ConfigMigrationContext;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

/**
 * Moves the orphaned global {@code MyPet.RetainEquipmentOnTame} key out of
 * {@code config.yml} and onto the per-pet-type keys that replaced it.
 *
 * <p>The global key was declared as a {@code ConfigKey} but never read by any
 * code, so it was written into every server's {@code config.yml} and did
 * nothing. 4.0.1 replaced it with a real per-type option,
 * {@code MyPet.Pets.<Type>.RetainEquipmentOnTame}, on every pet type that can
 * wear equipment.
 *
 * <p>Leaving the dead key in place would be worse than merely untidy: it has
 * the same name as the working option, so an admin who found it would read it
 * as a master switch, flip it, and see no effect. This migration therefore:
 *
 * <ol>
 *   <li>Copies the admin's global value onto every equipment-capable pet type,
 *       so someone who had set it to {@code false} gets the behavior they
 *       clearly intended rather than silently reverting to the default</li>
 *   <li>Deletes the global key from {@code config.yml}</li>
 *   <li>Updates each type's in-memory {@link ConfigKey} so the migrated value
 *       applies on this boot without needing a restart</li>
 * </ol>
 *
 * <p>Idempotent: once the global key is gone the migration is a no-op, and it
 * does nothing at all on servers that never had the key.
 */
@Migration(
        version = "4.0.1",
        description = "Move the dead global RetainEquipmentOnTame onto the per-pet-type keys"
)
public class MigrateGlobalRetainEquipmentToPerPet implements ConfigMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");
    private static final String MAIN_CONFIG = "config.yml";
    private static final String PET_CONFIG = "pet-config.yml";
    private static final String GLOBAL_KEY = "MyPet.RetainEquipmentOnTame";
    private static final String PET_KEY = "RetainEquipmentOnTame";

    @Override
    public void migrate(ConfigMigrationContext ctx) throws MigrationException {
        if (!new File(ctx.getDataFolder(), MAIN_CONFIG).exists()) {
            return;
        }
        YamlConfiguration mainConfig = ctx.getConfig(MAIN_CONFIG);
        if (!mainConfig.contains(GLOBAL_KEY)) {
            return;
        }

        boolean globalValue = mainConfig.getBoolean(GLOBAL_KEY, true);

        // Port the value first, so a failure here leaves the old key in place
        // rather than dropping the admin's setting on the floor.
        int ported = 0;
        if (new File(ctx.getDataFolder(), PET_CONFIG).exists()) {
            YamlConfiguration petConfig = ctx.getConfig(PET_CONFIG);
            for (PetType type : PetType.values()) {
                if (!PetEquipment.class.isAssignableFrom(type.getPetClass())) {
                    continue;
                }
                petConfig.set("MyPet.Pets." + type.name() + "." + PET_KEY, globalValue);

                // ConfigKeyRegistry.loadFromYaml already ran against the
                // pre-migration file, so push the migrated value through the
                // key's own update path to keep memory and YAML in step.
                ConfigKey<?> ck = ConfigKeyRegistry.lookup(type.name(), PET_KEY);
                if (ck != null && ck.defaultValue() instanceof Boolean) {
                    @SuppressWarnings("unchecked")
                    ConfigKey<Boolean> boolKey = (ConfigKey<Boolean>) ck;
                    boolKey.update(globalValue);
                }
                ported++;
            }
            if (ported > 0) {
                ctx.saveConfig(PET_CONFIG);
            }
        } else {
            LOG.warning("pet-config.yml is missing — removing the dead global "
                    + GLOBAL_KEY + " without porting its value. The per-pet defaults "
                    + "(" + PET_KEY + ": true) will be written on this boot.");
        }

        mainConfig.set(GLOBAL_KEY, null);
        ctx.saveConfig(MAIN_CONFIG);

        LOG.info("Removed the unused global " + GLOBAL_KEY + " (was " + globalValue
                + ") and applied it to " + ported + " equipment-capable pet types.");
    }
}
