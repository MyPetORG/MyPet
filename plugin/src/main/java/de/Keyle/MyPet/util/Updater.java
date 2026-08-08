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

package de.Keyle.MyPet.util;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.util.ErrorUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;

/**
 * Checks for MyPet updates against BuiltByBit and, for BBB-downloaded copies, downloads the
 * matching build from the MyPet Hub with SHA-512 verification. Copies obtained elsewhere
 * (Spigot, self-compiled, etc.) still see the version-check result — BBB version numbers are
 * public — but are pointed at the BBB resource page instead of an automatic download.
 */
public class Updater {

    public class Update {

        String version;
        String downloadURL;
        String sha512Hash;

        public Update(String version, String downloadURL, String sha512Hash) {
            this.version = version;
            this.downloadURL = downloadURL;
            this.sha512Hash = sha512Hash;
        }

        public String getVersion() {
            return version;
        }

        public String getDownloadURL() { return downloadURL; }

        public String getSha512Hash() { return sha512Hash; }

        @Override
        public String toString() {
            return version;
        }
    }

    /** BuiltByBit resource id for the MyPet resource page. */
    private static final long RESOURCE_ID = 115339;
    private static final String BBB_API_BASE = "https://api.builtbybit.com/v1";
    private static final String VOXEL_API_BASE = "https://api.voxel.shop/v1";
    /** Public BuiltByBit resource page, shown to non-BBB copies instead of an auto-download. */
    public static final String RESOURCE_PAGE_URL = "https://builtbybit.com/resources/" + RESOURCE_ID + "/";

    protected static volatile Update latest = null;
    protected String plugin;
    protected Thread thread;
    /** Background thread running an async version check (dev checks, or BBB/Voxel checks with auto-download off). */
    protected Thread checkThread;
    private boolean checkFailed = false;

    public Updater(String plugin) {
        this.plugin = plugin;
        latest = null;
    }

    public static Update getLatest() {
        return latest;
    }

    public static boolean isUpdateAvailable() {
        return latest != null;
    }

    /**
     * Checks for updates and returns a status message for display in the splash screen.
     * Dev-build checks (including Hub-stamped auto-download) and BBB or Voxel checks with
     * auto-download disabled run asynchronously on {@link #checkThread} and log their own
     * result — including any auto-download they trigger, which runs on {@link #thread}. Only
     * the BBB or Voxel release path with auto-download enabled stays synchronous. {@link #waitForDownload()}
     * waits for both threads before shutdown, regardless of which path was taken.
     *
     * @return a status message, or null if update checking is disabled or runs asynchronously
     */
    public String update() {
        if (!MyPetGlobal.Update.CHECK.get()) {
            return null;
        }

        if (VersionUtil.isLocalBuild()) {
            return "Skipping update check for local build.";
        }

        if (VersionUtil.isDevBuild()) {
            checkThread = new Thread(() -> {
                String result = runDevCheck();
                if (result != null) {
                    MyPetApi.getLogger().info(result);
                }
            }, "MyPet-UpdateCheck");
            checkThread.setDaemon(true);
            checkThread.start();
            return null;
        }

        if (VoxelInfo.isInjected()) {
            if (!MyPetGlobal.Update.DOWNLOAD.get()) {
                checkThread = new Thread(() -> {
                    MyPetApi.getLogger().info(runVoxelCheck());
                }, "MyPet-UpdateCheck");
                checkThread.setDaemon(true);
                checkThread.start();
                return null;
            }
            return runVoxelCheck();
        }

        if (BuiltByBitInfo.sharedToken().isEmpty()) {
            return "Skipping update check: no BuiltByBit token bundled with this build.";
        }

        if (!MyPetGlobal.Update.DOWNLOAD.get()) {
            checkThread = new Thread(() -> {
                MyPetApi.getLogger().info(runCheck());
            }, "MyPet-UpdateCheck");
            checkThread.setDaemon(true);
            checkThread.start();
            return null;
        }

        return runCheck();
    }

