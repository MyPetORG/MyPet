package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPetFlyingEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.migration.ConfigMigration;
import de.Keyle.MyPet.migration.ConfigMigrationContext;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

/**
 * Splits the pre-4.x single {@code CanGlide} key on flying pets into the new
 * {@code CanFly} (free upward thrust) and {@code CanGlide} (slow-fall) pair.
 *
 * <p>Old configs used one boolean per flying pet — labelled {@code CanGlide}
 * — to mean "can fly". v4 separates the two capabilities because a rider on a
 * flight-disabled pet still needs the pet to slow-fall, otherwise both pet
 * and rider plummet. For each pet that now implements {@link MyPetFlyingEntity},
 * this migration:
 *
 * <ol>
 *   <li>Reads the legacy {@code CanGlide} value (if present) from
 *       {@code pet-config.yml}</li>
 *   <li>Writes it as {@code CanFly} (preserving the admin's "no flight" intent)</li>
 *   <li>Deletes the legacy {@code CanGlide} entry so the next
 *       {@code ConfigurationLoader.setDefault()} writes the new {@code CanGlide:
 *       true} default for that pet</li>
 *   <li>Updates the in-memory {@link Configuration.MyPet} maps so the current
 *       boot reflects the migrated state without requiring a restart</li>
 * </ol>
 *
 * <p>Idempotent: once the legacy key is gone for a flying pet, this migration
 * is a no-op for that pet on subsequent runs. Chicken's {@code CanGlide} is
 * intentionally untouched — that key was always a glide-only toggle and keeps
 * its meaning under the new model.
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
        for (MyPetType type : MyPetType.values()) {
            if (!MyPetFlyingEntity.class.isAssignableFrom(type.getMyPetClass())) {
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

            // Sync the runtime maps so the change takes effect without a restart.
            // setDefault/loadConfiguration already populated these from the pre-migration
            // file state; overwrite with the authoritative migrated values.
            Configuration.MyPet.setCanFly(type.name(), legacyValue);
            Configuration.MyPet.setCanGlide(type.name(), true);

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
