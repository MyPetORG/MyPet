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

package de.Keyle.MyPet.webeditor.socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Keyle.MyPet.MyPetApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Thin WebSocket transport over a Lucko bytesocks channel, built on the JDK
 * {@link WebSocket} (no OkHttp). Counterpart of the web {@code bytesocks.ts}.
 *
 * <p>Responsibilities: create a channel, connect, send text frames, and
 * reassemble inbound frames (bytesocks may split a message across {@code onText}
 * calls) before handing each full message to the supplied consumer.
 *
 * <p>Channel creation is {@code GET /create} (verified against lucko/bytesocks),
 * returning the key in both a {@code {"key":"..."}} body and a {@code Location}
 * header; the WebSocket then connects to {@code <wss>/<channelId>}.
 */
public final class WebEditorSocket {

    private final HttpClient http = HttpClient.newHttpClient();
    private final Consumer<String> onMessage;
    private final Runnable onClose;
    private final StringBuilder buffer = new StringBuilder();
    private volatile WebSocket webSocket;

    public WebEditorSocket(Consumer<String> onMessage, Runnable onClose) {
        this.onMessage = onMessage;
        this.onClose = onClose;
    }

    /** Create a fresh bytesocks channel; returns its id. */
    public String createChannel(String bytesocksUrl) throws IOException, InterruptedException {
        String httpBase = bytesocksUrl.replaceFirst("^ws", "http"); // ws->http, wss->https
        // bytesocks channel creation is GET /create (verified against lucko/bytesocks).
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(httpBase) + "/create"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("bytesocks create failed: HTTP " + response.statusCode());
        }
        // The channel key is returned both as a {"key":"..."} body and a Location header.
        try {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            if (body.has("key")) {
                return body.get("key").getAsString();
            }
        } catch (RuntimeException ignored) {
            // not JSON — fall back to the Location header below
        }
        String location = response.headers().firstValue("Location").orElse("");
        if (!location.isBlank()) {
            return location.replaceFirst("^/", "");
        }
        throw new IOException("bytesocks create returned no channel key");
    }

    /** Connect to an existing channel. */
    public void connect(String bytesocksUrl, String channelId) {
        URI uri = URI.create(trimTrailingSlash(bytesocksUrl) + "/" + channelId);
        this.webSocket = http.newWebSocketBuilder().buildAsync(uri, new Listener()).join();
    }

    /** Send a complete text frame. */
    public void send(String text) {
        WebSocket ws = this.webSocket;
        if (ws != null) {
            ws.sendText(text, true);
        }
    }

    /** Politely close the channel. */
    public void close() {
        WebSocket ws = this.webSocket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "session closed");
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private final class Listener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                try {
                    onMessage.accept(message);
                } catch (RuntimeException e) {
                    MyPetApi.getLogger().warning("WebEditor: error handling message: " + e.getMessage());
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            if (onClose != null) {
                onClose.run();
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            MyPetApi.getLogger().warning("WebEditor: socket error: " + error.getMessage());
        }
    }
}
