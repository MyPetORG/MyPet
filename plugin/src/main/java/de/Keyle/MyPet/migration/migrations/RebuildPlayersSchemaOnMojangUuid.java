package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.api.migration.DatabaseMigration;
import de.Keyle.MyPet.api.migration.Migration;
import de.Keyle.MyPet.api.migration.MigrationException;
import de.Keyle.MyPet.api.migration.SqlMigrationContext;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reshapes the players table from the 3.x identity model ({@code internal_uuid} PK +
 * {@code mojang_uuid} + {@code name}) to the v4 model (single {@code uuid} PK holding
 * the Mojang UUID).
 * <p>
 * Must run AFTER {@code TranslatePetOwnerUuidToMojang}, which relies on both
 * identifier columns existing side-by-side to translate {@code pets.owner_uuid}.
 * <p>
 * The v4 {@code initStructure()} methods in each repository already create the new
 * shape, so fresh installs never see the old columns. This migration only runs on
 * 3.x upgrades.
 */
@Migration(
        version = "4.0.0",
        description = "Rebuild players table on mojang uuid as single-column primary key",
        dependsOn = {"TranslatePetOwnerUuidToMojang"}
)
public class RebuildPlayersSchemaOnMojangUuid implements DatabaseMigration {

    private static final int ORPHAN_SAMPLE_LIMIT = 10;
    private static final Logger LOG = Logger.getLogger("MyPet");

    // --------------------------------------------------------------------------------
    // SQL — branches on DatabaseMetaData.getDatabaseProductName()
    // --------------------------------------------------------------------------------

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        if (!(ctx instanceof SqlMigrationContextImpl impl)) {
            throw new MigrationException("SqlMigrationContext is not a SqlMigrationContextImpl; "
                    + "cannot reach underlying Connection for schema rewrite.");
        }
        Connection connection = impl.getConnection();
        String prefix = ctx.getTablePrefix();
        String playersTable = prefix + "players";

        String product;
        try {
            DatabaseMetaData meta = connection.getMetaData();
            product = meta.getDatabaseProductName();
        } catch (SQLException e) {
            throw new MigrationException("Failed to read database product name", e);
        }

        try {
            if (hasColumn(connection, playersTable, "uuid")
                    && !hasColumn(connection, playersTable, "internal_uuid")) {
                LOG.info("[MyPet] players table is already on the v4 shape — skipping rebuild.");
                return;
            }
        } catch (SQLException e) {
            throw new MigrationException("Failed to inspect players columns", e);
        }

