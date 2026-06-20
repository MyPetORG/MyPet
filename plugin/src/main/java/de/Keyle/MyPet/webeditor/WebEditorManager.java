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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.Keyle.MyPet.MyPetApi;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Owns the single active {@link WebEditorSession} (one editor session per server,
 * per the design's single-session rule) and the shared {@link WebEditorKeystore}.
 * The {@code /mypet editor} command delegates here.
 *
 * <p>Trust persistence is self-contained: the keystore is lazily loaded from
 * {@code plugins/MyPet/editor-keys.json} on first use and re-saved whenever a new
 * browser is trusted. This avoids touching {@code MyPetPlugin}'s enable/disable
 * and means trust survives restarts.
 */
public final class WebEditorManager {

    private static final WebEditorManager INSTANCE = new WebEditorManager();
    private static final Gson GSON = new Gson();
    private static final Type STATE_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();
    private static final String KEYSTORE_FILE = "editor-keys.json";

    public static WebEditorManager getInstance() {
        return INSTANCE;
    }

    private final WebEditorKeystore keystore = new WebEditorKeystore();
    private WebEditorSession active;
    private boolean keystoreLoaded;

    private WebEditorManager() {
    }

    /** Open a new session for the player, enforcing one active session at a time. */
    public synchronized String open(Player player) throws Exception {
        loadKeystoreIfNeeded();
        if (active != null && !active.isClosed()) {
            throw new IllegalStateException("A web editor session is already active. Use /mypet editor close first.");
        }
        active = new WebEditorSession(player, keystore);
        return active.start();
    }

    /** Confirm a pending trust attempt; persists the keystore when a new browser is trusted. */
    public synchronized boolean trust(Player player, String code) throws Exception {
        if (active == null || active.isClosed()) {
            return false;
        }
        boolean confirmed = active.confirmTrust(player.getUniqueId(), code);
        if (confirmed) {
            persistKeystore();
        }
        return confirmed;
    }

    /** Close the active session, if any. */
    public synchronized boolean close(Player player) {
        if (active == null || active.isClosed()) {
            return false;
        }
        active.close();
        active = null;
        return true;
    }

    /** Close the active session on plugin shutdown so the browser stops showing it as live. */
    public synchronized void shutdown() {
        if (active != null && !active.isClosed()) {
            active.close(); // sends the `close` notice to the browser before tearing down
        }
        active = null;
    }

    public WebEditorKeystore getKeystore() {
        return keystore;
    }

    private void loadKeystoreIfNeeded() {
        if (keystoreLoaded) {
            return;
        }
        keystoreLoaded = true;
        File file = new File(MyPetApi.getPlugin().getDataFolder(), KEYSTORE_FILE);
        if (!file.exists()) {
            return;
        }
        try {
            Map<String, List<String>> state = GSON.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8), STATE_TYPE);
            if (state != null) {
                keystore.importState(state);
            }
        } catch (IOException | RuntimeException e) {
            MyPetApi.getLogger().warning("WebEditor: could not load " + KEYSTORE_FILE + ": " + e.getMessage());
        }
    }

    private void persistKeystore() {
        File file = new File(MyPetApi.getPlugin().getDataFolder(), KEYSTORE_FILE);
        try {
            Files.writeString(file.toPath(), GSON.toJson(keystore.exportState(), STATE_TYPE), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            MyPetApi.getLogger().warning("WebEditor: could not save " + KEYSTORE_FILE + ": " + e.getMessage());
        }
    }
}
