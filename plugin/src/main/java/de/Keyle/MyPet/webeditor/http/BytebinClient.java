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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP client for Lucko's bytebin relay (a key/value blob store), built on the
 * JDK {@link HttpClient} — no OkHttp, no new dependencies. Counterpart of the
 * web editor's {@code bytebin.ts}.
 *
 * <ul>
 *   <li>{@link #post}: POST a blob to {@code <base>/post}, returns the new key.</li>
 *   <li>{@link #get}: GET {@code <base>/<key>}, returns the raw body.</li>
 * </ul>
 *
 * <p>bytebin returns the new key both in a {@code Location} header and a JSON
 * body {@code {"key":"..."}}; we read the header first and fall back to the body
 * so no JSON dependency is needed here. Contract-based so the relay stays
 * swappable (see the "dumb relay" ADR).
 */
public final class BytebinClient {

    private static final Pattern KEY_PATTERN = Pattern.compile("\"key\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient http;
    private final String baseUrl;

    public BytebinClient(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** POST a blob; returns the assigned bytebin key. */
    public String post(String body, String contentType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/post"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("bytebin POST failed: HTTP " + response.statusCode());
        }
        return extractKey(response);
    }

    /** GET a blob by key; returns the raw body text. */
    public String get(String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + key))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("bytebin GET failed: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String extractKey(HttpResponse<String> response) throws IOException {
        Optional<String> location = response.headers().firstValue("Location");
        if (location.isPresent() && !location.get().isBlank()) {
            return location.get().replaceFirst("^/", "");
        }
        Matcher matcher = KEY_PATTERN.matcher(response.body());
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IOException("bytebin POST returned no key (no Location header or JSON key)");
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
