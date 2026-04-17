package de.Keyle.MyPet.migration;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.migration.ConfigMigration;
import de.Keyle.MyPet.api.migration.ConfigMigrationContext;
import de.Keyle.MyPet.api.migration.DatabaseMigration;
import de.Keyle.MyPet.api.migration.Migration;
import de.Keyle.MyPet.api.migration.MigrationDomain;
import de.Keyle.MyPet.api.migration.PetDataMigration;
import de.Keyle.MyPet.api.migration.PlayerDataMigration;
import de.Keyle.MyPet.api.migration.SkilltreeMigration;
import de.Keyle.MyPet.api.migration.SkilltreeMigrationContext;
import de.Keyle.MyPet.api.migration.SqlMigrationContext;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.migration.context.ConfigMigrationContextImpl;
import de.Keyle.MyPet.migration.context.SkilltreeMigrationContextImpl;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Load(Load.State.Migration)
@ServiceName("MigrationService")
public class MigrationService implements ServiceContainer {

    private static final String MIGRATIONS_PACKAGE_PATH = "de/Keyle/MyPet/migration/migrations/";
    private static final String INFO_TABLE = "info";
    private static final String MIGRATIONS_TABLE = "migrations";

    private final CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();
    private final Logger logger = MyPetApi.getLogger();

