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
import java.io.InputStream;
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
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
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

    private static final Pattern MC_VERSION_RE = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");
    private static final Pattern SHA1_HEX_RE = Pattern.compile("[0-9a-fA-F]{40}");

    private static TranslationRegistry activeRegistry = null;

    // Tracks the in-flight async load so MyPetPlugin#onDisable can interrupt it before any
    // straggling GlobalTranslator mutation lands after the plugin has been disabled.
    private static volatile CompletableFuture<Void> currentFuture = null;

    public static CompletableFuture<Void> loadAsync(Plugin plugin) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                loadInternal(plugin);
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "Vanilla translation loader failed; pet default names will use English derived from entity type. Cause: "
                                + t.getMessage());
            }
        });
        currentFuture = future;
        return future;
    }

    /**
     * Cancels any in-flight {@link #loadAsync(Plugin)} task. Safe to call when no load is running.
     * Intended for invocation from {@code MyPetPlugin#onDisable} so a long-running CDN fetch on the
     * common ForkJoinPool does not survive past plugin shutdown and mutate the JVM-singleton
     * {@link GlobalTranslator} after the plugin has been disabled.
     */
    public static void cancelLoad() {
        CompletableFuture<Void> future = currentFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    private static void loadInternal(Plugin plugin) throws Exception {
        String mcVersion = Bukkit.getMinecraftVersion();
        // Skip the entire CDN dance when Bukkit's version string is missing or shaped unexpectedly —
        // the manifest scan would just loop the full versions array to no avail and then re-fire on
        // every startup. Cheaper to bail out with a single warning.
        if (mcVersion == null || !MC_VERSION_RE.matcher(mcVersion).matches()) {
            MyPetApi.getLogger().warning("Skipping vanilla translation load: unrecognized Minecraft version '"
                    + mcVersion + "'");
            return;
        }
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
        registerAllLocales(cacheDir, plugin);
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
        // Normalize so the path-traversal guard in downloadLocalesFromAssetIndex (which compares a
        // normalized target against this base) does not produce false negatives on data-folder paths
        // that contain '.' segments — common when the server is launched from a container or via a
        // wrapper script that resolves the working directory loosely.
        Path dir = plugin.getDataFolder().toPath()
                .resolve("cache")
                .resolve("vanilla-lang")
                .resolve(mcVersion)
                .normalize();
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
        if (manifestSha1.equalsIgnoreCase(hashes.get(CLIENT_JAR_SHA1_KEY))
                && Files.exists(cacheDir.resolve("en_us.json"))) {
            return false;
        }

        // Stream the client jar (~30-60 MB on modern MC) to a temp file in the cache directory
        // while computing SHA-1 incrementally via DigestInputStream. Keeps the full jar body out
        // of the heap and lets us verify integrity against Mojang's manifest hash before trusting
        // the extracted en_us.json. HttpClient's BodyHandlers.ofInputStream defers to the caller
        // to drain the body, which is exactly what we want for streaming-hash + streaming-extract.
        HttpRequest req = HttpRequest.newBuilder(URI.create(clientUrl))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) {
            try { res.body().close(); } catch (IOException ignored) {}
            throw new IOException("HTTP " + res.statusCode() + " fetching " + clientUrl);
        }
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        Path tmpJar = Files.createTempFile(cacheDir, "client-", ".jar");
        try {
            try (DigestInputStream dis = new DigestInputStream(res.body(), md)) {
                Files.copy(dis, tmpJar, StandardCopyOption.REPLACE_EXISTING);
            }
            String computedSha1 = HexFormat.of().formatHex(md.digest());
            if (!computedSha1.equalsIgnoreCase(manifestSha1)) {
                throw new IOException("Client jar SHA-1 mismatch: expected " + manifestSha1
                        + " got " + computedSha1 + " at " + clientUrl);
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(tmpJar))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!"assets/minecraft/lang/en_us.json".equalsIgnoreCase(entry.getName())) continue;
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    zip.transferTo(buf);
                    Path target = cacheDir.resolve("en_us.json");
                    Path stage = cacheDir.resolve("en_us.json.tmp");
                    Files.write(stage, buf.toByteArray());
                    // ATOMIC_MOVE guarantees the cache target is never observed mid-write across
                    // crashes; REPLACE_EXISTING lets a re-fetch overwrite a previous version's file
                    // in the same cache dir.
                    Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                    hashes.put(CLIENT_JAR_SHA1_KEY, manifestSha1);
                    return true;
                }
            }
            throw new IOException("en_us.json not found inside client jar at " + clientUrl);
        } finally {
            try {
                Files.deleteIfExists(tmpJar);
            } catch (IOException ignored) {}
        }
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

            // Defensive: a missing "hash" field, a non-string value, or a malformed hex string
            // would otherwise NPE / throw inside the substring/URL construction below and abort
            // the whole load via the outer catch — silently dropping every other locale.
            JsonElement hashElem = e.getValue().getAsJsonObject().get("hash");
            if (hashElem == null || hashElem.isJsonNull()
                    || !hashElem.isJsonPrimitive() || !hashElem.getAsJsonPrimitive().isString()) {
                MyPetApi.getLogger().warning("Skipping locale " + locale + " from asset index: missing or non-string 'hash' field");
                continue;
            }
            String hash = hashElem.getAsString();
            if (!SHA1_HEX_RE.matcher(hash).matches()) {
                MyPetApi.getLogger().warning("Skipping locale " + locale + " from asset index: malformed hash '" + hash + "'");
                continue;
            }
            Path target = cacheDir.resolve(locale + ".json").normalize();
            // Guard against a crafted index key whose normalized resolution escapes the cache directory.
            if (!target.startsWith(cacheDir)) {
                MyPetApi.getLogger().warning("Skipping suspicious locale key from asset index: " + locale);
                continue;
            }
            if (hash.equalsIgnoreCase(hashes.get(locale)) && Files.exists(target)) continue;

            String url = RESOURCES_URL + hash.substring(0, 2) + "/" + hash;
            byte[] bytes = fetchBytes(url);
            // Verify byte integrity against the asset-index declared hash before writing or
            // recording the cache key. A MITM or transient corruption that produces a 200 OK with
            // wrong bytes would otherwise be silently persisted and trusted on every future start.
            String actual = sha1Hex(bytes);
            if (!actual.equalsIgnoreCase(hash)) {
                MyPetApi.getLogger().warning("Locale " + locale + " SHA-1 mismatch: expected " + hash
                        + " got " + actual + "; skipping");
                continue;
            }
            Path stage = cacheDir.resolve(locale + ".json.tmp");
            Files.write(stage, bytes);
            Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            hashes.put(locale, hash);
            changed = true;
        }
        return changed;
    }

    private static void registerAllLocales(Path cacheDir, Plugin plugin) throws IOException {
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
        // If the plugin has been disabled while this fetch was in flight (e.g. /reload or rapid
        // restart), do not mutate the JVM-singleton GlobalTranslator — under a classloader-
        // preserving reloader the successor plugin instance has already registered its own
        // sources and a late add/remove here would corrupt them.
        if (plugin != null && !plugin.isEnabled()) {
            return;
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
        } catch (IOException | RuntimeException ex) {
            // RuntimeException covers JsonSyntaxException (corrupt JSON) and IllegalStateException
            // (root is not a JSON object) — both would otherwise propagate out of registerAllLocales
            // and abort the entire load, discarding every other locale that parsed cleanly.
            MyPetApi.getLogger().warning("Skipping malformed vanilla lang file "
                    + file.getFileName() + ": " + ex.getMessage());
        }
        return added;
    }
}
