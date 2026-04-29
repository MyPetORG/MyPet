package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.migration.Migration;
import de.Keyle.MyPet.api.migration.MigrationException;
import de.Keyle.MyPet.api.migration.PetDataMigration;
import de.Keyle.MyPet.api.migration.SqlMigrationContext;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.migration.migrations.entitysnapshot.LegacyPetReader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * One-shot startup migration that converts pre-v4 {@code info} BLOBs
 * (legacy curated NBT) into the EntitySnapshot envelope format. Idempotent:
 * re-running skips rows whose info already contains a {@code schema_version}
 * key.
 *
 * <p>Per-pet conversion: spawn a transient mob at a high-altitude hidden
 * location, apply the legacy compound via
 * {@link LegacyPetReader#applyToMob}, capture vanilla NBT via
 * {@link PetEntitySnapshot#capture}, write the envelope back through the
 * repository, remove the transient mob.
 *
 * <p><b>Threading.</b> Integrates with {@code MigrationService} via the
 * {@link PetDataMigration} interface. {@code migrateSql} runs synchronously
 * on the migration phase's calling thread (main thread on Paper non-Folia,
 * global region thread on Folia). For each pet the per-region scheduler
 * dispatch is conditional:
 *
 * <ul>
 *   <li>If we already own the target region (Paper non-Folia main thread,
 *       or Folia happening to land on this region), the work runs inline —
 *       avoids scheduling to a queue that will not drain while the main
 *       thread is held inside {@code migrateSql}.</li>
 *   <li>Otherwise (Folia, target world's region differs from caller's),
 *       dispatch via {@code Bukkit.getRegionScheduler().run} and await on a
 *       {@link CompletableFuture} — safe because Folia region threads run
 *       independently of the global thread we're blocking on.</li>
 * </ul>
 *
 * <p>The {@link SqlMigrationContext} parameter is unused: pet info reads and
 * writes go through {@code MyPetApi.getRepository()} so the high-level
 * {@code StoredMyPet} model is preserved (ctx exposes a raw connection that
 * would require duplicating the GZIP+NBT codec).
 */
@Migration(
        version = "4.0.0",
        description = "Convert legacy per-mob-type pet info to vanilla NBT EntitySnapshot envelope"
)
public final class EntitySnapshotMigration implements PetDataMigration {

    private static final long PER_PET_TIMEOUT_SECONDS = 30L;

    private final Logger logger = MyPetApi.getLogger();

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        JavaPlugin plugin = (JavaPlugin) MyPetApi.getPlugin();
        Repository repository = MyPetApi.getRepository();
        if (repository == null) {
            throw new MigrationException("Repository is null at EntitySnapshot migration time");
        }

        List<StoredMyPet> all = repository.getAllPets();
        List<StoredMyPet> legacy = all.stream()
                .filter(p -> !p.getInfo().keySet().contains("schema_version"))
                .toList();

        if (legacy.isEmpty()) {
            logger.info("EntitySnapshot: 0 legacy pets to convert.");
            return;
        }

        logger.info("EntitySnapshot: converting " + legacy.size()
                + " legacy pet(s) to envelope format.");

        int successes = 0;
        int failures = 0;
        for (StoredMyPet pet : legacy) {
            if (convertOne(plugin, repository, pet)) {
                successes++;
            } else {
                failures++;
            }
        }

        logger.info("EntitySnapshot: " + successes + " converted, "
                + failures + " failed, " + legacy.size() + " total.");

        if (failures > 0) {
            throw new MigrationException(failures + " pet(s) failed to convert; "
                    + "see prior log warnings for per-pet details. Migration aborted.");
        }
    }

    private boolean convertOne(JavaPlugin plugin, Repository repository, StoredMyPet pet) {
        World world = Bukkit.getWorlds().stream().findFirst().orElse(null);
        if (world == null) {
            logger.warning("EntitySnapshot: pet " + pet.getUUID()
                    + " has no resolvable world; skipping.");
            return false;
        }

        Location hiddenLoc = new Location(world,
                world.getSpawnLocation().getX(),
                world.getMaxHeight() - 1,
                world.getSpawnLocation().getZ());

        if (Bukkit.isOwnedByCurrentRegion(hiddenLoc)) {
            return doConvert(repository, pet, world, hiddenLoc);
        }

        // Folia: dispatch onto the region's thread and block on completion.
        // Safe because region threads run independently of our caller thread.
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Bukkit.getRegionScheduler().run(plugin, hiddenLoc, task -> {
            try {
                result.complete(doConvert(repository, pet, world, hiddenLoc));
            } catch (Throwable t) {
                logger.warning("EntitySnapshot: pet " + pet.getUUID()
                        + " region task threw: " + t.getMessage());
                result.complete(false);
            }
        });
        try {
            return result.get(PER_PET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            logger.warning("EntitySnapshot: pet " + pet.getUUID()
                    + " conversion timed out after " + PER_PET_TIMEOUT_SECONDS + "s.");
            return false;
        } catch (Exception e) {
            logger.warning("EntitySnapshot: pet " + pet.getUUID()
                    + " await failed: " + e.getMessage());
            return false;
        }
    }

    private boolean doConvert(Repository repository, StoredMyPet pet,
                               World world, Location hiddenLoc) {
        try {
            Class<? extends Mob> mobClass = pet.getPetType().getBukkitEntityClass();
            if (mobClass == null) {
                logger.warning("EntitySnapshot: pet " + pet.getUUID()
                        + " has unknown type " + pet.getPetType().name() + "; skipping.");
                return false;
            }

            Mob transientMob = world.spawn(hiddenLoc, mobClass,
                    CreatureSpawnEvent.SpawnReason.CUSTOM, m -> {
                        m.setPersistent(false);
                        m.setRemoveWhenFarAway(false);
                        m.setAI(false);
                        m.setSilent(true);
                        m.setInvulnerable(true);
                        m.setInvisible(true);
                    });

            try {
                LegacyPetReader.applyToMob(transientMob, pet.getPetType(), pet.getInfo());
                byte[] snapshot = PetEntitySnapshot.capture(transientMob);

                CompoundBinaryTag legacyStorage = pet.getInfo().keySet().contains("storage")
                        ? pet.getInfo().getCompound("storage")
                        : CompoundBinaryTag.empty();
                pet.setInfo(PetEntitySnapshot.envelope(snapshot, legacyStorage));
                repository.updatePet(pet).join();
                return true;
            } finally {
                transientMob.remove();
            }
        } catch (Throwable t) {
            logger.warning("EntitySnapshot: pet " + pet.getUUID()
                    + " (" + pet.getPetType().name() + ") failed: " + t.getMessage());
            return false;
        }
    }
}