    @Override
    public boolean onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    completionFuture.complete(executeMigrations());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "[MyPet] Unexpected error during migration", e);
                    completionFuture.complete(false);
                }
            }
        }.runTaskAsynchronously(MyPetApi.getPlugin());
        return true;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getServiceName() {
        return "MigrationService";
    }

    /**
     * Blocks until all migrations complete. Returns true if all migrations succeeded.
     */
    public boolean awaitCompletion() {
        try {
            return completionFuture.get();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Error waiting for migrations", e);
            return false;
        }
    }

    private boolean executeMigrations() {
        String prefix = getTablePrefix();

        if (!bootstrapTrackingTable(prefix)) {
            return false;
        }

        InstallType installType = detectInstallType(prefix);
        if (installType == null) {
            return false;
        }

        List<MigrationGraph.MigrationEntry> discovered = discoverMigrations();
        if (discovered == null) {
            return false;
        }

        if (installType == InstallType.FRESH) {
            markAllComplete(discovered, prefix);
            return true;
        }

        if (installType == InstallType.UPGRADE_3X) {
            logger.info("[MyPet] Detected upgrade from 3.x — running migrations...");
        }

        List<MigrationRecord> existingRecords = loadTrackingRecords(prefix);
        if (existingRecords == null) {
            return false;
        }

        for (MigrationRecord record : existingRecords) {
            if (record.getStatus() == MigrationStatus.IN_PROGRESS) {
                logger.severe("[MyPet] Migration " + record.getMigrationId()
                        + " has status IN_PROGRESS from a previous interrupted run.");
                logger.severe("[MyPet] Data may be in an inconsistent state. Manual intervention required.");
                logger.severe("[MyPet] MyPet has been disabled.");
                return false;
            }
        }

        Set<String> completedIds = existingRecords.stream()
                .filter(r -> r.getStatus() == MigrationStatus.COMPLETE)
                .map(MigrationRecord::getMigrationId)
                .collect(Collectors.toSet());

        String currentMcVersion = Bukkit.getMinecraftVersion();

        List<MigrationGraph.MigrationEntry> pending = discovered.stream()
                .filter(e -> !completedIds.contains(e.getId()))
                .filter(e -> !e.hasMinecraftVersion()
                        || MigrationGraph.compareVersions(currentMcVersion, e.getMinecraftVersion()) >= 0)
                .collect(Collectors.toList());

        // Propagate MC-version filtering through dependencies: if a migration depends on one
        // that was excluded (not completed, not in pending), it can't run yet either. Iterate
        // to a fixed point so chains of deps are handled.
        pending = filterUnsatisfiedDependencies(pending, completedIds);

        if (pending.isEmpty()) {
            logger.info("[MyPet] No pending migrations.");
            updateInfoVersion(prefix);
            return true;
        }

        List<MigrationGraph.MigrationEntry> ordered;
        try {
            ordered = MigrationGraph.resolveForPending(discovered, completedIds)
                    .stream()
                    .filter(pending::contains)
                    .collect(Collectors.toList());
        } catch (IllegalStateException e) {
            logger.severe("[MyPet] " + e.getMessage());
            logger.severe("[MyPet] MyPet has been disabled. Please fix the issue and restart.");
            return false;
        }

        MigrationBackupService backupService = new MigrationBackupService(logger);
        if (!performBackups(backupService, ordered, prefix)) {
            return false;
        }
        logger.info("[MyPet] Backups saved to: " + backupService.getBackupDir().getPath());

        long totalStart = System.currentTimeMillis();
        int applied = 0;

        for (MigrationGraph.MigrationEntry entry : ordered) {
            insertTrackingRecord(entry, prefix);

            String versionLabel = entry.hasVersion() ? entry.getVersion() : "MC " + entry.getMinecraftVersion();
            logger.info("[MyPet] Running migration: " + entry.getId()
                    + " (" + versionLabel + ", " + entry.getDomain() + ")...");

            long start = System.currentTimeMillis();
            try {
                executeMigration(entry);
                long elapsed = System.currentTimeMillis() - start;
                updateTrackingRecord(entry.getId(), MigrationStatus.COMPLETE, elapsed, null, prefix);
                logger.info("OK (" + elapsed + "ms)");
                applied++;
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                updateTrackingRecord(entry.getId(), MigrationStatus.FAILED, elapsed, errorMsg, prefix);
                logger.severe("FAILED");
                logger.severe("[MyPet] Migration failed: " + entry.getId());
                logger.log(Level.SEVERE, "[MyPet] " + e, e);
                logger.severe("[MyPet] A backup was created at: " + backupService.getBackupDir().getPath());
                logger.severe("[MyPet] MyPet has been disabled due to a migration failure. "
                        + "Please check the error above and restart.");
                return false;
            }
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        logger.info("[MyPet] All migrations complete (" + applied + " applied, " + totalElapsed + "ms total)");

        updateInfoVersion(prefix);
        return true;
    }

    /**
     * Removes migrations whose {@code dependsOn} targets are neither completed nor in the
     * pending set — those targets were filtered out (typically by MC version gating), so the
     * dependent migration cannot run yet. Iterates to a fixed point to handle dep chains.
     */
    private List<MigrationGraph.MigrationEntry> filterUnsatisfiedDependencies(
            List<MigrationGraph.MigrationEntry> pending, Set<String> completedIds) {
        List<MigrationGraph.MigrationEntry> current = new ArrayList<>(pending);
        while (true) {
            Set<String> pendingIds = current.stream()
                    .map(MigrationGraph.MigrationEntry::getId)
                    .collect(Collectors.toSet());
            List<MigrationGraph.MigrationEntry> next = new ArrayList<>(current.size());
            for (MigrationGraph.MigrationEntry entry : current) {
                boolean depsSatisfied = true;
                for (String dep : entry.getAnnotation().dependsOn()) {
                    if (!completedIds.contains(dep) && !pendingIds.contains(dep)) {
                        logger.info("[MyPet] Deferring migration " + entry.getId()
                                + " — dependency " + dep + " is not available on this server yet.");
                        depsSatisfied = false;
                        break;
                    }
                }
                if (depsSatisfied) {
                    next.add(entry);
                }
            }
            if (next.size() == current.size()) {
                return next;
            }
            current = next;
        }
    }

    private void executeMigration(MigrationGraph.MigrationEntry entry) throws Exception {
        Object migration = entry.getMigrationClass().getDeclaredConstructor().newInstance();

        if (migration instanceof DatabaseMigration dbm) {
            dbm.migrateSql(createSqlContext());
        } else if (migration instanceof ConfigMigration cm) {
            cm.migrate(createConfigContext());
        } else if (migration instanceof SkilltreeMigration sm) {
            sm.migrate(createSkilltreeContext());
        } else if (migration instanceof PetDataMigration pdm) {
            pdm.migrateSql(createSqlContext());
        } else if (migration instanceof PlayerDataMigration plm) {
            plm.migrateSql(createSqlContext());
        }
    }

    // --- Discovery & configuration helpers ---

    private String getTablePrefix() {
        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            return Configuration.Repository.MySQL.PREFIX;
        }
        return "";
    }

    private boolean isSqliteRepo() {
        return !Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL");
    }

    private MigrationDomain inferDomain(Class<?> clazz) {
        EnumSet<MigrationDomain> matches = EnumSet.noneOf(MigrationDomain.class);
        if (DatabaseMigration.class.isAssignableFrom(clazz)) matches.add(MigrationDomain.DATABASE);
        if (ConfigMigration.class.isAssignableFrom(clazz)) matches.add(MigrationDomain.CONFIG);
        if (SkilltreeMigration.class.isAssignableFrom(clazz)) matches.add(MigrationDomain.SKILLTREE);
        if (PetDataMigration.class.isAssignableFrom(clazz)) matches.add(MigrationDomain.PET_DATA);
        if (PlayerDataMigration.class.isAssignableFrom(clazz)) matches.add(MigrationDomain.PLAYER_DATA);
        if (matches.size() != 1) {
            return null;
        }
        return matches.iterator().next();
    }

    private List<MigrationGraph.MigrationEntry> discoverMigrations() {
        List<MigrationGraph.MigrationEntry> entries = new ArrayList<>();
        try {
            File jarFile = new File(MyPetApi.getPlugin().getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) {
                return scanMigrationsFromClassLoader();
            }
            ClassLoader classLoader = MyPetApi.getPlugin().getClass().getClassLoader();
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> jarEntries = jar.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement();
                    String name = jarEntry.getName();
                    if (!name.startsWith(MIGRATIONS_PACKAGE_PATH)
                            || !name.endsWith(".class")
                            || name.contains("$")) {
                        continue;
                    }
                    String className = name.replace('/', '.').replace(".class", "");
                    MigrationGraph.MigrationEntry entry = loadMigrationEntry(className, classLoader);
                    if (entry == null) {
                        continue;
                    }
                    if (entry == SENTINEL_ERROR) {
                        return null;
                    }
                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to discover migrations", e);
            return null;
        }
        return entries;
    }

    private static final MigrationGraph.MigrationEntry SENTINEL_ERROR =
            new MigrationGraph.MigrationEntry(Object.class, null, MigrationDomain.DATABASE) {
            };

    private MigrationGraph.MigrationEntry loadMigrationEntry(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            if (!clazz.isAnnotationPresent(Migration.class)) {
                return null;
            }
            Migration annotation = clazz.getAnnotation(Migration.class);
            if (annotation.version().isEmpty() && annotation.minecraftVersion().isEmpty()) {
                logger.severe("[MyPet] Migration " + clazz.getSimpleName()
                        + " must have at least one of version or minecraftVersion set.");
                return SENTINEL_ERROR;
            }
            MigrationDomain domain = inferDomain(clazz);
            if (domain == null) {
                logger.severe("[MyPet] Migration " + clazz.getSimpleName()
                        + " must implement exactly one domain interface.");
                return SENTINEL_ERROR;
            }
            return new MigrationGraph.MigrationEntry(clazz, annotation, domain);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to load migration class: " + className, e);
            return SENTINEL_ERROR;
        }
    }

    private List<MigrationGraph.MigrationEntry> scanMigrationsFromClassLoader() {
        return Collections.emptyList();
    }

    // --- Context creation ---

    private SqlMigrationContext createSqlContext() throws SQLException {
        Repository repo = MyPetApi.getRepository();
        Connection connection;
        if (repo instanceof SqLiteRepository sqlite) {
            connection = sqlite.openNewConnection();
        } else if (repo instanceof MySqlRepository mysql) {
            connection = mysql.getConnection();
        } else {
            throw new IllegalStateException("SQL migration requested but active repository is not SQL-backed: "
                    + (repo == null ? "null" : repo.getClass().getSimpleName()));
        }
        return new SqlMigrationContextImpl(connection, getTablePrefix());
    }

    private ConfigMigrationContext createConfigContext() {
        return new ConfigMigrationContextImpl(MyPetApi.getPlugin().getDataFolder());
    }

    private SkilltreeMigrationContext createSkilltreeContext() {
        return new SkilltreeMigrationContextImpl(
                new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"));
    }

    // --- Tracking table operations ---

    private boolean bootstrapTrackingTable(String prefix) {
        try (Connection connection = openSqlConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + prefix + MIGRATIONS_TABLE + " ("
                    + "migration_id VARCHAR(255) PRIMARY KEY,"
                    + "version VARCHAR(50) NOT NULL,"
                    + "domain VARCHAR(50) NOT NULL,"
                    + "applied_at BIGINT NOT NULL,"
                    + "status VARCHAR(20) NOT NULL,"
                    + "execution_time_ms BIGINT DEFAULT 0,"
                    + "error_message TEXT"
                    + ")");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to bootstrap migrations tracking table", e);
            return false;
        }
    }

    /**
     * Opens a fresh SQL connection to the active repository. The caller owns the returned
     * connection and must close it.
     */
    private Connection openSqlConnection() throws SQLException {
        Repository repo = MyPetApi.getRepository();
        if (repo instanceof SqLiteRepository sqlite) {
            return sqlite.openNewConnection();
        }
        if (repo instanceof MySqlRepository mysql) {
            return mysql.getConnection();
        }
        throw new IllegalStateException("No SQL repository active");
    }

    private InstallType detectInstallType(String prefix) {
        try (Connection connection = openSqlConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT mypet_version FROM " + prefix + INFO_TABLE + " LIMIT 1")) {
            if (!rs.next()) {
                return InstallType.FRESH;
            }
            return classifyInstall(rs.getString(1));
        } catch (SQLException e) {
            if (isTableNotFound(e)) {
                return InstallType.FRESH;
            }
            logger.log(Level.SEVERE, "[MyPet] Failed to detect install type "
                    + "(SQLState=" + e.getSQLState() + ", errorCode=" + e.getErrorCode() + ")", e);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to detect install type", e);
            return null;
        }
    }

    /**
     * Heuristic for "table or column does not exist" across SQLite and MySQL. We only want to
     * treat that case as a fresh install — lock errors, I/O errors, and corruption should fail
     * loudly so the operator doesn't silently have all migrations marked COMPLETE on bad data.
     */
    private boolean isTableNotFound(SQLException e) {
        String sqlState = e.getSQLState();
        if ("42S02".equals(sqlState) || "42S22".equals(sqlState)) {
            return true;
        }
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("no such table") || lower.contains("no such column");
    }

    private InstallType classifyInstall(String version) {
        if (version == null || version.isEmpty() || version.startsWith("3.")) {
            return InstallType.UPGRADE_3X;
        }
        return InstallType.NORMAL_4X;
    }

    private List<MigrationRecord> loadTrackingRecords(String prefix) {
        List<MigrationRecord> records = new ArrayList<>();
        try (Connection connection = openSqlConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT migration_id, version, domain, applied_at, "
                     + "status, execution_time_ms, error_message FROM " + prefix + MIGRATIONS_TABLE)) {
            while (rs.next()) {
                records.add(new MigrationRecord(
                        rs.getString(1),
                        rs.getString(2),
                        MigrationDomain.valueOf(rs.getString(3)),
                        rs.getLong(4),
                        MigrationStatus.valueOf(rs.getString(5)),
                        rs.getLong(6),
                        rs.getString(7)));
            }
            return records;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to load tracking records", e);
            return null;
        }
    }

    private void insertTrackingRecord(MigrationGraph.MigrationEntry entry, String prefix) {
        MigrationRecord record = new MigrationRecord(
                entry.getId(),
                entry.hasVersion() ? entry.getVersion() : "MC-" + entry.getMinecraftVersion(),
                entry.getDomain());
        try (Connection connection = openSqlConnection()) {
            // Upsert: DELETE then INSERT in a single transaction so a crash between
            // them doesn't erase the tracking row and mask IN_PROGRESS state on retry.
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (var ps = connection.prepareStatement("DELETE FROM " + prefix + MIGRATIONS_TABLE
                        + " WHERE migration_id = ?")) {
                    ps.setString(1, record.getMigrationId());
                    ps.executeUpdate();
                }
                try (var ps = connection.prepareStatement("INSERT INTO " + prefix + MIGRATIONS_TABLE
                        + " (migration_id, version, domain, applied_at, status, execution_time_ms, error_message) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, record.getMigrationId());
                    ps.setString(2, record.getVersion());
                    ps.setString(3, record.getDomain().name());
                    ps.setLong(4, record.getAppliedAt());
                    ps.setString(5, record.getStatus().name());
                    ps.setLong(6, record.getExecutionTimeMs());
                    ps.setString(7, record.getErrorMessage());
                    ps.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to insert tracking record for " + entry.getId(), e);
        }
    }

    private void updateTrackingRecord(String id, MigrationStatus status, long timeMs,
                                       String errorMsg, String prefix) {
        try (Connection connection = openSqlConnection();
             var ps = connection.prepareStatement("UPDATE " + prefix + MIGRATIONS_TABLE
                     + " SET status = ?, execution_time_ms = ?, error_message = ? WHERE migration_id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, timeMs);
            ps.setString(3, errorMsg);
            ps.setString(4, id);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Failed to update tracking record for " + id, e);
        }
    }

    private void markAllComplete(List<MigrationGraph.MigrationEntry> entries, String prefix) {
        long now = System.currentTimeMillis();
        for (MigrationGraph.MigrationEntry entry : entries) {
            try (Connection connection = openSqlConnection();
                 var ps = connection.prepareStatement("INSERT INTO " + prefix + MIGRATIONS_TABLE
                         + " (migration_id, version, domain, applied_at, status, execution_time_ms, error_message) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.hasVersion() ? entry.getVersion() : "MC-" + entry.getMinecraftVersion());
                ps.setString(3, entry.getDomain().name());
                ps.setLong(4, now);
                ps.setString(5, MigrationStatus.COMPLETE.name());
                ps.setLong(6, 0L);
                ps.setString(7, null);
                ps.executeUpdate();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[MyPet] Failed to mark migration complete on fresh install: "
                        + entry.getId(), e);
            }
        }
        updateInfoVersion(prefix);
    }

    private void updateInfoVersion(String prefix) {
        String version = MyPetVersion.getVersion();
        try (Connection connection = openSqlConnection()) {
            int updated;
            try (var ps = connection.prepareStatement(
                    "UPDATE " + prefix + INFO_TABLE + " SET mypet_version = ?")) {
                ps.setString(1, version);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (var ps = connection.prepareStatement(
                        "INSERT INTO " + prefix + INFO_TABLE + " (mypet_version) VALUES (?)")) {
                    ps.setString(1, version);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "[MyPet] Failed to update info.mypet_version — "
                    + "migrations completed but version row could not be updated", e);
        }
    }

    // --- Backup coordination ---

    private boolean performBackups(MigrationBackupService backupService,
                                    List<MigrationGraph.MigrationEntry> ordered,
                                    String prefix) {
        String version = MyPetVersion.getVersion();
        backupService.initBackupDir(version);

        EnumSet<MigrationDomain> pendingDomains = EnumSet.noneOf(MigrationDomain.class);
        for (MigrationGraph.MigrationEntry entry : ordered) {
            pendingDomains.add(entry.getDomain());
        }

        boolean needsDatabaseBackup = pendingDomains.contains(MigrationDomain.DATABASE)
                || pendingDomains.contains(MigrationDomain.PET_DATA)
                || pendingDomains.contains(MigrationDomain.PLAYER_DATA);

        try {
            if (needsDatabaseBackup) {
                if (isSqliteRepo()) {
                    File dbFile = new File(MyPetApi.getPlugin().getDataFolder(), "pets.db");
                    if (dbFile.exists()) {
                        backupService.backupSqliteFile(dbFile);
                    } else {
                        logger.info("[MyPet] SQLite file not found at " + dbFile.getPath()
                                + " — skipping database backup");
                    }
                } else {
                    try (Connection connection = openSqlConnection()) {
                        backupService.backupMysqlTables(connection, prefix);
                    }
                }
            }
            if (pendingDomains.contains(MigrationDomain.CONFIG)) {
                backupService.backupConfigFiles(MyPetApi.getPlugin().getDataFolder());
            }
            if (pendingDomains.contains(MigrationDomain.SKILLTREE)) {
                backupService.backupSkilltreeFiles(
                        new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"));
            }
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MyPet] Backup failed — aborting migration to protect data", e);
            return false;
        }
    }

    private enum InstallType {
        FRESH, UPGRADE_3X, NORMAL_4X
    }
}
