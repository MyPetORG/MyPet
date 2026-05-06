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

package de.Keyle.MyPet.repository;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Constructs and initializes MyPet's persistence backend, applying a fallback chain so a
 * misconfigured MySQL connection does not take the plugin offline when SQLite is also viable.
 *
 * <p>Resolution order:</p>
 * <ol>
 *   <li>If {@code Configuration.Repository.REPOSITORY_TYPE} equals {@code "MySQL"} (case
 *       insensitive), attempt to initialize {@link MySqlRepository}. On
 *       {@link RepositoryInitException}, log via {@link ErrorUtil#reportSevere} and fall through.</li>
 *   <li>Attempt to initialize {@link SqLiteRepository}. On failure, log a warning and return
 *       {@link Optional#empty()} so the caller can disable the plugin.</li>
 * </ol>
 *
 * <p>Logging is performed at info level on success and severe/warning on failure; this class
 * does not throw — failure is communicated by an empty Optional return.</p>
 */
public final class RepositoryFactory {

    private RepositoryFactory() {
    }

    /**
     * initializes the configured repository, falling back from MySQL to SQLite on failure.
     *
     * @return the initialized repository, or {@link Optional#empty()} if both backends failed
     *         to initialize — in which case the caller should disable the plugin
     */
    @NotNull
    public static Optional<Repository> initWithFallback() {
        Repository repository = null;

        if (Configuration.Repository.REPOSITORY_TYPE.equalsIgnoreCase("MySQL")) {
            repository = new MySqlRepository();
            try {
                repository.init();
                MyPetApi.getLogger().info("MySQL connection successful.");
            } catch (RepositoryInitException e) {
                ErrorUtil.reportSevere("MySQL database connection failed during initialization", e);
                repository = null;
            }
        }

        if (repository == null) {
            repository = new SqLiteRepository();
            try {
                repository.init();
                MyPetApi.getLogger().info("SQLite connection successful.");
            } catch (RepositoryInitException ignored) {
                MyPetApi.getLogger().warning("SQLite connection failed!");
                return Optional.empty();
            }
        }

        return Optional.of(repository);
    }
}
