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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Translates pets.owner_uuid values from players.internal_uuid to players.mojang_uuid.
 * <p>
 * Pre-v4 installs stored the opaque internal_uuid in pets.owner_uuid. The v4 code paths
 * write the Mojang UUID instead (MyPetPlayerImpl#getUniqueId returns the mojangUUID field).
 * This migration rewrites historical rows so owner_uuid is uniformly the Mojang UUID. A
 * separate later migration rebuilds the players table and drops internal_uuid — that
 * migration declares dependsOn on this one.
 * <p>
 * Idempotency is by construction: the UPDATE only touches rows whose owner_uuid is
 * still an internal_uuid whose player has a non-null mojang_uuid. A second invocation
 * finds nothing to translate.
 * <p>
 * Depends on {@code BackfillOfflineUuidsFromName}: rows from an offline-mode 3.x
 * install have a NULL {@code mojang_uuid} until that migration derives one, and
 * would otherwise be reported here as untranslatable and then deleted.
 */
@Migration(
        version = "4.0.0",
        description = "Switch pets.owner_uuid from players.internal_uuid to players.mojang_uuid",
        dependsOn = {"BackfillOfflineUuidsFromName"}
)
public class TranslatePetOwnerUuidToMojang implements DatabaseMigration {

    private static final int ORPHAN_SAMPLE_LIMIT = 10;
    private static final Logger LOG = Logger.getLogger("MyPet");

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        String prefix = ctx.getTablePrefix();
        String petsTable = prefix + "pets";
        String playersTable = prefix + "players";

        // Every statement below reads players.internal_uuid, so confirm the table is
        // actually on the 3.x shape rather than trusting the install classification.
        // BackfillOfflineUuidsFromName performs the mirror check; without this one a
        // misclassified v4 install would simply move its "Unknown column" failure from
        // that migration to this one. See SchemaIntrospector for how a v4 install came
        // to be classified as a 3.x upgrade in the first place.
        if (ctx instanceof SqlMigrationContextImpl impl) {
            try {
                if (!SchemaIntrospector.hasColumn(impl.getConnection(), playersTable, "internal_uuid")) {
                    LOG.info("players table has no internal_uuid column — the v4 identity "
                            + "model is already in place and there is nothing to translate.");
                    return;
                }
            } catch (SQLException e) {
                throw new MigrationException("Failed to inspect players columns", e);
            }
        }

        // "Translatable" = players with both internal_uuid and mojang_uuid populated.
        // Players with internal_uuid set but mojang_uuid NULL can't be translated —
        // their pets would get a NULL owner_uuid from the correlated subquery. We
        // exclude them from the UPDATE so those pets are left with their old owner_uuid
        // and surface as untranslatable in the log.
        String translatableSubquery =
                "SELECT internal_uuid FROM " + playersTable
                        + " WHERE internal_uuid IS NOT NULL AND mojang_uuid IS NOT NULL";

        // Pets whose owner is a player with NULL mojang_uuid — can't migrate these rows.
        List<String> untranslatableSamples = new ArrayList<>();
        long untranslatableCount = ctx.queryAndMap(
                "SELECT p.owner_uuid FROM " + petsTable + " p"
                        + " JOIN " + playersTable + " pl ON pl.internal_uuid = p.owner_uuid"
                        + " WHERE pl.mojang_uuid IS NULL",
                rs -> {
                    long count = 0;
                    while (rs.next()) {
                        count++;
                        if (untranslatableSamples.size() < ORPHAN_SAMPLE_LIMIT) {
                            untranslatableSamples.add(rs.getString(1));
                        }
                    }
                    return count;
                });

        if (untranslatableCount > 0) {
            LOG.warning("" + untranslatableCount + " pet rows are owned by a player "
                    + "whose mojang_uuid is NULL. These cannot be translated and will be left "
                    + "with their legacy internal_uuid (they will be surfaced as orphans by the "
                    + "subsequent players-rebuild migration). Sample owner_uuid values: "
                    + untranslatableSamples
                    + (untranslatableCount > untranslatableSamples.size()
                            ? " (showing first " + untranslatableSamples.size() + ")" : ""));
        }

        // True orphans: owner_uuid matches neither internal_uuid nor mojang_uuid in players.
        // Query BEFORE the UPDATE so the sample reflects pre-translation state.
        List<String> orphanSamples = new ArrayList<>();
        long orphanCount = ctx.queryAndMap(
                "SELECT owner_uuid FROM " + petsTable
                        + " WHERE owner_uuid NOT IN (SELECT internal_uuid FROM " + playersTable
                        + " WHERE internal_uuid IS NOT NULL)"
                        + " AND owner_uuid NOT IN (SELECT mojang_uuid FROM " + playersTable
                        + " WHERE mojang_uuid IS NOT NULL)",
                rs -> {
                    long count = 0;
                    while (rs.next()) {
                        count++;
                        if (orphanSamples.size() < ORPHAN_SAMPLE_LIMIT) {
                            orphanSamples.add(rs.getString(1));
                        }
                    }
                    return count;
                });

        if (orphanCount > 0) {
            LOG.warning("" + orphanCount + " pet rows have an owner_uuid that matches "
                    + "neither players.internal_uuid nor players.mojang_uuid. These rows will be "
                    + "left unchanged. Sample UUIDs: " + orphanSamples
                    + (orphanCount > orphanSamples.size() ? " (showing first " + orphanSamples.size() + ")" : ""));
        }

        // Single correlated UPDATE — atomic at the statement level on both SQLite and MySQL.
        // The IN-subquery in WHERE excludes NULL-mojang_uuid players so we never write a
        // NULL into owner_uuid. A second run finds nothing translatable (every pet either
        // has a mojang_uuid already, is untranslatable, or is an orphan).
        ctx.execute(
                "UPDATE " + petsTable + " SET owner_uuid = ("
                        + "SELECT mojang_uuid FROM " + playersTable
                        + " WHERE internal_uuid = " + petsTable + ".owner_uuid"
                        + " AND mojang_uuid IS NOT NULL"
                        + ") WHERE owner_uuid IN (" + translatableSubquery + ")");

        // Post-condition: every translatable pet should now hold a mojang_uuid.
        long stillTranslatable = ctx.queryAndMap(
                "SELECT COUNT(*) FROM " + petsTable
                        + " WHERE owner_uuid IN (" + translatableSubquery + ")",
                rs -> rs.next() ? rs.getLong(1) : -1L);

        long nowMojang = ctx.queryAndMap(
                "SELECT COUNT(*) FROM " + petsTable
                        + " WHERE owner_uuid IN (SELECT mojang_uuid FROM " + playersTable
                        + " WHERE mojang_uuid IS NOT NULL)",
                rs -> rs.next() ? rs.getLong(1) : -1L);

        LOG.info("pets.owner_uuid translation complete.");
        LOG.info("  Translatable rows remaining on internal_uuid: " + stillTranslatable + " (expected 0).");
        LOG.info("  Joins to players.mojang_uuid: " + nowMojang + ".");
        LOG.info("  Untranslatable (NULL mojang_uuid owner, left unchanged): " + untranslatableCount + ".");
        LOG.info("  Orphans (left unchanged): " + orphanCount + ".");

        if (stillTranslatable > 0) {
            throw new MigrationException("Post-condition failed: " + stillTranslatable
                    + " pet rows still hold a translatable internal_uuid after the UPDATE.");
        }
    }
}
