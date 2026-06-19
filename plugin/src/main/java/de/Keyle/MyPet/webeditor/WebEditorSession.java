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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.webeditor.http.BytebinClient;
import de.Keyle.MyPet.webeditor.socket.SignatureAlgorithm;
import de.Keyle.MyPet.webeditor.socket.WebEditorSocket;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

/**
 * One live {@code /mypet editor} session: owns the per-session ECDSA keypair,
 * the bytesocks socket, and the protocol dispatch (the server half of the web
 * {@code session.ts} state machine — the spec's separate Handler classes are
 * consolidated here as {@code handle*} methods).
 *
 * <p>Threading: socket callbacks arrive off the main thread. Reads/writes of
 * files are fine off-thread; the config <em>reload</em> runs on the global
 * region scheduler (Folia-safe), and the follow-up bytebin upload runs on the
 * async scheduler so we never block a server thread on network I/O.
 */
public final class WebEditorSession {

    private static final int PROTOCOL_VERSION = 1;
    private static final long INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000L; // 10 minutes
    private static final long CHECK_PERIOD_TICKS = 600L;               // 30s

    private final UUID owner;
    private final WebEditorKeystore keystore;
    private final KeyPair keyPair;
    private final BytebinClient bytebin;
    private final ConfigSerializer serializer = new ConfigSerializer();
    private final ConfigApplier applier = new ConfigApplier();

    private WebEditorSocket socket;
    private PublicKey remoteKey;          // browser key, set once trusted
    private String pendingPublicKey;      // candidate awaiting in-game trust
    private String pendingNonce;
    private volatile boolean closed;
    private volatile long lastActivity = System.currentTimeMillis();
    private ScheduledTask inactivityTask;

    public WebEditorSession(Player player, WebEditorKeystore keystore) {
        this.owner = player.getUniqueId();
        this.keystore = keystore;
        this.keyPair = SignatureAlgorithm.generateKeyPair();
        this.bytebin = new BytebinClient(MyPetGlobal.WebEditor.BYTEBIN_URL.get());
    }

    public boolean isClosed() {
        return closed;
    }

    /** Serialize configs, upload the envelope, open the channel; returns the editor URL. */
    public String start() throws Exception {
        String bytesocksUrl = MyPetGlobal.WebEditor.BYTESOCKS_URL.get();
        socket = new WebEditorSocket(this::onMessage, this::onSocketClosed);
        String channelId = socket.createChannel(bytesocksUrl);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("pluginVersion", MyPetApi.getPlugin().getPluginMeta().getVersion());
        metadata.addProperty("serverVersion", Bukkit.getServer().getMinecraftVersion());
        metadata.addProperty("timestamp", System.currentTimeMillis());

        JsonObject socketInfo = new JsonObject();
        socketInfo.addProperty("protocolVersion", PROTOCOL_VERSION);
        socketInfo.addProperty("channelId", channelId);
        socketInfo.addProperty("publicKey", SignatureAlgorithm.exportPublicKey(keyPair.getPublic()));

        JsonObject envelope = new JsonObject();
        envelope.add("metadata", metadata);
        envelope.add("socket", socketInfo);
        envelope.add("configs", serializer.serializeConfigs());

        String key = bytebin.post(envelope.toString(), "application/json");
        socket.connect(bytesocksUrl, channelId);
        startInactivityWatch();
        return trimTrailingSlash(MyPetGlobal.WebEditor.EDITOR_URL.get()) + "/#" + key;
    }

    /** Confirm a pending trust attempt (admin ran {@code /mypet editor trust <code>}). */
    public boolean confirmTrust(String code) throws Exception {
        if (keystore.confirmTrust(code).isEmpty()) {
            return false;
        }
        if (pendingPublicKey != null && code.equals(pendingNonce)) {
            remoteKey = SignatureAlgorithm.importPublicKey(pendingPublicKey);
            pendingPublicKey = null;
            pendingNonce = null;
        }
        sendHelloReply("trusted");
        return true;
    }

    /** Intentional close (command / timeout): tell the browser, then tear down. */
    public void close() {
        if (closed) {
            return;
        }
        sendCloseNotice();
        shutdown();
    }

    /** Notify the browser the session is ending so it disables (doesn't reconnect). */
    private void sendCloseNotice() {
        if (remoteKey != null) {
            try {
                sendSigned(typed("close"));
            } catch (RuntimeException ignored) {
                // socket may already be gone — we tear down regardless
            }
        }
    }

