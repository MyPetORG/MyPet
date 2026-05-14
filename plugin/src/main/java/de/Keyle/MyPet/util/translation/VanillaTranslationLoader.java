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

package de.Keyle.MyPet.util.translation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import de.Keyle.MyPet.MyPetApi;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fetches Mojang's vanilla translation files at runtime from launcher meta +
 * the asset CDN, then registers them with Adventure's {@link GlobalTranslator}
 * so {@code Component.translatable("entity.minecraft.<type>")} resolves to a
 * locale-correct string server-side.
 *
 * <p>Failure modes (offline server, Mojang CDN down, MC version not in
 * manifest, malformed JSON) are absorbed: the loader logs a warning and the
 * resolver falls through to {@link #englishFromEntityKey(EntityType)} which
 * mechanically derives an English string from the entity's enum value.</p>
 *
 * <p>No data is ever bundled with the plugin JAR. The plugin acts as an
 * asset-fetching agent on behalf of the running server, using the same public
 * endpoints the official launcher uses.</p>
 */
public final class VanillaTranslationLoader {

    private VanillaTranslationLoader() {}

    private static final String VERSION_MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String RESOURCES_URL =
            "https://resources.download.minecraft.net/";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final String CLIENT_JAR_SHA1_KEY = "__client_jar_sha1";

    private static final Key TRANSLATOR_KEY = Key.key("mypet", "vanilla");
    private static final String ENTITY_KEY_PREFIX = "entity.minecraft.";

    private static TranslationRegistry activeRegistry = null;

    public static CompletableFuture<Void> loadAsync(Plugin plugin) {
        return CompletableFuture.runAsync(() -> {
            try {
                loadInternal(plugin);
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "Vanilla translation loader failed; pet default names will use English derived from entity type. Cause: "
                                + t.getMessage());
            }
        });
    }

    private static void loadInternal(Plugin plugin) throws Exception {
        String mcVersion = Bukkit.getMinecraftVersion();
        Path cacheDir = cacheDirFor(plugin, mcVersion);
        Map<String, String> hashes = readHashes(cacheDir);

        JsonObject manifest = fetchJson(VERSION_MANIFEST_URL);
        String versionUrl = null;
        for (JsonElement v : manifest.getAsJsonArray("versions")) {
            JsonObject entry = v.getAsJsonObject();
            if (mcVersion.equalsIgnoreCase(entry.get("id").getAsString())) {
                versionUrl = entry.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("No version manifest entry for Minecraft " + mcVersion);
        }
        JsonObject versionMeta = fetchJson(versionUrl);

        String clientUrl = versionMeta
                .getAsJsonObject("downloads")
                .getAsJsonObject("client")
                .get("url").getAsString();
        String clientSha1 = versionMeta
                .getAsJsonObject("downloads")
                .getAsJsonObject("client")
                .get("sha1").getAsString();
        boolean enUsChanged = downloadEnUsFromClientJar(cacheDir, clientUrl, clientSha1, hashes);

        String indexUrl = versionMeta
                .getAsJsonObject("assetIndex")
                .get("url").getAsString();
        boolean localesChanged = downloadLocalesFromAssetIndex(cacheDir, indexUrl, hashes);

        if (enUsChanged || localesChanged) {
            writeHashes(cacheDir, hashes);
        }
        registerAllLocales(cacheDir);
    }

    public static String resolveEntityName(EntityType type, String localeTag) {
        String key = type.translationKey();
        Locale loc = parseLocale(localeTag);
        Component rendered = GlobalTranslator.render(Component.translatable(key), loc);
        String text = PlainTextComponentSerializer.plainText().serialize(rendered);
        if (text.equals(key)) {
            return englishFromEntityKey(type);
        }
        return text;
    }

    public static String englishFromEntityKey(EntityType type) {
        NamespacedKey nk = type.getKey();
        String name = nk.value();
        StringBuilder sb = new StringBuilder(name.length());
        boolean cap = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Locale parseLocale(String tag) {
        if (tag == null || tag.isEmpty()) return Locale.US;
        String[] parts = tag.toLowerCase(Locale.ROOT).split("[_\\-]");
        if (parts.length >= 2) return Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT));
        return Locale.of(parts[0]);
    }

    private static Path cacheDirFor(Plugin plugin, String mcVersion) throws IOException {
        Path dir = plugin.getDataFolder().toPath()
                .resolve("cache")
                .resolve("vanilla-lang")
                .resolve(mcVersion);
        Files.createDirectories(dir);
        return dir;
    }

    private static Map<String, String> readHashes(Path cacheDir) throws IOException {
        Path file = cacheDir.resolve("hashes.json");
        Map<String, String> out = new HashMap<>();
        if (!Files.exists(file)) return out;
        try (Reader rdr = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(rdr).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                out.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return out;
    }

    private static void writeHashes(Path cacheDir, Map<String, String> hashes) throws IOException {
        Path file = cacheDir.resolve("hashes.json");
        try (JsonWriter w = new JsonWriter(new OutputStreamWriter(
                Files.newOutputStream(file), StandardCharsets.UTF_8))) {
            w.beginObject();
            for (Map.Entry<String, String> e : hashes.entrySet()) {
                w.name(e.getKey()).value(e.getValue());
            }
            w.endObject();
        }
    }

    private static byte[] fetchBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("HTTP " + res.statusCode() + " fetching " + url);
        }
        return res.body();
    }

    private static JsonObject fetchJson(String url) throws IOException, InterruptedException {
        byte[] body = fetchBytes(url);
        try (Reader rdr = new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(rdr).getAsJsonObject();
        }
    }

    private static String sha1Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
    }

    private static boolean downloadEnUsFromClientJar(Path cacheDir, String clientUrl,
                                                     String manifestSha1,
                                                     Map<String, String> hashes) throws Exception {
        if (manifestSha1.equals(hashes.get(CLIENT_JAR_SHA1_KEY))
                && Files.exists(cacheDir.resolve("en_us.json"))) {
            return false;
        }
        byte[] clientJar = fetchBytes(clientUrl);
        String hash = sha1Hex(clientJar);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(clientJar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("assets/minecraft/lang/en_us.json".equalsIgnoreCase(entry.getName())) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    zip.transferTo(buf);
                    Files.write(cacheDir.resolve("en_us.json"), buf.toByteArray());
                    hashes.put(CLIENT_JAR_SHA1_KEY, hash);
                    return true;
                }
            }
        }
        throw new IOException("en_us.json not found inside client jar at " + clientUrl);
    }

    private static boolean downloadLocalesFromAssetIndex(Path cacheDir, String indexUrl,
                                                         Map<String, String> hashes) throws Exception {
        JsonObject index = fetchJson(indexUrl);
        JsonObject objects = index.getAsJsonObject("objects");
        boolean changed = false;
        for (Map.Entry<String, JsonElement> e : objects.entrySet()) {
            String key = e.getKey().toLowerCase(Locale.ROOT);
            if (!key.startsWith("minecraft/lang/") || !key.endsWith(".json")) continue;
            String locale = key.substring("minecraft/lang/".length(), key.length() - ".json".length());
            if ("en_us".equals(locale)) continue; // already pulled from the client jar

            String hash = e.getValue().getAsJsonObject().get("hash").getAsString();
            Path target = cacheDir.resolve(locale + ".json").normalize();
            // Guard against a crafted index key whose normalized resolution escapes the cache directory.
            if (!target.startsWith(cacheDir)) {
                MyPetApi.getLogger().warning("Skipping suspicious locale key from asset index: " + locale);
                continue;
            }
            if (hash.equals(hashes.get(locale)) && Files.exists(target)) continue;

            String url = RESOURCES_URL + hash.substring(0, 2) + "/" + hash;
            Files.write(target, fetchBytes(url));
            hashes.put(locale, hash);
            changed = true;
        }
        return changed;
    }

    private static void registerAllLocales(Path cacheDir) throws IOException {
        TranslationRegistry registry = TranslationRegistry.create(TRANSLATOR_KEY);
        int localeCount = 0;
        int keyCount = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(cacheDir, "*.json")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                if ("hashes.json".equals(name)) continue;
                String localeTag = name.substring(0, name.length() - ".json".length());
                Locale loc = parseLocale(localeTag);
                int added = registerOneLocale(registry, loc, file);
                if (added > 0) {
                    localeCount++;
                    keyCount += added;
                }
            }
        }
        // Remove any registry from a prior load (e.g. across /reload) so GlobalTranslator
        // doesn't accumulate sources across the JVM-singleton lifetime.
        if (activeRegistry != null) {
            GlobalTranslator.translator().removeSource(activeRegistry);
        }
        GlobalTranslator.translator().addSource(registry);
        activeRegistry = registry;
        MyPetApi.getLogger().info("Loaded " + keyCount + " vanilla entity translations across "
                + localeCount + " locales");
    }

    private static int registerOneLocale(TranslationRegistry registry, Locale loc, Path file) {
        int added = 0;
        try (Reader rdr = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(rdr).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getKey().startsWith(ENTITY_KEY_PREFIX)) continue;
                JsonElement v = e.getValue();
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) continue;
                String escaped = v.getAsString().replace("'", "''");
                try {
                    registry.register(e.getKey(), loc, new MessageFormat(escaped, loc));
                    added++;
                } catch (IllegalArgumentException ignored) {
                    // Skip keys with malformed MessageFormat patterns; no entity name needs args.
                }
            }
        } catch (IOException ex) {
            MyPetApi.getLogger().warning("Skipping malformed vanilla lang file "
                    + file.getFileName() + ": " + ex.getMessage());
        }
        return added;
    }
}
