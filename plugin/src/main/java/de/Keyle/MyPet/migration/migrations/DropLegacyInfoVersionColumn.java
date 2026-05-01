package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.migration.DatabaseMigration;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.SqlMigrationContext;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Drops the legacy {@code info.version} INTEGER column. It was used by the pre-v4
 * {@code updateStructure() / updateToV{n}()} migration chain, which has been replaced by
 * this per-class migration framework. Nothing reads {@code info.version} anymore; this
 * migration removes it from existing databases. The v4 {@code initStructure()} methods no
 * longer create the column, so fresh installs never see it.
 */
@Migration(
        version = "4.0.0",
        description = "Drop legacy info.version integer column and its trigger"
)
public class DropLegacyInfoVersionColumn implements DatabaseMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        if (!(ctx instanceof SqlMigrationContextImpl impl)) {
            throw new MigrationException("SqlMigrationContext is not a SqlMigrationContextImpl; "
                    + "cannot reach underlying Connection for schema rewrite.");
        }
        Connection connection = impl.getConnection();
        String prefix = ctx.getTablePrefix();
        String infoTable = prefix + "info";

        String product;
        try {
            product = connection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new MigrationException("Failed to read database product name", e);
        }

        try {
            if (!hasColumn(connection, infoTable, "version")) {
                LOG.info("info.version column already absent — skipping drop.");
                return;
            }
        } catch (SQLException e) {
            throw new MigrationException("Failed to inspect info columns", e);
        }

        if (product != null && product.toLowerCase().contains("sqlite")) {
            rebuildSqlite(connection, infoTable);
        } else if (product != null
                && (product.toLowerCase().contains("mysql") || product.toLowerCase().contains("mariadb"))) {
            try {
                execute(connection, "ALTER TABLE " + infoTable + " DROP COLUMN version");
                LOG.info("Dropped legacy info.version column (MySQL).");
            } catch (SQLException e) {
                throw new MigrationException("Failed to drop info.version on MySQL", e);
            }
        } else {
            throw new MigrationException("Unsupported SQL backend: " + product);
        }
    }

    private void rebuildSqlite(Connection connection, String infoTable) throws MigrationException {
        // SQLite can't DROP a UNIQUE column directly (version is `INTEGER UNIQUE`), so use
        // the table-rewrite pattern inside a transaction. The info trigger is dropped
        // entirely — it referenced `NEW.version = OLD.version` in its WHERE, which can't be
        // replicated after the column is gone, and info is a single-row table that doesn't
        // need a timestamp-refresh trigger.
        String tempTable = infoTable + "_new";

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

        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TRIGGER IF EXISTS update_time_trigger_info");

                stmt.execute("CREATE TABLE " + tempTable + " ("
                        + "mypet_version VARCHAR(20), "
                        + "mypet_build VARCHAR(20), "
                        + "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ")");

                stmt.execute("INSERT INTO " + tempTable
                        + " (mypet_version, mypet_build, last_update) "
                        + "SELECT mypet_version, mypet_build, last_update FROM " + infoTable);

                stmt.execute("DROP TABLE " + infoTable);
                stmt.execute("ALTER TABLE " + tempTable + " RENAME TO " + infoTable);
            }

            connection.commit();
            LOG.info("Dropped legacy info.version column (SQLite table rewrite).");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw new MigrationException("SQLite info rebuild failed — rolled back", e);
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // Not worth failing the migration for.
            }
        }
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getColumns(null, null, table.toLowerCase(), column)) {
            return rs.next();
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
