package de.Keyle.MyPet.repository.types;

import java.sql.Connection;

/**
 * AutoCloseable wrapper around a JDBC Connection. {@link #close()} does whatever
 * the owning repository needs — for pooled backends it returns the connection
 * to the pool; for SQLite (single long-lived connection) it is a no-op.
 * Never throws from close().
 */
public interface ConnectionHolder extends AutoCloseable {
    Connection connection();

    @Override
    void close();
}
