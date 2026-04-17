package de.Keyle.MyPet.migration.context;

import de.Keyle.MyPet.api.migration.MigrationException;
import de.Keyle.MyPet.api.migration.SqlMigrationContext;

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
