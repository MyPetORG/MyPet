package de.Keyle.MyPet.migration.migrations;

import de.Keyle.MyPet.api.migration.Migration;
import de.Keyle.MyPet.api.migration.MigrationException;
import de.Keyle.MyPet.api.migration.PetDataMigration;
import de.Keyle.MyPet.api.migration.SqlMigrationContext;
import de.Keyle.MyPet.migration.context.SqlMigrationContextImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Converts {@code pets.name} values from legacy color-code formatting
 * ({@code &4}, {@code §c}, hex {@code &x&A&B&C&D&E&F}) to the MiniMessage format
 * that the v4 runtime expects.
 * <p>
 * The v4 {@code MyPet#updateName} path deserializes the pet name through
 * {@link MiniMessage}. Legacy-formatted names there lose all colors and render as
 * literal {@code &4Fluffy} text. This migration rewrites those values in-place so
 * existing named pets render correctly.
 * <p>
 * Idempotent: once a name has been converted to MiniMessage, it contains no
 * {@code &} or {@code §} color codes, so the detection regex won't match on a
 * second run.
 */
@Migration(
        version = "4.0.0",
        description = "Convert pets.name from legacy color codes to MiniMessage"
)
public class MigratePetNamesToMiniMessage implements PetDataMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");
    private static final int SAMPLE_LIMIT = 10;

    /**
     * Matches any single legacy color/format code: {@code &} or {@code §} followed by
     * 0-9, a-f, k-o, r, or x (the vanilla code alphabet, case-insensitive).
     */
    private static final Pattern LEGACY_CODE = Pattern.compile("[§&][0-9a-fk-orxA-FK-ORX]");

    /**
     * Legacy parser configured for Bukkit conventions: {@code &} prefix, hex colors
     * enabled, and the "unusual" {@code &x&A&B&C&D&E&F} repeated-character hex format
     * that Bukkit uses in config files and database-stored names.
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void migrateSql(SqlMigrationContext ctx) throws MigrationException {
        if (!(ctx instanceof SqlMigrationContextImpl impl)) {
            throw new MigrationException("SqlMigrationContext is not a SqlMigrationContextImpl; "
                    + "cannot reach underlying Connection for column update.");
        }
        Connection connection = impl.getConnection();
        String petsTable = ctx.getTablePrefix() + "pets";

        // Step 1: read every (uuid, name) pair.
        Map<String, String> rows = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, name FROM " + petsTable)) {
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            throw new MigrationException("Failed to read pets for name migration", e);
        }

        // Step 2: classify each name.
        List<Conversion> conversions = new ArrayList<>();
        int alreadyClean = 0;
        int nullNames = 0;
        int failed = 0;
        List<String[]> samples = new ArrayList<>();

        for (Map.Entry<String, String> row : rows.entrySet()) {
            String uuid = row.getKey();
            String oldName = row.getValue();
            if (oldName == null) {
                nullNames++;
                continue;
            }
            if (!hasLegacyCode(oldName)) {
                alreadyClean++;
                continue;
            }
            try {
                String newName = convert(oldName);
                conversions.add(new Conversion(uuid, newName));
                if (samples.size() < SAMPLE_LIMIT) {
                    samples.add(new String[]{oldName, newName});
                }
            } catch (Exception e) {
                failed++;
                LOG.warning("Failed to convert pet name for " + uuid + ": "
                        + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving name unchanged");
            }
        }

        // Step 3: apply updates in a single transaction.
        if (!conversions.isEmpty()) {
            boolean previousAutoCommit;
            try {
                previousAutoCommit = connection.getAutoCommit();
            } catch (SQLException e) {
                throw new MigrationException("Failed to read autoCommit", e);
            }
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE " + petsTable + " SET name = ? WHERE uuid = ?")) {
                    for (Conversion conversion : conversions) {
                        ps.setString(1, conversion.newName());
                        ps.setString(2, conversion.uuid());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw new MigrationException("Failed to write converted pet names — rolled back", e);
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // not worth failing the migration for
                }
            }
        }

        LOG.info("Pet name migration complete.");
        LOG.info("  Converted: " + conversions.size() + ".");
        LOG.info("  Already clean (no legacy codes): " + alreadyClean + ".");
        LOG.info("  Null names: " + nullNames + ".");
        LOG.info("  Failed (left unchanged): " + failed + ".");
    }

    private static boolean hasLegacyCode(String name) {
        return LEGACY_CODE.matcher(name).find();
    }

    private static String convert(String oldName) {
        // Normalize § to & so a single legacy serializer handles both. Both prefix characters
        // use the same code alphabet, so a straight character replace is safe.
        String normalized = oldName.replace('§', '&');
        Component component = LEGACY.deserialize(normalized);
        return MINI_MESSAGE.serialize(component);
    }

    private record Conversion(String uuid, String newName) {
    }
}
