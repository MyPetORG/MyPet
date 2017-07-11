/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

public class Updater {
    public class Update {
        String version;
        int build;

        public Update(String version, int build) {
            this.version = version;
            this.build = build;
        }

        public String getVersion() {
            return version;
        }

        public int getBuild() {
            return build;
        }

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
            Optional<Update> update = check();
            if (update.isPresent()) {
                latest = update.get();

                notifyVersion();

                if (Configuration.Update.DOWNLOAD) {
                    download();
                }
            }
        }
    }

    protected Optional<Update> check() {
        try {
            String parameter = "";
            parameter += "&package=" + MyPetApi.getCompatUtil().getInternalVersion();
            parameter += "&build=" + 1;//MyPetVersion.getBuild();
            parameter += "&dev=" + MyPetVersion.isDevBuild();

            String url = "http://update.mypet-plugin.de/" + plugin + "?" + parameter;

            // no data will be saved on the server
            String content = Util.readUrlContent(url);
            JSONParser parser = new JSONParser();
            JSONObject result = (JSONObject) parser.parse(content);

            if (result.containsKey("latest")) {
                String version = result.get("latest").toString();
                int build = ((Long) result.get("build")).intValue();
                return Optional.of(new Update(version, build));
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
        MyPetApi.getLogger().info("#  https://mypet-plugin.de/download" + StringUtils.repeat(" ", m.length() - 35) + "#");
        MyPetApi.getLogger().info(StringUtils.repeat("#", m.length()));
    }

    public void download() {
        String url = "https://mypet-plugin.de/download/" + plugin + "/";
        if (MyPetVersion.isDevBuild()) {
            url += "dev";
        } else {
            url += "release";
        }
        url += "?api_token=" + Configuration.Update.TOKEN;
        File pluginFile;
        if (Configuration.Update.REPLACE_OLD) {
            pluginFile = new File(MyPetApi.getPlugin().getFile().getParentFile().getAbsolutePath(), "update/" + MyPetApi.getPlugin().getFile().getName());
        } else {
            pluginFile = new File(MyPetApi.getPlugin().getFile().getParentFile().getAbsolutePath(), "update/MyPet-" + latest.getVersion() + ".jar");
        }

        String finalUrl = url;
        thread = new Thread(() -> {
            try {
                MyPetApi.getLogger().info(ChatColor.RED + "Start update download: " + ChatColor.RESET + latest);
                URL website = new URL(finalUrl);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(pluginFile);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
                rbc.close();
                String message = "Finished update download.";
                if (Configuration.Update.REPLACE_OLD || MyPetApi.getPlugin().getFile().getName().equals("MyPet-" + latest.getVersion() + ".jar")) {
                    message += " The update will be loaded on the next server start.";
                } else {
                    message += " The file was stored in the \"update\" folder.";
                }
                MyPetApi.getLogger().info(message);
            } catch (IOException e) {
                if (e.getMessage().contains("403")) {
                    MyPetApi.getLogger().warning(ChatColor.RED + "You are not allowed to download MyPet-Premium. Please check/set your download token in the config.");
                } else {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void waitForDownload() {
        if (thread != null && thread.isAlive()) {
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