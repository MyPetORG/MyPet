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

package de.Keyle.MyPet.api.player;

import de.Keyle.MyPet.MyPetApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContributorCheck {
    private static final Map<String, Character> contributorMap = new ConcurrentHashMap<>();
    private static volatile boolean contributorMapLoaded = false;
    private static final long REFRESH_INTERVAL_TICKS = 10 * 60 * 20L; // 10 minutes in ticks

    public static void startRefreshTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(
                MyPetApi.getPlugin(),
                ContributorCheck::refreshContributorMap,
                REFRESH_INTERVAL_TICKS,
                REFRESH_INTERVAL_TICKS
        );
    }

    private static synchronized void refreshContributorMap() {
        contributorMapLoaded = false;
        fillContributorMap();
    }

    public enum ContributorRank {
        Creator("☣"),
        Donator("❤"),
        Translator("✈"),
        Developer("✪"),
        Helper("☘"),
        Premium("$"),
        None("");

        String defaultIcon;

        ContributorRank(String defaultIcon) {
            this.defaultIcon = defaultIcon;
        }

        public String getDefaultIcon() {
            return defaultIcon;
        }
    }

    private static synchronized void fillContributorMap() {
        if (contributorMapLoaded) {
            return;
        }
        int timeout = 2000;
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://raw.githubusercontent.com/MyPetORG/MyPet/particles/particles.csv");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            connection.connect();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                contributorMap.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() >= 2) {
                        contributorMap.put(line.substring(0, line.length() - 1), line.charAt(line.length() - 1));
                    }
                }
            }
            contributorMapLoaded = true;
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static ContributorRank getContributorRank(MyPetPlayer player) {
        try {
            // Check whether this player has contributed to MyPet
            // returns
            //   0 for nothing
            //   1 for donator
            //   2 for developer
            //   3 for translator
            //   4 for helper
            //   5 for creator
            //   6 for premium
            // no data will be saved on the server
            String check;
            if (player.getMojangUUID() != null) {
                check = player.getName()+","+player.getMojangUUID()+",";
            } else {
                check = player.getName()+",,";
            }

            if (!contributorMapLoaded) {
                fillContributorMap();
            }

            Character contributorType = '0';
            if (contributorMap.containsKey(check)) {
                contributorType = contributorMap.get(check);
            }
            switch (contributorType) {
                case '1':
                    return ContributorRank.Donator;
                case '2':
                    return ContributorRank.Developer;
                case '3':
                    return ContributorRank.Translator;
                case '4':
                    return ContributorRank.Helper;
                case '5':
                    return ContributorRank.Creator;
                case '6':
                    return ContributorRank.Premium;
                default:
                    return ContributorRank.None;
            }
        } catch (Exception ignored) {
            return ContributorRank.None;
        }
    }
}