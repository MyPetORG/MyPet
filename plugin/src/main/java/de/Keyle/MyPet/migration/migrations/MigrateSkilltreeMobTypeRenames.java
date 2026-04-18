package de.Keyle.MyPet.migration.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.migration.Migration;
import de.Keyle.MyPet.api.migration.MigrationException;
import de.Keyle.MyPet.api.migration.SkilltreeMigration;
import de.Keyle.MyPet.api.migration.SkilltreeMigrationContext;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Renames legacy mob type identifiers in the {@code MobTypes} listings of MyPet skilltree
 * ({@code .st.json}) files to their current canonical names:
 * <ul>
 *     <li>{@code PigZombie} / {@code pigzombie} → {@code ZombifiedPiglin}</li>
 *     <li>{@code Snowman} / {@code snowman} → {@code SnowGolem}</li>
 * </ul>
 * The skilltree loader still looks types up case-insensitively, but unknown legacy names
 * are silently dropped because no {@link de.Keyle.MyPet.api.entity.MyPetType} is registered
 * under the old identifiers, leaving affected skilltrees inapplicable to those pets.
 * <p>
 * The leading {@code -} that marks a negated entry is preserved across the rewrite.
 * Idempotent: rewritten entries no longer match any rename source on a second run.
 */
@Migration(
        version = "4.0.0",
        description = "Rename legacy mob type identifiers (PigZombie, Snowman) in skilltree MobTypes lists"
)
public class MigrateSkilltreeMobTypeRenames implements SkilltreeMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");

    private static final Map<String, String> RENAMES;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("pigzombie", "ZombifiedPiglin");
        map.put("snowman", "SnowGolem");
        RENAMES = Map.copyOf(map);
    }

    @Override
    public void migrate(SkilltreeMigrationContext ctx) throws MigrationException {
        List<File> files = ctx.getSkilltreeFiles();
        if (files.isEmpty()) {
            LOG.info("No skilltree files found — skipping skilltree mob-type rename migration.");
            return;
        }

        int totalRenamed = 0;
        int filesUpdated = 0;
        int filesUntouched = 0;

        for (File file : files) {
            try {
                JsonObject root = ctx.readSkilltree(file);
                int renamed = migrateFile(root, file.getName());
                if (renamed > 0) {
                    ctx.writeSkilltree(file, root);
                    filesUpdated++;
                    totalRenamed += renamed;
                } else {
                    filesUntouched++;
                }
            } catch (Exception e) {
                LOG.warning("Failed to process skilltree " + file.getName()
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving file unchanged");
            }
        }

        LOG.info("Skilltree mob-type rename migration complete.");
        LOG.info("  Files updated: " + filesUpdated + ".");
        LOG.info("  Files untouched: " + filesUntouched + ".");
        LOG.info("  Total entries renamed: " + totalRenamed + ".");
    }

    private int migrateFile(JsonObject root, String fileName) {
        if (!root.has("MobTypes") || !root.get("MobTypes").isJsonArray()) {
            return 0;
        }
        JsonArray array = root.getAsJsonArray("MobTypes");
        JsonArray newArray = new JsonArray(array.size());
        int renamed = 0;

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonPrimitive()) {
                newArray.add(element);
                continue;
            }
            String oldEntry = element.getAsString();
            String newEntry = renameEntry(oldEntry);
            if (newEntry != null) {
                newArray.add(newEntry);
                renamed++;
                LOG.info("Renamed " + fileName + "::MobTypes[" + i + "]"
                        + ": " + oldEntry + "  →  " + newEntry);
            } else {
                newArray.add(element);
            }
        }

        if (renamed > 0) {
            root.add("MobTypes", newArray);
        }
        return renamed;
    }

    /**
     * Returns the renamed entry, or {@code null} if no rename applies. The optional
     * leading {@code -} (negation) is stripped before lookup and re-attached on output.
     */
    private static String renameEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return null;
        }
        boolean negated = entry.charAt(0) == '-';
        String token = negated ? entry.substring(1) : entry;
        String replacement = RENAMES.get(token.toLowerCase());
        if (replacement == null) {
            return null;
        }
        return negated ? "-" + replacement : replacement;
    }
}
