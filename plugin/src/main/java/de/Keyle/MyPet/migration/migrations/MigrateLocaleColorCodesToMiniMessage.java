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

import de.Keyle.MyPet.migration.ConfigMigration;
import de.Keyle.MyPet.migration.ConfigMigrationContext;
import de.Keyle.MyPet.migration.Migration;
import de.Keyle.MyPet.migration.MigrationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Normalizes user locale override files ({@code <dataFolder>/locale/MyPet_<locale>.properties})
 * to a form the runtime can parse without band-aids. Two independent passes per file:
 * <ol>
 *   <li>Convert legacy {@code §}/{@code &} color codes to MiniMessage (per-line, value-only).</li>
 *   <li>Replace non-canonical MiniMessage alias tags ({@code <r>}, {@code <darkblue>}, etc.)
 *       with canonical forms (whole-file string replace).</li>
 * </ol>
 *
 * <p>The runtime {@code Locale} class parses every locale value through MiniMessage with no
 * normalization step, so surviving {@code &}/{@code §} codes or alias tags would render as
 * literal text.</p>
 *
 * <p>Only files inside the plugin data folder are touched. Bundled locale files inside the
 * MyPet JAR are shipped in canonical MiniMessage form and are not affected.</p>
 *
 * <p>Idempotent: once converted, the detection patterns no longer match and the string
 * replacements are no-ops.</p>
 */
