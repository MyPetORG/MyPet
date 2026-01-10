/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

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

    protected static Update latest = null;
    protected String plugin;
    protected Thread thread;

    public Updater(String plugin) {
        this.plugin = plugin;
        latest = null;
    }

    public void update() {
        if (Configuration.Update.CHECK) {
            // Skip update check for local builds
            if ("local".equals(MyPetVersion.getBuild())) {
                MyPetApi.getLogger().info("Skipping update check for local build.");
                return;
            }

            Runnable updateRunner = () -> {
                Optional<Update> update = check();
                if (update.isPresent()) {
                    latest = update.get();

                    notifyVersion();

                    if (Configuration.Update.DOWNLOAD) {
                        download();
                    }
                } else {
                    MyPetApi.getLogger().info("No update available.");
                }
            };
            if (Configuration.Update.ASYNC) {
                new Thread(updateRunner).start();
            } else {
                updateRunner.run();
            }
        }
    }

    protected Optional<Update> check() {
        try {

            String currentPluginVersion = MyPetVersion.getVersion();

            // Use Modrinth API to check for updates
            String url = "https://api.modrinth.com/v2/project/mypet/version?loaders=%5B%22paper%22,%22spigot%22%5D";

            String content = Util.readUrlContent(url);
            JsonArray resultArr = new Gson().fromJson(content, JsonArray.class);
            Optional<Update> update = Optional.empty();

            String currentMcVersion = MyPetApi.getCompatUtil().getMinecraftVersion();
            boolean isDevBuild = MyPetVersion.isDevBuild();

            for (int i = 0; i < resultArr.size(); i++) {
                JsonObject version = (JsonObject) resultArr.get(i);

                // Check version type: "release" for stable, "alpha"/"beta" for snapshots
                String versionType = version.get("version_type").getAsString();
                boolean isAlpha = "alpha".equals(versionType) || "beta".equals(versionType);

                // Release builds should only see releases
                // Dev builds can see both snapshots AND releases (to upgrade to final release)
                if (!isDevBuild && isAlpha) {
                    continue;
                }

                // Check if this version supports the current Minecraft version
                JsonArray gameVersions = version.has("game_versions") ? version.getAsJsonArray("game_versions") : null;
                if (gameVersions == null || gameVersions.size() == 0) {
                    continue;
                }
                boolean supportsCurrentMc = false;
                for (int j = 0; j < gameVersions.size(); j++) {
                    String gameVersion = gameVersions.get(j).getAsString();
                    // Support both exact match (1.21.4) and wildcard (1.21.x)
                    if (gameVersion.equals(currentMcVersion) ||
                        (gameVersion.endsWith(".x") && currentMcVersion.startsWith(gameVersion.substring(0, gameVersion.length() - 2)))) {
                        supportsCurrentMc = true;
                        break;
                    }
                }

                if (!supportsCurrentMc) {
                    continue;
                }

                String versionNumber = version.get("version_number").getAsString();

                // Only consider this an update if the version is actually newer
                // Handle versions like "3.14.1" and "3.14.1-SNAPSHOT-b42"
                if (!isNewerVersion(versionNumber, currentPluginVersion)) {
                    continue;
                }

                // Find the primary file (or first file if none marked primary)
                JsonArray files = version.getAsJsonArray("files");
                JsonObject primaryFile = null;
                for (int j = 0; j < files.size(); j++) {
                    JsonObject file = files.get(j).getAsJsonObject();
                    if (file.has("primary") && file.get("primary").getAsBoolean()) {
                        primaryFile = file;
                        break;
                    }
                }
                if (primaryFile == null && files.size() > 0) {
                    primaryFile = files.get(0).getAsJsonObject();
                }

                if (primaryFile != null && primaryFile.has("url")) {
                    String downloadURL = primaryFile.get("url").getAsString();
                    JsonObject hashes = primaryFile.has("hashes") ? primaryFile.getAsJsonObject("hashes") : null;
                    String sha512Hash = (hashes != null && hashes.has("sha512")) ? hashes.get("sha512").getAsString() : null;
                    update = Optional.of(new Update(versionNumber, downloadURL, sha512Hash));
                    break;
                }
            }
            return update;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void notifyVersion() {
        String m = "#  A new ";
        m += MyPetVersion.isDevBuild() ? "build" : "version";
        m += " is available: " + latest + " #";

        int len = m.length();

        MyPetApi.getLogger().info(new String(new char[len]).replace('\0', '#'));

        MyPetApi.getLogger().info(m);

        String url = "#  https://modrinth.com/plugin/mypet/versions";
        int pad = len - url.length();
        String spaces = pad > 0 ? new String(new char[pad]).replace('\0', ' ') : "";

        MyPetApi.getLogger().info(url + spaces + "#");
        MyPetApi.getLogger().info(new String(new char[len]).replace('\0', '#'));
    }

    public void download() {
        File pluginFile;
        if (Configuration.Update.REPLACE_OLD) {
            pluginFile = new File(MyPetApi.getPlugin().getFile().getParentFile().getAbsolutePath(), "update/" + MyPetApi.getPlugin().getFile().getName());
        } else {
            pluginFile = new File(MyPetApi.getPlugin().getFile().getParentFile().getAbsolutePath(), "update/MyPet-" + latest.getVersion() + ".jar");
        }
        if (!pluginFile.getParentFile().exists()) {
            pluginFile.getParentFile().mkdirs();
        }

        String finalUrl = latest.getDownloadURL();
        String expectedHash = latest.getSha512Hash();
        Runnable downloadRunner = () -> {
            MyPetApi.getLogger().info(ChatColor.RED + "Start update download: " + ChatColor.RESET + latest);

            if (expectedHash == null) {
                MyPetApi.getLogger().severe("Download aborted: Hash verification unavailable (API did not provide SHA512).");
                return;
            }

            String actualHash;
            try {
                URL website = new URL(finalUrl);
                HttpURLConnection httpConn = (HttpURLConnection) website.openConnection();
                int responseCode = httpConn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    httpConn.disconnect();
                    return;
                }

                Hasher hasher = Hashing.sha512().newHasher();
                try (InputStream inputStream = httpConn.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(pluginFile)) {
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
                pluginFile.delete(); // Clean up partial download
                return;
            }

            // Verify SHA512 hash
            if (!expectedHash.equalsIgnoreCase(actualHash)) {
                MyPetApi.getLogger().severe("Download verification failed! Hash mismatch.");
                MyPetApi.getLogger().severe("Expected: " + expectedHash);
                MyPetApi.getLogger().severe("Actual:   " + actualHash);
                MyPetApi.getLogger().severe("Deleting corrupted file...");
                if (!pluginFile.delete()) {
                    MyPetApi.getLogger().warning("Failed to delete corrupted file: " + pluginFile.getAbsolutePath());
                }
                return;
            }

            String message = "Finished update download (verified).";
            if (Configuration.Update.REPLACE_OLD || MyPetApi.getPlugin().getFile().getName().equals("MyPet-" + latest.getVersion() + ".jar")) {
                message += " The update will be loaded on the next server start.";
            } else {
                message += " The file was stored in the \"update\" folder.";
            }
            MyPetApi.getLogger().info(message);
        };
        if (!Configuration.Update.ASYNC) {
            downloadRunner.run();
        } else {
            thread = new Thread(downloadRunner);
            thread.start();
        }
    }

    public void waitForDownload() {
        if (Configuration.Update.ASYNC && thread != null && thread.isAlive()) {
            MyPetApi.getLogger().info("Wait for the update download to finish...");
            try {
                thread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Update getLatest() {
        return latest;
    }

    public static boolean isUpdateAvailable() {
        return latest != null;
    }

    /**
     * Compares two version strings to determine if the first is newer.
     * Handles formats like "3.14.1", "3.14.1-SNAPSHOT", and "3.14.1-SNAPSHOT-b42".
     * A release (3.14.1) is always considered newer than snapshots of the same base (3.14.1-SNAPSHOT-b10).
     */
    private static boolean isNewerVersion(String newVersion, String currentVersion) {
        // Extract base version (before any dash)
        String newBase = newVersion.split("-")[0];
        String currentBase = currentVersion.split("-")[0];

        int baseCompare = Util.versionCompare(newBase, currentBase);
        if (baseCompare != 0) {
            return baseCompare > 0;
        }

        // Base versions are equal - check if release vs snapshot
        boolean newIsSnapshot = newVersion.contains("-SNAPSHOT");
        boolean currentIsSnapshot = currentVersion.contains("-SNAPSHOT");

        // A release is always newer than a snapshot of the same base version
        if (!newIsSnapshot && currentIsSnapshot) {
            return true;
        }
        // A snapshot is never newer than a release of the same base version
        if (newIsSnapshot && !currentIsSnapshot) {
            return false;
        }

        // Both are snapshots (or both releases), compare build numbers
        int newBuild = extractBuildNumber(newVersion);
        int currentBuild = extractBuildNumber(currentVersion);

        return newBuild > currentBuild;
    }

    /**
     * Extracts build number from version string like "3.14.1-SNAPSHOT-b42".
     * Returns 0 if no build number is found.
     */
    private static int extractBuildNumber(String version) {
        int bIndex = version.lastIndexOf("-b");
        if (bIndex == -1) {
            return 0;
        }
        try {
            return Integer.parseInt(version.substring(bIndex + 2));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}