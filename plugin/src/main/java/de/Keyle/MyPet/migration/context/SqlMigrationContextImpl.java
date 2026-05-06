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

package de.Keyle.MyPet.migration.context;

import de.Keyle.MyPet.migration.MigrationException;
import de.Keyle.MyPet.migration.SqlMigrationContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlMigrationContextImpl implements SqlMigrationContext, AutoCloseable {
    private final Connection connection;
    private final String tablePrefix;

    public SqlMigrationContextImpl(Connection connection, String tablePrefix) {
        this.connection = connection;
        this.tablePrefix = tablePrefix;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    @Override
    public void execute(String sql) throws MigrationException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new MigrationException("SQL execution failed: " + sql, e);
        }
    }

    @Override
    public <T> T queryAndMap(String sql, ResultSetMapper<T> mapper) throws MigrationException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapper.map(rs);
        } catch (SQLException e) {
            throw new MigrationException("SQL query failed: " + sql, e);
        }
    }

    @Override
    public String getTablePrefix() {
        return tablePrefix;
    }
}
