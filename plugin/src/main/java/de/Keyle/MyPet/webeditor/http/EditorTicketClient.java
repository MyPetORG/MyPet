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

package de.Keyle.MyPet.webeditor.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.util.BuiltByBitInfo;
import de.Keyle.MyPet.util.HubInfo;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Exchanges the entitlement evidence baked into this jar for a short-lived editor ticket from
 * the MyPet Hub. Jars with no evidence (local builds, self-compiled copies) never call out at
 * all — mirroring the updater's "skip for local build" rule.
 *
 * <p>Carries no policy: any failure returns empty and the session start proceeds without a
 * ticket. Whether that session is allowed is the relay's decision, not the plugin's.
 */
public final class EditorTicketClient {

    private static final String TICKET_PATH = "/api/v1/editor/ticket";

    /** The ticket for this server, or empty when there is no evidence or the Hub said no. */
    public Optional<String> requestTicket() {
        String query = buildQuery();
        if (query == null) {
            return Optional.empty();
        }
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(HubInfo.HUB_BASE + TICKET_PATH + "?" + query))
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                MyPetApi.getLogger().warning("WebEditor: ticket refused (HTTP "
                        + response.statusCode() + "): " + response.body());
                return Optional.empty();
            }
            JsonObject body = new Gson().fromJson(response.body(), JsonObject.class);
            if (body == null || !body.has("ticket")) {
                MyPetApi.getLogger().warning("WebEditor: ticket response had no ticket field");
                return Optional.empty();
            }
            return Optional.of(body.get("ticket").getAsString());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            MyPetApi.getLogger().warning("WebEditor: ticket request failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Query string for whichever evidence this jar carries, or null when it carries none. */
    private static String buildQuery() {
        if (BuiltByBitInfo.isInjected()) {
            return "member=" + encode(BuiltByBitInfo.memberId())
                    + "&nonce=" + encode(BuiltByBitInfo.nonce())
                    + "&timestamp=" + encode(BuiltByBitInfo.timestamp());
        }
        if (HubInfo.isInjected()) {
            return "discord=" + encode(HubInfo.discordId())
                    + "&nonce=" + encode(HubInfo.nonce());
        }
        return null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
