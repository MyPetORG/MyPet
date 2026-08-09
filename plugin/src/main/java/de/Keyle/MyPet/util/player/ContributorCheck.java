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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.util.HubInfo;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ContributorCheck {
    private static volatile Map<UUID, ContributorRank> uuidRanks = Map.of();
    /** Fallback for legacy entries the Hub has no UUID for. */
    private static volatile Map<String, ContributorRank> nameRanks = Map.of();
    private static volatile boolean contributorMapLoaded = false;
    private static volatile long lastFailedAttempt = 0L;
    private static final long REFRESH_INTERVAL_HOURS = 12L;
    private static final long RETRY_BACKOFF_MS = TimeUnit.MINUTES.toMillis(10);

    public static void startRefreshTask() {
        Bukkit.getServer().getAsyncScheduler().runAtFixedRate(
                MyPetApi.getPlugin(),
                t -> refreshContributorMap(),
                REFRESH_INTERVAL_HOURS,
                REFRESH_INTERVAL_HOURS,
                TimeUnit.HOURS
        );
    }

    private static synchronized void refreshContributorMap() {
        contributorMapLoaded = false;
        lastFailedAttempt = 0L; // scheduled refresh always attempts, regardless of backoff
        fillContributorMaps();
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

    private static synchronized void fillContributorMaps() {
        if (contributorMapLoaded) {
            return;
        }
        if (System.currentTimeMillis() - lastFailedAttempt < RETRY_BACKOFF_MS) {
            return; // back off after a failed fetch so offline servers don't retry per player check
        }
        try {
            String content = Util.readUrlContent(HubInfo.HUB_BASE + "/api/v1/contributors");
            JsonObject root = new Gson().fromJson(content, JsonObject.class);
            JsonArray contributors = root.getAsJsonArray("contributors");
            Map<UUID, ContributorRank> byUuid = new HashMap<>();
            Map<String, ContributorRank> byName = new HashMap<>();
            for (JsonElement element : contributors) {
                JsonObject entry = element.getAsJsonObject();
                ContributorRank rank = parseRank(entry.get("rank"));
                if (rank == ContributorRank.None) {
                    continue;
                }
                UUID uuid = parseUuid(entry.get("uuid"));
                JsonElement name = entry.get("name");
                if (uuid != null) {
                    byUuid.put(uuid, rank);
                } else if (name != null && !name.isJsonNull()) {
                    byName.put(name.getAsString(), rank);
                }
            }
            uuidRanks = Map.copyOf(byUuid);
            nameRanks = Map.copyOf(byName);
            contributorMapLoaded = true;
        } catch (Exception e) {
            lastFailedAttempt = System.currentTimeMillis();
            MyPetApi.getLogger().warning("Failed to load contributor list: " + e.getMessage());
        }
    }

    private static ContributorRank parseRank(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return ContributorRank.None;
        }
        try {
            return ContributorRank.valueOf(element.getAsString());
        } catch (IllegalArgumentException e) {
            return ContributorRank.None; // unknown rank from a newer Hub
        }
    }

    private static UUID parseUuid(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return UUID.fromString(element.getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ContributorRank getContributorRank(MyPetPlayer player) {
        try {
            if (!contributorMapLoaded) {
                fillContributorMaps();
            }
            if (player.getUniqueId() != null) {
                ContributorRank rank = uuidRanks.get(player.getUniqueId());
                if (rank != null) {
                    return rank;
                }
            }
            ContributorRank byName = nameRanks.get(player.getName());
            return byName != null ? byName : ContributorRank.None;
        } catch (Exception ignored) {
            return ContributorRank.None;
        }
    }
}