        if (product != null && product.toLowerCase().contains("sqlite")) {
            rebuildSqlite(connection, playersTable);
        } else if (product != null
                && (product.toLowerCase().contains("mysql") || product.toLowerCase().contains("mariadb"))) {
            rebuildMysql(connection, playersTable);
        } else {
            throw new MigrationException("Unsupported SQL backend: " + product);
        }
    }

    private void rebuildSqlite(Connection connection, String playersTable) throws MigrationException {
        // SQLite cannot ALTER PK / DROP COLUMN. Use the table-rewrite pattern inside
        // a single transaction so a mid-migration crash rolls the whole thing back.
        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            throw new MigrationException("Failed to read autoCommit", e);
        }

        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new MigrationException("Failed to disable autoCommit for SQLite rebuild", e);
        }

        String tempTable = playersTable + "_new";

        try {
            long nullMojangCount = countNullMojangRows(connection, playersTable);
            List<String> nullMojangSamples = sampleNullMojangRows(connection, playersTable);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TRIGGER IF EXISTS update_time_trigger_players");

                stmt.execute("CREATE TABLE " + tempTable + " ("
                        + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                        + "auto_respawn BOOLEAN, "
                        + "auto_respawn_min INTEGER, "
                        + "capture_mode BOOLEAN, "
                        + "health_bar INTEGER, "
                        + "pet_idle_volume FLOAT, "
                        + "extended_info BLOB, "
                        + "multi_world VARCHAR(2000), "
                        + "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ")");

                stmt.execute("INSERT INTO " + tempTable + " "
                        + "(uuid, auto_respawn, auto_respawn_min, capture_mode, health_bar, "
                        + "pet_idle_volume, extended_info, multi_world, last_update) "
                        + "SELECT mojang_uuid, auto_respawn, auto_respawn_min, capture_mode, "
                        + "health_bar, pet_idle_volume, extended_info, multi_world, last_update "
                        + "FROM " + playersTable + " WHERE mojang_uuid IS NOT NULL");

                stmt.execute("DROP TABLE " + playersTable);
                stmt.execute("ALTER TABLE " + tempTable + " RENAME TO " + playersTable);

                // Recreate the trigger. Shape matches SqLiteRepository#createTimestampTrigger —
                // kept intentionally identical so migrated installs and fresh installs have
                // the same trigger definition.
                stmt.execute("CREATE TRIGGER [update_time_trigger_players] "
                        + "AFTER UPDATE ON " + playersTable + " FOR EACH ROW "
                        + "WHEN NEW.last_update < OLD.last_update "
                        + "BEGIN "
                        + "  UPDATE " + playersTable + " SET last_update=CURRENT_TIMESTAMP "
                        + "    WHERE NEW.uuid=OLD.uuid; "
                        + "END;");
            }

            connection.commit();

            if (nullMojangCount > 0) {
                LOG.warning("[MyPet] Dropped " + nullMojangCount + " player rows with NULL "
                        + "mojang_uuid during SQLite rebuild. These could not be migrated to the "
                        + "v4 uuid primary key. Sample internal_uuid values: " + nullMojangSamples
                        + (nullMojangCount > nullMojangSamples.size()
                                ? " (showing first " + nullMojangSamples.size() + ")" : ""));
            }
            LOG.info("[MyPet] SQLite players table rebuilt on new uuid PK.");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw new MigrationException("SQLite players rebuild failed — rolled back", e);
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // Leaving autoCommit in its new state is not worth failing the migration for.
            }
        }
    }

    private void rebuildMysql(Connection connection, String playersTable) throws MigrationException {
        // MySQL auto-commits every DDL statement. Cross-step atomicity is impossible;
        // each step is written to be independently retry-safe by guarding on current
        // column presence so a FAILED re-run resumes from the right point.
        try {
            deleteNullMojangRows(connection, playersTable);

            if (hasColumn(connection, playersTable, "name")) {
                execute(connection, "ALTER TABLE " + playersTable + " DROP COLUMN name");
            }

            // Drop PK and internal_uuid in one ALTER — MySQL permits this combination.
            if (hasColumn(connection, playersTable, "internal_uuid")) {
                execute(connection, "ALTER TABLE " + playersTable
                        + " DROP PRIMARY KEY, DROP COLUMN internal_uuid");
            }

            // Rename mojang_uuid -> uuid. CHANGE COLUMN allows the rename + type restate.
            if (hasColumn(connection, playersTable, "mojang_uuid")) {
                execute(connection, "ALTER TABLE " + playersTable
                        + " CHANGE COLUMN mojang_uuid uuid VARCHAR(36) NOT NULL");
            }

            // Add PK on uuid if not already present (belt-and-suspenders for partial retries).
            if (!hasPrimaryKey(connection, playersTable)) {
                execute(connection, "ALTER TABLE " + playersTable + " ADD PRIMARY KEY (uuid)");
            }

            LOG.info("[MyPet] MySQL players table rebuilt on new uuid PK.");
        } catch (SQLException e) {
            throw new MigrationException("MySQL players rebuild failed", e);
        }
    }

    // --------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------

    private long countNullMojangRows(Connection connection, String playersTable) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + playersTable
                     + " WHERE mojang_uuid IS NULL")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private List<String> sampleNullMojangRows(Connection connection, String playersTable) throws SQLException {
        List<String> samples = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT internal_uuid FROM " + playersTable
                     + " WHERE mojang_uuid IS NULL")) {
            while (rs.next() && samples.size() < ORPHAN_SAMPLE_LIMIT) {
                samples.add(rs.getString(1));
            }
        }
        return samples;
    }

    private void deleteNullMojangRows(Connection connection, String playersTable) throws SQLException {
        // Only meaningful if the old columns still exist (partial retry may have dropped them).
        if (!hasColumn(connection, playersTable, "mojang_uuid")) {
            return;
        }
        long count = countNullMojangRows(connection, playersTable);
        if (count == 0) {
            return;
        }
        List<String> samples = sampleNullMojangRows(connection, playersTable);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM " + playersTable + " WHERE mojang_uuid IS NULL")) {
            ps.executeUpdate();
        }
        LOG.warning("[MyPet] Dropped " + count + " player rows with NULL mojang_uuid during "
                + "MySQL rebuild. These could not be migrated to the v4 uuid primary key. "
                + "Sample internal_uuid values: " + samples
                + (count > samples.size() ? " (showing first " + samples.size() + ")" : ""));
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }
        // Case sensitivity on some backends — try lowercase table name too.
        try (ResultSet rs = meta.getColumns(null, null, table.toLowerCase(), column)) {
            return rs.next();
        }
    }

    private boolean hasPrimaryKey(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getPrimaryKeys(null, null, table.toLowerCase())) {
            return rs.next();
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
