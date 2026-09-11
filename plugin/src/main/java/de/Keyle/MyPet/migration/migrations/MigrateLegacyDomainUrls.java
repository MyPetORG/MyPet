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
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Repoints the service URLs that still name the retired {@code mypet-plugin.de} domain.
 *
 * <p>MyPet moved to {@code mypet-plugin.com}. Changing the {@code ConfigKey} defaults only
 * reaches servers that have never written the key — every existing install already has the
 * old host persisted in {@code config.yml}, and a new default will never override a value
 * that is already on disk. Without this migration the rewrite would silently fix fresh
 * installs only, which are the minority.
 *
 * <p>The old domain still redirects, so nothing is broken today; this is about not leaving
 * every server in the world depending on a redirect indefinitely.
 *
 * <p><b>Only an untouched default is rewritten.</b> Each key is compared against the exact
 * value MyPet used to ship, so an admin running a self-hosted relay or a private wiki mirror
 * keeps it. That check is also what makes this idempotent: once a row says {@code .com} it
 * no longer matches, so a second run is a no-op.
 *
 * <p>{@code skilltree.mypet-plugin.de} is deliberately absent — it has no {@code .com}
 * counterpart yet, and it is a compiled constant rather than a config key in any case.
 */
@Migration(
        version = "4.1.0",
        description = "Repoint mypet-plugin.de service URLs at mypet-plugin.com"
)
public class MigrateLegacyDomainUrls implements ConfigMigration {

    private static final Logger LOG = Logger.getLogger("MyPet");
    private static final String CONFIG = "config.yml";

    /** Config key to the exact value MyPet shipped before the move. */
    private static final Map<String, String> LEGACY_DEFAULTS = new LinkedHashMap<>();

    static {
        LEGACY_DEFAULTS.put("MyPet.Info.Wiki-URL", "https://wiki.mypet-plugin.de");
        LEGACY_DEFAULTS.put("MyPet.WebEditor.BytebinUrl", "https://bytebin.mypet-plugin.de");
        LEGACY_DEFAULTS.put("MyPet.WebEditor.BytesocksUrl", "wss://bytesocks.mypet-plugin.de");
        LEGACY_DEFAULTS.put("MyPet.WebEditor.EditorUrl", "https://editor.mypet-plugin.de");
    }

    @Override
    public void migrate(ConfigMigrationContext ctx) throws MigrationException {
        if (!new File(ctx.getDataFolder(), CONFIG).exists()) {
            return;
        }
        YamlConfiguration config = ctx.getConfig(CONFIG);

        boolean changed = false;
        for (Map.Entry<String, String> entry : LEGACY_DEFAULTS.entrySet()) {
            String key = entry.getKey();
            String legacy = entry.getValue();
            String current = config.getString(key);
            if (current == null) {
                continue;
            }
            // A trailing slash is the one cosmetic difference an admin may have introduced
            // without meaning to change the destination, so it is tolerated on the match.
            String normalized = current.endsWith("/")
                    ? current.substring(0, current.length() - 1) : current;
            if (!normalized.equals(legacy)) {
                continue;
            }
            config.set(key, legacy.replace("mypet-plugin.de", "mypet-plugin.com"));
            changed = true;
        }

        if (changed) {
            ctx.saveConfig(CONFIG);
            LOG.info("[MyPet] Repointed legacy mypet-plugin.de URLs at mypet-plugin.com.");
        }
    }
}
