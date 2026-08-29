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

package de.Keyle.MyPet.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Schema lookups scoped to the database the connection is actually pointed at.
 * <p>
 * Every migration needs the same three questions answered — does this table exist, does
 * it have this column, does it have a primary key — and each one used to answer them
 * with its own copy of {@code meta.getColumns(null, null, table, column)}. That null
 * catalog is a trap on MySQL: Connector/J's {@code nullDatabaseMeansCurrent} (formerly
 * {@code nullCatalogMeansCurrent}) defaults to <b>false</b>, so a null catalog is not
 * "the current database" — it is no filter at all, and the driver reports columns
 * belonging to any other database on the same server that the connecting user can see.
 * <p>
 * That is not hypothetical. An admin running two servers off one MySQL instance, one
 * still on MyPet 3, made {@code MigrationService#detectInstallType} find
 * {@code players.internal_uuid} in the <i>other</i> server's database, classify a
 * perfectly good v4 install as a 3.x upgrade, and run the 3.x migrations against it.
 * {@code BackfillOfflineUuidsFromName} then died on
 * {@code Unknown column 'internal_uuid' in 'field list'} — the one piece of luck in the
 * sequence, since failing there disabled the plugin before the row-deleting
 * {@code RebuildPlayersSchemaOnMojangUuid} could run against a database that never
 * needed migrating.
 * <p>
 * Passing the connection's own catalog closes that off at the source, rather than
 * relying on a driver-specific connection property being set correctly in a URL the
 * admin controls.
 * <p>
 * The second, quieter bug fixed here: the {@code table} and {@code column} arguments to
 * {@code getTables}/{@code getColumns} are <i>patterns</i>, in which {@code _} matches
 * any single character. Table prefixes and legacy column names are full of underscores
 * ({@code mypet_players}, {@code internal_uuid}, {@code multi_world}), so each of those
 * lookups was a wildcard match waiting to hit the wrong object. They are escaped here.
 * {@link DatabaseMetaData#getPrimaryKeys} takes an exact name rather than a pattern, so
 * it is deliberately left unescaped.
 */
public final class SchemaIntrospector {

    private SchemaIntrospector() {
    }

    /**
     * Whether {@code table} exists in the connection's current database.
     */
    public static boolean hasTable(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = currentCatalog(connection);
        String schema = catalog == null ? currentSchema(connection) : null;
        String escape = searchStringEscape(meta);

        for (String candidate : nameCandidates(table)) {
            try (ResultSet rs = meta.getTables(catalog, schema, escapePattern(candidate, escape), null)) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether {@code table} in the connection's current database has {@code column}.
     */
    public static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = currentCatalog(connection);
        String schema = catalog == null ? currentSchema(connection) : null;
        String escape = searchStringEscape(meta);
        String columnPattern = escapePattern(column, escape);

        for (String candidate : nameCandidates(table)) {
            try (ResultSet rs = meta.getColumns(catalog, schema, escapePattern(candidate, escape), columnPattern)) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The driver-reported {@code TYPE_NAME} of a column in the connection's current
     * database, or null when the column is not there. Callers that only need presence
     * should use {@link #hasColumn} — this exists for the migrations that branch on the
     * declared type rather than on existence.
     */
    public static String columnTypeName(Connection connection, String table, String column)
            throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = currentCatalog(connection);
        String schema = catalog == null ? currentSchema(connection) : null;
        String escape = searchStringEscape(meta);
        String columnPattern = escapePattern(column, escape);

        for (String candidate : nameCandidates(table)) {
            try (ResultSet rs = meta.getColumns(catalog, schema, escapePattern(candidate, escape), columnPattern)) {
                if (rs.next()) {
                    return rs.getString("TYPE_NAME");
                }
            }
        }
        return null;
    }

    /**
     * Whether {@code table} in the connection's current database has a primary key.
     */
    public static boolean hasPrimaryKey(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = currentCatalog(connection);
        String schema = catalog == null ? currentSchema(connection) : null;

        // getPrimaryKeys takes an exact table name, NOT a pattern — no escaping here.
        for (String candidate : nameCandidates(table)) {
            try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, candidate)) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The name as given, plus its lowercase form — MySQL folds table names to lowercase
     * on case-insensitive filesystems ({@code lower_case_table_names=1}), so the stored
     * name may not match what the config says. A LinkedHashSet keeps the given form
     * first and collapses the two into one lookup when they are already equal.
     */
    private static Set<String> nameCandidates(String table) {
        Set<String> candidates = new LinkedHashSet<>(2);
        candidates.add(table);
        candidates.add(table.toLowerCase());
        return candidates;
    }

    /**
     * The connection's current database, or null if the backend has no meaningful
     * catalog. Null falls back to the driver's default scoping — correct for SQLite,
     * which has exactly one catalog and therefore nothing to leak across.
     */
    private static String currentCatalog(Connection connection) {
        try {
            String catalog = connection.getCatalog();
            return catalog == null || catalog.isBlank() ? null : catalog;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Only consulted when there is no catalog: a MySQL URL carrying
     * {@code databaseTerm=SCHEMA} reports the database through getSchema() instead.
     * Some drivers (older SQLite builds) throw here rather than return null.
     */
    private static String currentSchema(Connection connection) {
        try {
            String schema = connection.getSchema();
            return schema == null || schema.isBlank() ? null : schema;
        } catch (SQLException | AbstractMethodError | UnsupportedOperationException e) {
            return null;
        }
    }

    private static String searchStringEscape(DatabaseMetaData meta) {
        try {
            String escape = meta.getSearchStringEscape();
            return escape == null || escape.isEmpty() ? null : escape;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Escapes the LIKE metacharacters in an identifier so it matches literally. A driver
     * that reports no escape sequence gets the raw name — the pre-existing behaviour,
     * which is still correct for every name without an underscore.
     */
    private static String escapePattern(String value, String escape) {
        if (escape == null) {
            return value;
        }
        char escapeChar = escape.length() == 1 ? escape.charAt(0) : '\0';
        StringBuilder escaped = new StringBuilder(value.length() + 4);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '_' || ch == '%' || (escapeChar != '\0' && ch == escapeChar)) {
                escaped.append(escape);
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }
}
