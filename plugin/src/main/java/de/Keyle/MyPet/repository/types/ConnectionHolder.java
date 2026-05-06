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
