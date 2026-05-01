package de.Keyle.MyPet.migration;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SqlMigrationContext {
    void execute(String sql) throws MigrationException;

    /**
     * Executes a read query and passes the {@link ResultSet} to the provided mapper.
     * The {@code ResultSet} and its backing {@code Statement} are closed after the mapper
     * returns, so migration authors must extract everything they need before returning.
     */
    <T> T queryAndMap(String sql, ResultSetMapper<T> mapper) throws MigrationException;

    String getTablePrefix();

    @FunctionalInterface
    interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
