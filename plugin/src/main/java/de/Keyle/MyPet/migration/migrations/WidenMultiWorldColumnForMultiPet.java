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

import de.Keyle.MyPet.migration.DatabaseMigration;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.SchemaIntrospector;
import de.Keyle.MyPet.migration.SqlMigrationContext;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Widens {@code players.multi_world} from {@code VARCHAR(2000)} to {@code TEXT}.
 *
 * <p>The column holds a JSON object of world group to pet UUID bindings. It was sized
 * when a group could bind exactly one pet, so 2000 characters was generous. Once a
 * player may have several pets active per group the value becomes a JSON array per
 * group, and with a high stored-pet limit across several world groups it can outgrow
 * the declared width — silently truncating a player's bindings on MySQL.
 *
 * <p>Only MySQL and MariaDB enforce the declared width. SQLite ignores column lengths
 * entirely (dynamic typing), so this is a no-op there and the table is left untouched
 * rather than rewritten for nothing.
 */
@Migration(
        version = "4.0.1",
        description = "Widen players.multi_world to TEXT for multi-pet world group bindings"
)
public class WidenMultiWorldColumnForMultiPet implements DatabaseMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        if (!(ctx instanceof SqlMigrationContextImpl impl)) {
            throw new MigrationException("SqlMigrationContext is not a SqlMigrationContextImpl; "
                    + "cannot reach underlying Connection for schema change.");
        }
        Connection connection = impl.getConnection();
        String playersTable = ctx.getTablePrefix() + "players";

        String product;
        try {
            product = connection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new MigrationException("Failed to read database product name", e);
        }

        if (product == null) {
            throw new MigrationException("Unsupported SQL backend: null product name");
        }
        String lower = product.toLowerCase();

        if (lower.contains("sqlite")) {
            // SQLite ignores declared column lengths, so the existing column already
            // stores an arbitrarily long value. Rewriting the table would be churn.
            LOG.info("multi_world widening not needed on SQLite (dynamic typing) — skipping.");
            return;
        }

        if (!lower.contains("mysql") && !lower.contains("mariadb")) {
            throw new MigrationException("Unsupported SQL backend: " + product);
        }

        try {
            if (!hasColumn(connection, playersTable, "multi_world")) {
                LOG.info("players.multi_world column absent — skipping widening.");
                return;
            }
            if (isAlreadyWide(connection, playersTable)) {
                LOG.info("players.multi_world is already TEXT — skipping widening.");
                return;
            }
        } catch (SQLException e) {
            throw new MigrationException("Failed to inspect players.multi_world", e);
        }

        try {
            execute(connection, "ALTER TABLE " + playersTable + " MODIFY multi_world TEXT");
            LOG.info("Widened players.multi_world to TEXT (MySQL).");
        } catch (SQLException e) {
            throw new MigrationException("Failed to widen players.multi_world on MySQL", e);
        }
    }

    /**
     * True when the column is already a LOB-ish type. Makes the migration idempotent:
     * re-running it on an installation that started fresh with TEXT is a no-op.
     */
    private boolean isAlreadyWide(Connection connection, String table) throws SQLException {
        String type = SchemaIntrospector.columnTypeName(connection, table, "multi_world");
        return type != null && type.toUpperCase().contains("TEXT");
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        return SchemaIntrospector.hasColumn(connection, table, column);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
