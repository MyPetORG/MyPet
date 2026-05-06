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

package de.Keyle.MyPet.util.player;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ContributorCheck {
    private static final Map<String, Character> contributorMap = new ConcurrentHashMap<>();
    private static volatile boolean contributorMapLoaded = false;
    private static final long REFRESH_INTERVAL_MINUTES = 10L;

    public static void startRefreshTask() {
        Bukkit.getServer().getAsyncScheduler().runAtFixedRate(
                MyPetApi.getPlugin(),
                t -> refreshContributorMap(),
                REFRESH_INTERVAL_MINUTES,
                REFRESH_INTERVAL_MINUTES,
                TimeUnit.MINUTES
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
        try {
            String content = Util.readUrlContent("https://raw.githubusercontent.com/MyPetORG/MyPet/particles/particles.csv");
            contributorMap.clear();
            for (String line : content.split("\n")) {
                if (line.length() >= 2) {
                    contributorMap.put(line.substring(0, line.length() - 1), line.charAt(line.length() - 1));
                }
            }
            contributorMapLoaded = true;
        } catch (Exception e) {
            MyPetApi.getLogger().warning("Failed to load contributor list: " + e.getMessage());
        }
    }

    public static ContributorRank getContributorRank(MyPetPlayer player) {
        try {
            String check;
            if (player.getUniqueId() != null) {
                check = player.getName() + "," + player.getUniqueId() + ",";
            } else {
                check = player.getName() + ",,";
            }

            if (!contributorMapLoaded) {
                fillContributorMap();
            }

            Character contributorType = '0';
            if (contributorMap.containsKey(check)) {
                contributorType = contributorMap.get(check);
            }
            return switch (contributorType) {
                case '1' -> ContributorRank.Donator;
                case '2' -> ContributorRank.Developer;
                case '3' -> ContributorRank.Translator;
                case '4' -> ContributorRank.Helper;
                case '5' -> ContributorRank.Creator;
                case '6' -> ContributorRank.Premium;
                default -> ContributorRank.None;
            };
        } catch (Exception ignored) {
            return ContributorRank.None;
        }
    }
}
