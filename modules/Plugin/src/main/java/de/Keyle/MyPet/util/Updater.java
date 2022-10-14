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
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

public class Updater {

    public class Update {

        String version;
        int build;
        String downloadURL;

        public Update(String version, int build, String downloadURL) {
            this.version = version;
            this.build = build;
            this.downloadURL = downloadURL;
        }

        public String getVersion() {
            return version;
        }

        public int getBuild() {
            return build;
        }

        public String getDownloadURL() { return downloadURL; }

        @Override
        public String toString() {
            return version + " #" + build;
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
            Runnable updateRunner = () -> {
                Optional<Update> update = check();
                if (update.isPresent()) {
                    latest = update.get();

                    notifyVersion();

                    if (Configuration.Update.DOWNLOAD) {
                        download();
                    }
                } else {
                    MyPetApi.getLogger().info("No Update available.");
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
            String url = "https://api.github.com/repos/MyPetORG/MyPet/releases";

            // no data will be saved on the server
            String content = Util.readUrlContent(url);
            JsonArray resultArr = new Gson().fromJson(content, JsonArray.class);

            for (int i = 0; i<resultArr.size(); i++) {
                JsonObject release = (JsonObject) resultArr.get(i);
                if (release.has("prerelease") &&
                        release.get("prerelease").getAsBoolean() == MyPetVersion.isDevBuild()) {
                    String rawVersion = release.get("name").getAsString();

                    String[] split = rawVersion.split("-");

                    int build = Integer.parseInt(split[split.length-1].substring(1));
                    if(MyPetVersion.getBuild().isEmpty() || build <= Integer.parseInt(MyPetVersion.getBuild())) { //Only do the rest if the build is even newer
                        return Optional.empty();
                    }

                    if(!release.get("body").getAsString().contains(MyPetApi.getCompatUtil().getInternalVersion()))
                        return Optional.empty();

                    String version = "";
                    for(int j = 0; j<split.length-1;j++) {
                        version+=split[j];
                        if(j<split.length-2) {
                            version+="-";
                        }
                    }

                    String downloadURL = release.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
                    Bukkit.getConsoleSender().sendMessage(downloadURL);
                    return Optional.of(new Update(version, build, downloadURL));
                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return Optional.empty();
    }

    private void notifyVersion() {
        String m = "#  A new ";
        m += MyPetVersion.isDevBuild() ? "build" : "version";
        m += " is available: " + latest + " #";
        MyPetApi.getLogger().info(StringUtils.repeat("#", m.length()));
        MyPetApi.getLogger().info(m);
        MyPetApi.getLogger().info("#  https://github.com/MyPetORG/MyPet/releases" + StringUtils.repeat(" ", m.length() - 46) + "#");
        MyPetApi.getLogger().info(StringUtils.repeat("#", m.length()));
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
        Runnable downloadRunner = () -> {
            MyPetApi.getLogger().info(ChatColor.RED + "Start update download: " + ChatColor.RESET + latest);

            try {
                URL website = new URL(finalUrl);
                HttpURLConnection httpConn = (HttpURLConnection) website.openConnection();
                int responseCode = httpConn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    httpConn.disconnect();
                    return;
                }
                InputStream inputStream = httpConn.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(pluginFile);
                Hasher hasher = Hashing.sha256().newHasher();

                int bytesRead;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    hasher.putBytes(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                return;
            }

            String message = "Finished update download.";
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
}