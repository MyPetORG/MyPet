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

package de.Keyle.MyPet.webeditor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.util.MyPetReloader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Writes a change payload (the {@code configs} object, containing only the dirty
 * files) back to disk and hot-reloads the affected subsystems.
 *
 * <p>{@link #reload(JsonObject)} touches plugin state and MUST be called on the
 * server main thread; the socket handler schedules it there. Writing files is
 * plain I/O and is safe off-thread.
 */
public final class ConfigApplier {

    private final File dataFolder;

    public ConfigApplier(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public ConfigApplier() {
        this(MyPetApi.getPlugin().getDataFolder());
    }

    /** Write all changed files from the payload to disk (off-thread safe). */
    public void writeChanges(JsonObject changedConfigs) throws IOException {
        writeYaml(changedConfigs, "config", "config.yml");
        writeYaml(changedConfigs, "pet-config", "pet-config.yml");
        writeYaml(changedConfigs, "exp-config", "exp-config.yml");
        writeYaml(changedConfigs, "pet-shops", "pet-shops.yml");
        writeYaml(changedConfigs, "hooks-config", "hooks-config.yml");

        if (changedConfigs.has("skilltrees")) {
            File dir = ensureDir("skilltrees");
            JsonObject files = changedConfigs.getAsJsonObject("skilltrees").getAsJsonObject("files");
            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                // .st.json files store the raw JSON object as written by the editor. A broken
                // file that can't be parsed round-trips as a raw-text JSON string primitive
                // instead — write it verbatim, not as a quoted .toString() literal.
                File target = safeChildFile(dir, entry.getKey(), ".st.json");
                if (target != null) {
                    JsonElement value = entry.getValue();
                    write(target, value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                            ? value.getAsString() : value.toString());
                }
            }
        }

        if (changedConfigs.has("locale")) {
            File dir = ensureDir("locale");
            JsonObject files = changedConfigs.getAsJsonObject("locale").getAsJsonObject("files");
            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                File target = safeChildFile(dir, entry.getKey(), ".properties");
                if (target != null) {
                    write(target, entry.getValue().getAsString());
                }
            }
        }
    }

    /**
     * Resolve an editor-supplied {@code name} as a direct child of {@code dir},
     * rejecting anything that isn't a plain single-segment filename with the
     * expected suffix. Guards the skilltree/locale bundles against path traversal
     * (`..`, separators, absolute paths) in untrusted change payloads — the
     * filenames come straight from the relayed JSON. Returns {@code null} (after
     * logging) for an unsafe name.
     */
    private File safeChildFile(File dir, String name, String requiredSuffix) {
        File target = (name != null && name.endsWith(requiredSuffix)) ? safeChildFile(dir, name) : null;
        if (target == null) {
            MyPetApi.getLogger().warning("WebEditor: rejected unsafe file name in change payload: " + name);
        }
        return target;
    }

    /**
     * Resolve {@code name} as a direct child of {@code dir} after normalization,
     * rejecting anything that would escape {@code dir} (`..`, separators, absolute
     * or drive-relative paths). Shared with other untrusted-filename call sites
     * (e.g. the web editor's model upload); callers log their own context-specific
     * warning. Returns {@code null} for an unsafe name.
     */
    static File safeChildFile(File dir, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Path base = dir.toPath().toAbsolutePath().normalize();
        Path target = base.resolve(name).normalize();
        if (target.startsWith(base) && base.equals(target.getParent())) {
            return target.toFile();
        }
        return null;
    }

    /**
     * The reload work a payload key requires. Declaration order IS the execution
     * order (config → skilltrees → shops, matching {@code /mypet reload all}):
     * reloadSkilltrees re-resolves trees against state reloadConfig may have just
     * rebuilt. EnumSet iterates in declaration order, so do not reorder.
     */
    private enum ReloadAction {
        CONFIG(MyPetReloader::reloadConfig),
        SKILLTREES(MyPetReloader::reloadSkilltrees),
        SHOPS(MyPetReloader::reloadShops);

        private final Runnable action;

        ReloadAction(Runnable action) {
            this.action = action;
        }

        void run() {
            action.run();
        }
    }

    /**
     * Which reload each payload key needs. locale/exp-config/hooks-config fold into
     * CONFIG because reloadConfig already covers them (Locale.init, the exp-config
     * re-read in ConfigurationLoader.loadConfiguration, and the ServiceManager hook
     * config load respectively) — and because /mypet reload has no separate target
     * for them.
     */
    private static final Map<String, ReloadAction> ACTION_BY_KEY = Map.of(
            "config", ReloadAction.CONFIG,
            "pet-config", ReloadAction.CONFIG,
            "exp-config", ReloadAction.CONFIG,
            "hooks-config", ReloadAction.CONFIG,
            "locale", ReloadAction.CONFIG,
            "skilltrees", ReloadAction.SKILLTREES,
            "pet-shops", ReloadAction.SHOPS
    );

    /**
     * Hot-reload only the subsystems the changed files affect. Call on the main thread.
     *
     * @param changedConfigs the payload's {@code configs} object (dirty files only)
     */
    public void reload(JsonObject changedConfigs) {
        EnumSet<ReloadAction> actions = EnumSet.noneOf(ReloadAction.class);
        for (String key : changedConfigs.keySet()) {
            ReloadAction action = ACTION_BY_KEY.get(key);
            if (action != null) {
                actions.add(action);
            }
        }
        for (ReloadAction action : actions) {
            action.run();
        }
    }

    private void writeYaml(JsonObject configs, String key, String fileName) throws IOException {
        if (!configs.has(key)) {
            return;
        }
        String content = configs.getAsJsonObject(key).get("content").getAsString();
        write(new File(dataFolder, fileName), content);
    }

    private File ensureDir(String subDir) {
        File dir = new File(dataFolder, subDir);
        if (!dir.exists() && !dir.mkdirs()) {
            MyPetApi.getLogger().warning("WebEditor: could not create directory " + dir.getPath());
        }
        return dir;
    }

    /**
     * Write via a temp file + atomic rename so a concurrent reader (e.g. a manual
     * /mypet reload racing the socket thread) never sees a half-written file. The
     * temp file must share the target's directory: ATOMIC_MOVE only guarantees
     * atomicity within a filesystem.
     */
    private static void write(File file, String content) throws IOException {
        Path target = file.toPath();
        Path parent = target.getParent();
        Path tmp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            applyTargetPermissions(target, parent, tmp);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Some filesystems (certain network/FUSE mounts) refuse atomic rename.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Best-effort: give the temp file the permissions the final file should have.
     * ATOMIC_MOVE is a rename, so it carries over the temp file's mode — and
     * {@link Files#createTempFile} defaults to owner-only (600) — rather than the
     * target's. If the target already exists we copy its exact mode; for a
     * brand-new file we derive one from the parent directory (execute bits
     * stripped) instead of hardcoding 644, which could loosen permissions under a
     * restrictive umask. Never fails the write: non-POSIX filesystems (Windows)
     * don't support this API at all, so failures here are swallowed.
     */
    private static void applyTargetPermissions(Path target, Path parent, Path tmp) {
        try {
            Set<PosixFilePermission> perms;
            if (Files.exists(target)) {
                perms = Files.getPosixFilePermissions(target);
            } else {
                perms = new HashSet<>(Files.getPosixFilePermissions(parent));
                perms.removeAll(EnumSet.of(
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_EXECUTE));
            }
            Files.setPosixFilePermissions(tmp, perms);
        } catch (UnsupportedOperationException | IOException e) {
            // Non-POSIX filesystem, or permissions unreadable/unsettable — best-effort only.
        }
    }
}