    /** Tear down without notifying (socket already closed, or browser-initiated). */
    private void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        if (inactivityTask != null) {
            inactivityTask.cancel();
        }
        if (socket != null) {
            socket.close();
        }
    }

    // --- incoming dispatch --------------------------------------------------

    private void onMessage(String raw) {
        try {
            lastActivity = System.currentTimeMillis(); // any inbound message resets the inactivity timer
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();

            // hello is the only unsigned message (browser → plugin).
            if (obj.has("type") && "hello".equals(obj.get("type").getAsString())) {
                handleHello(obj);
                return;
            }

            // Everything else is a signed { msg, signature } envelope.
            if (!obj.has("msg") || !obj.has("signature") || remoteKey == null) {
                return;
            }
            String msgStr = obj.get("msg").getAsString();
            if (!SignatureAlgorithm.verify(remoteKey, msgStr, obj.get("signature").getAsString())) {
                MyPetApi.getLogger().warning("WebEditor: dropped message with invalid signature");
                return;
            }
            JsonObject msg = JsonParser.parseString(msgStr).getAsJsonObject();
            switch (msg.get("type").getAsString()) {
                case "ping" -> sendSigned(pong());
                case "change-request" -> handleChangeRequest(msg);
                case "close" -> shutdown();
                default -> {
                    // "connected" and unknown types: nothing to do
                }
            }
        } catch (Exception e) {
            MyPetApi.getLogger().warning("WebEditor: failed to handle message: " + e.getMessage());
        }
    }

    private void handleHello(JsonObject hello) throws Exception {
        String publicKey = hello.get("publicKey").getAsString();
        String nonce = hello.get("nonce").getAsString();
        if (keystore.isTrusted(owner, publicKey)) {
            remoteKey = SignatureAlgorithm.importPublicKey(publicKey);
            sendHelloReply("accepted");
        } else {
            pendingPublicKey = publicKey;
            pendingNonce = nonce;
            keystore.beginTrustAttempt(nonce, owner, publicKey);
            sendHelloReply("untrusted");
            Player player = Bukkit.getPlayer(owner);
            if (player != null) {
                player.sendMessage(Component.text("Run /mypet editor trust " + nonce + " to authorize the web editor."));
            }
        }
    }

    private void handleChangeRequest(JsonObject msg) throws Exception {
        sendSigned(typed("change-response", "accepted", null));

        String payloadKey = msg.get("key").getAsString();
        JsonObject payload = JsonParser.parseString(bytebin.get(payloadKey)).getAsJsonObject();
        JsonObject changedConfigs = payload.getAsJsonObject("configs");
        // Diff against the current on-disk files BEFORE they're overwritten.
        List<Component> changes = WebEditorChanges.summarize(MyPetApi.getPlugin().getDataFolder(), changedConfigs);
        applier.writeChanges(changedConfigs); // file I/O — safe off the main thread

        // Reload touches plugin state → global region scheduler (Folia-safe);
        // then re-serialize on that thread and upload async so we never block a tick on I/O.
        Bukkit.getGlobalRegionScheduler().run(MyPetApi.getPlugin(), task -> {
            applier.reload();
            WebEditorChanges.broadcast(ownerName(), changes); // notify console + mypet.admin.notify
            JsonObject snapshot = new JsonObject();
            snapshot.add("configs", serializer.serializeConfigs());
            Bukkit.getAsyncScheduler().runNow(MyPetApi.getPlugin(), asyncTask -> {
                try {
                    String newKey = bytebin.post(snapshot.toString(), "application/json");
                    sendSigned(typed("change-response", "applied", newKey));
                } catch (Exception e) {
                    MyPetApi.getLogger().warning("WebEditor: failed to publish applied snapshot: " + e.getMessage());
                }
            });
        });
    }

    private void onSocketClosed() {
        // Socket already gone — just clean up (frees the single-session lock); no notice to send.
        shutdown();
    }

    private void startInactivityWatch() {
        inactivityTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(MyPetApi.getPlugin(), task -> {
            if (closed) {
                task.cancel();
                return;
            }
            if (System.currentTimeMillis() > expiresAt()) {
                MyPetApi.getLogger().info("WebEditor: session for " + owner + " timed out after inactivity.");
                Player player = Bukkit.getPlayer(owner);
                if (player != null) {
                    player.sendMessage(Component.text("Your MyPet web editor session expired after 10 minutes of inactivity."));
                }
                close();
            }
        }, CHECK_PERIOD_TICKS, CHECK_PERIOD_TICKS);
    }

    private long expiresAt() {
        return lastActivity + INACTIVITY_TIMEOUT_MS;
    }

    /** Best-effort display name for the session owner (for the apply broadcast). */
    private String ownerName() {
        Player online = Bukkit.getPlayer(owner);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(owner).getName();
        return name != null ? name : owner.toString();
    }

    // --- outgoing helpers ---------------------------------------------------

    private void sendHelloReply(String state) {
        JsonObject reply = new JsonObject();
        reply.addProperty("type", "hello-reply");
        reply.addProperty("state", state);
        if (state.equals("accepted") || state.equals("trusted")) {
            reply.addProperty("expiresAt", expiresAt());
        }
        sendSigned(reply);
    }

    private JsonObject pong() {
        JsonObject pong = typed("pong");
        pong.addProperty("expiresAt", expiresAt());
        return pong;
    }

    private void sendSigned(JsonObject message) {
        String msgStr = message.toString();
        JsonObject envelope = new JsonObject();
        envelope.addProperty("msg", msgStr);
        envelope.addProperty("signature", SignatureAlgorithm.sign(keyPair.getPrivate(), msgStr));
        socket.send(envelope.toString());
    }

    private static JsonObject typed(String type) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        return obj;
    }

    private static JsonObject typed(String type, String state, String newSessionCode) {
        JsonObject obj = typed(type);
        obj.addProperty("state", state);
        if (newSessionCode != null) {
            obj.addProperty("newSessionCode", newSessionCode);
        }
        return obj;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