@Migration(
        version = "4.0.0",
        description = "Convert legacy color codes and non-canonical MiniMessage aliases in user locale .properties files"
)
public class MigrateLocaleColorCodesToMiniMessage implements ConfigMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");

    private static final Pattern LEGACY_CODE = Pattern.compile("[§&][0-9a-fk-orxA-FK-ORX]");
    private static final Pattern LOCALE_FILE = Pattern.compile("MyPet_.+\\.properties");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Non-canonical MiniMessage alias tags → canonical forms. These were previously
     * normalized at load time by {@code Locale.normalizePlaceholders} (since removed).
     * Includes the four aliases that appeared in bundled translations plus the four
     * camelCase color names the old runtime handled defensively.
     */
    private static final Map<String, String> ALIAS_REPLACEMENTS = new LinkedHashMap<>();
    static {
        ALIAS_REPLACEMENTS.put("<r>", "<reset>");
        ALIAS_REPLACEMENTS.put("<darkblue>", "<dark_blue>");
        ALIAS_REPLACEMENTS.put("<darkgreen>", "<dark_green>");
        ALIAS_REPLACEMENTS.put("<darkaqua>", "<dark_aqua>");
        ALIAS_REPLACEMENTS.put("<darkred>", "<dark_red>");
        ALIAS_REPLACEMENTS.put("<darkpurple>", "<dark_purple>");
        ALIAS_REPLACEMENTS.put("<darkgray>", "<dark_gray>");
        ALIAS_REPLACEMENTS.put("<lightpurple>", "<light_purple>");
    }

    @Override
    public void migrate(ConfigMigrationContext ctx) throws MigrationException {
        File localeDir = new File(ctx.getDataFolder(), "locale");
        if (!localeDir.isDirectory()) {
            LOG.info("No locale/ override directory — skipping locale color-code migration.");
            return;
        }

        File[] files = localeDir.listFiles((dir, name) -> LOCALE_FILE.matcher(name).matches());
        if (files == null || files.length == 0) {
            LOG.info("No MyPet_<locale>.properties override files — skipping locale color-code migration.");
            return;
        }

        int totalConverted = 0;
        int totalAliasReplaced = 0;
        int filesUpdated = 0;
        int filesUntouched = 0;

        for (File file : files) {
            try {
                FileResult result = migrateFile(file);
                if (result.converted > 0 || result.aliasReplaced > 0) {
                    filesUpdated++;
                    totalConverted += result.converted;
                    totalAliasReplaced += result.aliasReplaced;
                } else {
                    filesUntouched++;
                }
            } catch (Exception e) {
                LOG.warning("Failed to process locale file " + file.getName()
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving file unchanged");
            }
        }

        LOG.info("Locale color-code migration complete.");
        LOG.info("  Files updated: " + filesUpdated + ".");
        LOG.info("  Files untouched: " + filesUntouched + ".");
        LOG.info("  Total lines with legacy codes converted: " + totalConverted + ".");
        LOG.info("  Total lines with alias replacements: " + totalAliasReplaced + ".");
    }

    private FileResult migrateFile(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<String> newLines = new ArrayList<>(lines.size());
        FileResult result = new FileResult();
        boolean sawContinuation = false;

        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1);
            String trimmed = line.stripLeading();

            // Comments / blank lines — passthrough.
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                newLines.add(line);
                continue;
            }

            // Java .properties line continuations (backslash at end of line) — we conservatively
            // leave those alone because joining continuation segments correctly requires
            // implementing the full escape/unescape rules. In practice MyPet locale files
            // never use continuations.
            if (endsWithUnescapedBackslash(line)) {
                newLines.add(line);
                sawContinuation = true;
                continue;
            }

            int delimiterIdx = findDelimiter(line);
            if (delimiterIdx < 0) {
                newLines.add(line);
                continue;
            }

            String keyAndDelimiter = line.substring(0, delimiterIdx + 1);
            String value = line.substring(delimiterIdx + 1);

            if (value.isEmpty() || !hasLegacyCode(value)) {
                newLines.add(line);
                continue;
            }

            try {
                String converted = convert(value);
                newLines.add(keyAndDelimiter + converted);
                result.converted++;
                LOG.info("Converted " + file.getName() + ":" + lineNumber
                        + " " + value + "  →  " + converted);
            } catch (Exception e) {
                newLines.add(line);
                LOG.warning("Failed to convert " + file.getName() + ":" + lineNumber
                        + ": " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + " — leaving line unchanged");
            }
        }

        // Pass 2: alias-tag replacement on the post-legacy in-memory lines. Operates as a
        // plain string.replace per line — safe to apply across the whole content since the
        // alias tokens never appear in property keys, and pass 1's escapes (e.g. \<r\>) do
        // not contain the literal <r> substring.
        for (int i = 0; i < newLines.size(); i++) {
            String original = newLines.get(i);
            String fixed = original;
            for (Map.Entry<String, String> repl : ALIAS_REPLACEMENTS.entrySet()) {
                fixed = fixed.replace(repl.getKey(), repl.getValue());
            }
            if (!fixed.equals(original)) {
                newLines.set(i, fixed);
                result.aliasReplaced++;
            }
        }

        if (result.converted > 0 || result.aliasReplaced > 0) {
            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
        }

        if (sawContinuation) {
            LOG.warning("" + file.getName() + " contains line continuations — those "
                    + "lines were not migrated. Inspect manually if they contain legacy color codes.");
        }

        return result;
    }

    /**
     * Finds the first unescaped {@code =} or {@code :} character, which delimits key from
     * value in a Java .properties line. Returns -1 if no delimiter is found.
     */
    private static int findDelimiter(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != '=' && c != ':') {
                continue;
            }
            int backslashes = 0;
            for (int j = i - 1; j >= 0 && line.charAt(j) == '\\'; j--) {
                backslashes++;
            }
            if (backslashes % 2 == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * True iff the last character of the line is an unescaped backslash — the .properties
     * signal for "this value continues on the next line."
     */
    private static boolean endsWithUnescapedBackslash(String line) {
        int trailing = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            trailing++;
        }
        return (trailing % 2) == 1;
    }

    private static boolean hasLegacyCode(String value) {
        return LEGACY_CODE.matcher(value).find();
    }

    private static String convert(String oldValue) {
        // Normalize § to & so one legacy serializer handles both. Both prefix characters
        // use the same code alphabet, so a straight character replace is safe.
        String normalized = oldValue.replace('§', '&');
        Component component = LEGACY.deserialize(normalized);
        return MINI_MESSAGE.serialize(component);
    }

    private static final class FileResult {
        int converted;
        int aliasReplaced;
    }
}