    private String runCheck() {
        Optional<Update> update = check();
        if (checkFailed) {
            return "Update check failed.";
        }

        if (update.isPresent()) {
            latest = update.get();

            if (latest.getDownloadURL() != null) {
                if (MyPetGlobal.Update.DOWNLOAD.get()) {
                    download();
                }
                return "A new version is available: " + latest;
            }
            return "A new version is available: " + latest + ". Download it from " + RESOURCE_PAGE_URL;
        }
        return "No update available.";
    }

    /**
     * Dev builds never auto-download; they check the Hub's public manifest (dev + release
     * channels, no token needed) and report where to get the newer build.
     */
    private String runDevCheck() {
        String currentVersion = VersionUtil.getVersion();
        String newest = null;
        for (String channel : new String[]{"dev", "release"}) {
            String candidate = newestHubVersion(channel);
            if (candidate != null && isNewerVersion(candidate, newest == null ? currentVersion : newest)) {
                newest = candidate;
            }
        }
        if (newest == null) {
            return "No newer build available.";
        }
        if (preReleaseTier(newest) == 2) {
            if (HubInfo.isInjected() && MyPetGlobal.Update.DOWNLOAD.get()) {
                Optional<Update> hubUpdate = fetchHubManifest(newest, hubEntitlementQuery());
                if (hubUpdate.isPresent() && hubUpdate.get().getDownloadURL() != null) {
                    latest = hubUpdate.get();
                    MyPetApi.getLogger().info("A newer release is available: " + newest + ". Downloading from the MyPet Hub...");
                    download();
                    return null;
                }
            }
            latest = new Update(newest, null, null);
            if (HubInfo.isInjected()) {
                return "A newer release is available: " + newest + ". Grab it from the MyPet Discord.";
            }
            return "A newer release is available: " + newest + ". Download it from " + RESOURCE_PAGE_URL;
        }
        if (HubInfo.isInjected() && MyPetGlobal.Update.DOWNLOAD.get()) {
            String downloadUrl = HubInfo.HUB_BASE + "/api/v1/updater/dev-download/" + urlEncode(newest)
                    + "?discord=" + urlEncode(HubInfo.discordId())
                    + "&nonce=" + urlEncode(HubInfo.nonce());
            latest = new Update(newest, downloadUrl, null);
            MyPetApi.getLogger().info("A newer dev build is available: " + newest + ". Downloading from the MyPet Hub...");
            download();
            return null;
        }
        latest = new Update(newest, null, null);
        return "A newer dev build is available: " + newest + ". Grab it from the MyPet Discord (#alpha-builds).";
    }

    /** Checks the Voxel.Shop listing for a newer release; auto-download when enabled. */
    private String runVoxelCheck() {
        String latestVersion = latestVoxelVersion();
        if (latestVersion == null) {
            return "Update check failed.";
        }
        if (!isNewerVersion(latestVersion, VersionUtil.getVersion())) {
            return "No update available.";
        }
        latest = new Update(latestVersion, null, null);
        if (MyPetGlobal.Update.DOWNLOAD.get()) {
            Optional<Update> hubUpdate = fetchHubManifest(latestVersion, VoxelInfo.evidenceQuery());
            if (hubUpdate.isPresent() && hubUpdate.get().getDownloadURL() != null) {
                latest = hubUpdate.get();
                download();
                return "A new version is available: " + latest;
            }
        }
        latest = new Update(latestVersion, null, null);
        return "A new version is available: " + latestVersion
                + ". Download it from https://voxel.shop/product/" + VoxelInfo.resource() + "/";
    }

    /** Newest version on the Voxel.Shop listing, or null when unavailable. */
    private String latestVoxelVersion() {
        try {
            String content = httpGet(VOXEL_API_BASE + "/getResourceUpdates?resource_id="
                    + urlEncode(VoxelInfo.resource()) + "&limit=1", null);
            JsonObject root = new Gson().fromJson(content, JsonObject.class);
            JsonArray updates = root.getAsJsonObject("response").getAsJsonArray("updates");
            if (updates == null || updates.size() == 0) {
                return null;
            }
            return optString(updates.get(0).getAsJsonObject(), "version");
        } catch (Exception e) {
            MyPetApi.getLogger().warning("Voxel.Shop version check failed: " + e.getMessage());
            return null;
        }
    }

