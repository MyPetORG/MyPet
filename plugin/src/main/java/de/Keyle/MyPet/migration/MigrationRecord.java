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

public class MigrationRecord {
    private final String migrationId;
    private final String version;
    private final MigrationDomain domain;
    private final long appliedAt;
    private MigrationStatus status;
    private long executionTimeMs;
    private String errorMessage;

    public MigrationRecord(String migrationId, String version, MigrationDomain domain) {
        this.migrationId = migrationId;
        this.version = version;
        this.domain = domain;
        this.appliedAt = System.currentTimeMillis();
        this.status = MigrationStatus.IN_PROGRESS;
        this.executionTimeMs = 0;
        this.errorMessage = null;
    }

    public MigrationRecord(String migrationId, String version, MigrationDomain domain,
                           long appliedAt, MigrationStatus status, long executionTimeMs,
                           String errorMessage) {
        this.migrationId = migrationId;
        this.version = version;
        this.domain = domain;
        this.appliedAt = appliedAt;
        this.status = status;
        this.executionTimeMs = executionTimeMs;
        this.errorMessage = errorMessage;
    }

    public String getMigrationId() {
        return migrationId;
    }

    public String getVersion() {
        return version;
    }

    public MigrationDomain getDomain() {
        return domain;
    }

    public long getAppliedAt() {
        return appliedAt;
    }

    public MigrationStatus getStatus() {
        return status;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setStatus(MigrationStatus status) {
        this.status = status;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