    /** Newest version in the Hub manifest for {@code channel}, or null when unavailable. */
    private String newestHubVersion(String channel) {
        try {
            String content = httpGet(HubInfo.HUB_BASE + "/api/v1/versions?channel=" + channel, null);
            JsonObject root = new Gson().fromJson(content, JsonObject.class);
            JsonArray versions = root != null ? root.getAsJsonArray("versions") : null;
            if (versions == null || versions.size() == 0) {
                return null;
            }
            return optString(versions.get(0).getAsJsonObject(), "version");
        } catch (Exception e) {
            MyPetApi.getLogger().warning("Hub version check failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Entitlement query for the Hub's release download endpoint, or null when this jar carries no evidence.
     */
    private static String hubEntitlementQuery() {
        if (BuiltByBitInfo.isInjected()) {
            return "member=" + urlEncode(BuiltByBitInfo.memberId())
                    + "&nonce=" + urlEncode(BuiltByBitInfo.nonce())
                    + "&timestamp=" + urlEncode(BuiltByBitInfo.timestamp());
        }
        if (HubInfo.isInjected()) {
            return "discord=" + urlEncode(HubInfo.discordId())
                    + "&nonce=" + urlEncode(HubInfo.nonce());
        }
        return null;
    }

    protected Optional<Update> check() {
        checkFailed = false;
        try {
            String currentVersion = VersionUtil.getVersion();
            String token = BuiltByBitInfo.sharedToken().orElse(null);
            if (token == null) {
                return Optional.empty();
            }

            String content = httpGet(BBB_API_BASE + "/resources/" + RESOURCE_ID + "/versions/latest", "Shared " + token);
            JsonObject root = new Gson().fromJson(content, JsonObject.class);
            if (root == null || !"success".equals(optString(root, "result"))) {
                checkFailed = true;
                return Optional.empty();
            }

            JsonObject data = root.getAsJsonObject("data");
            String latestVersion = data.get("name").getAsString();

            if (!isNewerVersion(latestVersion, currentVersion)) {
                return Optional.empty();
            }

            String query = hubEntitlementQuery();
            if (query != null) {
                Optional<Update> hubUpdate = fetchHubManifest(latestVersion, query);
                if (hubUpdate.isPresent()) {
                    return hubUpdate;
                }
            }

            return Optional.of(new Update(latestVersion, null, null));
        } catch (Exception e) {
            MyPetApi.getLogger().warning("BuiltByBit update check failed: " + e.getMessage());
            checkFailed = true;
            return Optional.empty();
        }
    }

    /**
     * Looks up the download URL and SHA-512 hash for {@code version} from the Hub's public
     * version manifest, and builds the entitled download URL from the provided entitlementQuery.
     * Serves any Hub-entitled copy (BBB or Hub-stamped).
     */
    private Optional<Update> fetchHubManifest(String version, String entitlementQuery) {
        try {
            String content = httpGet(HubInfo.HUB_BASE + "/api/v1/versions?channel=release", null);
            JsonObject root = new Gson().fromJson(content, JsonObject.class);
            JsonArray versions = root != null ? root.getAsJsonArray("versions") : null;
            if (versions == null) {
                return Optional.empty();
            }

            for (int i = 0; i < versions.size(); i++) {
                JsonObject entry = versions.get(i).getAsJsonObject();
                if (!version.equals(optString(entry, "version"))) {
                    continue;
                }
                String sha512 = optString(entry, "sha512");
                if (sha512 == null || sha512.isEmpty()) {
                    // No verifiable hash — fall back to a notify-only update (no download URL)
                    // so update()/download() don't promise a download they can't verify.
                    return Optional.of(new Update(version, null, null));
                }
                String downloadUrl = HubInfo.HUB_BASE + "/api/v1/updater/download/" + urlEncode(version)
                        + "?" + entitlementQuery + "&restamp=1";
                return Optional.of(new Update(version, downloadUrl, sha512));
            }
        } catch (Exception e) {
            MyPetApi.getLogger().warning("Failed to fetch Hub version manifest: " + e.getMessage());
        }
        return Optional.empty();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String optString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private static String httpGet(String url, String authorizationHeader) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10000))
                .build()) {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(10000))
                    .GET();
            if (authorizationHeader != null) {
                requestBuilder.header("Authorization", authorizationHeader);
            }
            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " from " + url);
            }
            return response.body();
        }
    }

    public void download() {
        if (latest == null || latest.getDownloadURL() == null) {
            MyPetApi.getLogger().warning("Download aborted: no verified download available for this copy.");
            return;
        }

        // Stage under the versioned name. Paper matches the update jar to the installed plugin
        // by plugin name (not filename) and renames the installed jar to this staged file's name,
        // so the loaded jar always ends up named for the version it actually contains.
        File pluginFile = new File(MyPetApi.getPlugin().getFile().getParentFile().getAbsolutePath(), "update/MyPet-" + latest.getVersion() + ".jar");
        if (!pluginFile.getParentFile().exists()) {
            pluginFile.getParentFile().mkdirs();
        }

        // Stream to a temp file alongside the final target and only promote it on success,
        // so a killed process (e.g. server shutdown) can never leave a half-written jar under
        // the final name — Bukkit's updater would otherwise blindly promote it next boot.
        File tmpFile = new File(pluginFile.getParentFile(), pluginFile.getName() + ".tmp");

        String finalUrl = latest.getDownloadURL();
        String manifestHash = latest.getSha512Hash();
        Runnable downloadRunner = () -> {
            MyPetApi.getLogger().info("Start update download: " + latest);

            String expectedHash = manifestHash;
            String actualHash;
            try {
                URL website = new URL(finalUrl);
                HttpURLConnection httpConn = (HttpURLConnection) website.openConnection();
                int responseCode = httpConn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    MyPetApi.getLogger().warning("Download aborted: HTTP " + responseCode + " from update server.");
                    httpConn.disconnect();
                    return;
                }

                // Prefer per-copy hash from header (personalized jars); fall back to manifest hash.
                String headerHash = httpConn.getHeaderField("X-Content-SHA512");
                if (headerHash != null && !headerHash.isBlank()) {
                    expectedHash = headerHash.trim();
                }
                if (expectedHash == null) {
                    MyPetApi.getLogger().warning("Download aborted: server did not provide a verification hash.");
                    httpConn.disconnect();
                    return;
                }

                Hasher hasher = Hashing.sha512().newHasher();
                try (InputStream inputStream = httpConn.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
                    int bytesRead;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        hasher.putBytes(buffer, 0, bytesRead);
                    }
                }
                actualHash = hasher.hash().toString();
            } catch (IOException e) {
                MyPetApi.getLogger().warning("Download failed: " + e.getMessage());
                tmpFile.delete(); // Clean up partial download
                return;
            }

            // Verify SHA512 hash
            if (!expectedHash.equalsIgnoreCase(actualHash)) {
                MyPetApi.getLogger().severe("Download verification failed! Hash mismatch.");
                MyPetApi.getLogger().severe("Expected: " + expectedHash);
                MyPetApi.getLogger().severe("Actual:   " + actualHash);
                MyPetApi.getLogger().severe("Deleting corrupted file...");
                if (!tmpFile.delete()) {
                    MyPetApi.getLogger().warning("Failed to delete corrupted file: " + tmpFile.getAbsolutePath());
                }
                return;
            }

            // Verified — atomically promote the staged file to its final name. A process
            // killed at any point up to here leaves only a stray ".tmp" that Bukkit ignores.
            try {
                try {
                    Files.move(tmpFile.toPath(), pluginFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile.toPath(), pluginFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                MyPetApi.getLogger().warning("Failed to finalize downloaded update: " + e.getMessage());
                tmpFile.delete();
                return;
            }

            MyPetApi.getLogger().info("Finished update download (verified). The update will be loaded on the next server start.");
        };
        if (!MyPetGlobal.Update.ASYNC.get()) {
            downloadRunner.run();
        } else {
            thread = new Thread(downloadRunner);
            thread.start();
        }
    }

    /**
     * Waits for shutdown for any pending background work: the async version check
     * (which may itself have kicked off a download, e.g. dev builds) and, for the
     * synchronous release path, the download thread. Both waits are bounded so a stuck
     * network call can't hang server shutdown indefinitely.
     */
    public void waitForDownload() {
        joinBounded(checkThread, "update check");
        if (MyPetGlobal.Update.ASYNC.get()) {
            joinBounded(thread, "update download");
        }
    }

    private void joinBounded(Thread target, String label) {
        if (target == null || !target.isAlive()) {
            return;
        }
        MyPetApi.getLogger().info("Wait for the " + label + " to finish...");
        try {
            target.join(30_000);
        } catch (InterruptedException e) {
            ErrorUtil.report(e);
            return;
        }
        if (target.isAlive()) {
            MyPetApi.getLogger().warning("Timed out waiting for the " + label + " to finish.");
        }
    }

    /**
     * Compares two version strings to determine if the first is newer.
     * Handles formats like "4.0.0", "4.0.0-alpha-03", "4.0.0-beta-01".
     * Ordering for the same base version: alpha &lt; beta &lt; release.
     */
    private static boolean isNewerVersion(String newVersion, String currentVersion) {
        String newBase = newVersion.split("-")[0];
        String currentBase = currentVersion.split("-")[0];

        int baseCompare = compareBaseVersions(newBase, currentBase);
        if (baseCompare != 0) {
            return baseCompare > 0;
        }

        // Base versions are equal — compare pre-release tier (release=2, beta=1, alpha=0)
        int newTier = preReleaseTier(newVersion);
        int currentTier = preReleaseTier(currentVersion);

        if (newTier != currentTier) {
            return newTier > currentTier;
        }

        // Same tier — compare build numbers
        int newBuild = extractBuildNumber(newVersion);
        int currentBuild = extractBuildNumber(currentVersion);

        return newBuild > currentBuild;
    }

    /**
     * Returns a numeric tier for version ordering: alpha=0, beta=1, release=2.
     */
    private static int preReleaseTier(String version) {
        if (version.contains("-alpha")) return 0;
        if (version.contains("-beta")) return 1;
        return 2;
    }

    /**
     * Compares two dotted base version strings (e.g. "4.0.0") numerically, segment by segment.
     * {@code CompatUtil.versionCompare} isn't used here because it delegates to
     * {@code Runtime.Version.parse}, which throws on trailing-zero versions like "4.0.0".
     * An optional leading "v"/"V" is stripped; missing or non-numeric segments count as 0.
     *
     * @return negative if {@code a} &lt; {@code b}, positive if {@code a} &gt; {@code b}, 0 if equal
     */
    private static int compareBaseVersions(String a, String b) {
        String[] segmentsA = stripLeadingV(a).split("\\.");
        String[] segmentsB = stripLeadingV(b).split("\\.");
        int length = Math.max(segmentsA.length, segmentsB.length);
        for (int i = 0; i < length; i++) {
            int partA = i < segmentsA.length ? parseSegment(segmentsA[i]) : 0;
            int partB = i < segmentsB.length ? parseSegment(segmentsB[i]) : 0;
            if (partA != partB) {
                return Integer.compare(partA, partB);
            }
        }
        return 0;
    }

    private static String stripLeadingV(String version) {
        if (!version.isEmpty() && (version.charAt(0) == 'v' || version.charAt(0) == 'V')) {
            return version.substring(1);
        }
        return version;
    }

    private static int parseSegment(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Extracts build number from version string like "4.0.0-alpha-03".
     * Returns 0 if no build number is found.
     */
    private static int extractBuildNumber(String version) {
        int lastDash = version.lastIndexOf('-');
        if (lastDash == -1) {
            return 0;
        }
        try {
            return Integer.parseInt(version.substring(lastDash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
