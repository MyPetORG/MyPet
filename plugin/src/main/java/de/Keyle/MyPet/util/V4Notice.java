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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tells admins when MyPet 4 is available for their server. The latest MyPet 4 release is read
 * from the public MyPet download site's release list, which needs no token. The notice is only
 * shown when the server could run MyPet 4: Paper (or a fork of it) on Minecraft 1.20.5 or newer,
 * with Java 21 or newer.
 */
public class V4Notice {

    public static final String URL = "https://builtbybit.com/mypet";

    private static final String RELEASES = "https://downloads.mypet-plugin.com/api/v1/versions?channel=release";
    private static final String MIN_MINECRAFT = "1.20.5";
    private static final int MIN_JAVA = 21;

    /** The sale dates are set on BuiltByBit in China Standard Time. */
    private static final ZoneId SALE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter SALE_END_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final Sale[] SALES = {
            new Sale("Black Friday", 40, LocalDateTime.of(2026, 11, 24, 21, 0), LocalDateTime.of(2026, 12, 1, 21, 0)),
            new Sale("Christmas", 33, LocalDateTime.of(2026, 12, 23, 21, 0), LocalDateTime.of(2027, 1, 1, 21, 0)),
    };

    /** During a sale its banner is shown again after this long, in the console and to each op. */
    private static final Duration SALE_REPEAT = Duration.ofHours(24);
    /** How often the sale task checks whether a banner is due (10 minutes, in ticks). */
    private static final long SALE_TASK_PERIOD = 20L * 60 * 10;

    private static final Map<UUID, Instant> saleShownToPlayer = new HashMap<>();
    private static Instant saleShownInConsole = null;
    private static Sale shownSale = null;

    // written by the startup check thread, read on the main thread (join message, /mypet update)
    private static volatile String latest = null;
    private static volatile boolean eligible = false;

    /**
     * Checks whether the server can run MyPet 4 and, if so, which MyPet 4 release is the newest.
     * Respects the existing "check for updates" setting. Blocks on the network, so MyPet calls it
     * from a background thread; until it finishes {@link #getLatest()} returns null.
     */
    public static void check() {
        latest = null;
        eligible = isEligible(MyPetApi.getCompatUtil().getMinecraftVersion(), isPaper(), javaVersion());
        if (!eligible || !Configuration.Update.CHECK) {
            return;
        }
        try {
            JsonObject root = new Gson().fromJson(Util.readUrlContent(RELEASES, 3000), JsonObject.class);
            JsonArray versions = root != null ? root.getAsJsonArray("versions") : null;
            if (versions != null && versions.size() > 0) {
                JsonObject newest = versions.get(0).getAsJsonObject();
                if (newest.has("version")) {
                    latest = newest.get("version").getAsString();
                }
            }
        } catch (Exception e) {
            MyPetApi.getLogger().fine("MyPet 4 release check failed: " + e.getMessage());
        }
    }

    /**
     * @return the notice to show, or null when there is nothing to tell
     */
    public static String getMessage() {
        String latest = V4Notice.latest;
        if (latest == null) {
            return null;
        }
        String message = "MyPet " + latest + " is available: " + URL;
        String sale = getSaleText();
        return sale != null ? message + " (" + sale + ")" : message;
    }

    /**
     * @return the running sale, for example "Black Friday sale: 40% off until Dec 1", or null
     */
    public static String getSaleText() {
        return getSaleText(Instant.now());
    }

    static String getSaleText(Instant now) {
        Sale sale = activeSale(now);
        return sale == null ? null : sale.name + " sale: " + sale.percent + "% off until " + sale.endText();
    }

    static Sale activeSale(Instant now) {
        for (Sale sale : SALES) {
            if (!now.isBefore(sale.start) && now.isBefore(sale.end)) {
                return sale;
            }
        }
        return null;
    }

    /**
     * Starts the task that shows the sale banner while a sale runs: in the console, and to every
     * online op, at most once every 24 hours each. Only servers that can run MyPet 4 see it. Bukkit cancels it when MyPet is disabled.
     */
    public static void startSaleTask() {
        Bukkit.getScheduler().runTaskTimer(MyPetApi.getPlugin(), V4Notice::announceSale, 20L, SALE_TASK_PERIOD);
    }

    private static void announceSale() {
        if (!Configuration.Update.CHECK || !eligible) {
            return;
        }
        Instant now = Instant.now();
        Sale sale = activeSale(now);
        if (sale != shownSale) {
            // A new sale (or the end of one) starts the 24 hour clock again for everyone.
            shownSale = sale;
            saleShownInConsole = null;
            saleShownToPlayer.clear();
        }
        if (sale == null) {
            return;
        }
        if (isDue(saleShownInConsole, now)) {
            saleShownInConsole = now;
            logSaleBanner(sale);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            showSaleIfDue(player, now);
        }
    }

    /**
     * Shows the sale banner to an op who has not seen it in the last 24 hours.
     *
     * @return true if the banner was shown
     */
    public static boolean showSaleIfDue(Player player) {
        if (!Configuration.Update.CHECK) {
            return false;
        }
        return showSaleIfDue(player, Instant.now());
    }

    private static boolean showSaleIfDue(Player player, Instant now) {
        Sale sale = activeSale(now);
        if (sale == null || !eligible || !Configuration.Update.SHOW_OP || !player.isOp()) {
            return false;
        }
        if (sale != shownSale) {
            shownSale = sale;
            saleShownInConsole = null;
            saleShownToPlayer.clear();
        }
        if (!isDue(saleShownToPlayer.get(player.getUniqueId()), now)) {
            return false;
        }
        saleShownToPlayer.put(player.getUniqueId(), now);
        sendSaleBanner(player, sale);
        return true;
    }

    private static boolean isDue(Instant lastShown, Instant now) {
        return lastShown == null || !now.isBefore(lastShown.plus(SALE_REPEAT));
    }

    private static void logSaleBanner(Sale sale) {
        String line = "==================================================";
        MyPetApi.getLogger().warning(line);
        MyPetApi.getLogger().warning("  MyPet 4 " + sale.name.toUpperCase(Locale.ENGLISH) + " SALE: " + sale.percent + "% OFF");
        MyPetApi.getLogger().warning("  Ends " + sale.endText() + ". Get it at " + URL);
        MyPetApi.getLogger().warning(line);
    }

    private static void sendSaleBanner(Player player, Sale sale) {
        String line = "━━━━━━━━━━━━━━━━━━━━━━━━━━";

        TextComponent top = new TextComponent(line);
        top.setColor(ChatColor.GOLD);

        TextComponent title = new TextComponent("  MyPet 4 " + sale.name + " Sale: " + sale.percent + "% OFF!");
        title.setColor(ChatColor.YELLOW);
        title.setBold(true);

        TextComponent body = new TextComponent("  Ends " + sale.endText() + ". ");
        body.setColor(ChatColor.WHITE);
        TextComponent link = new TextComponent("Click here to get it");
        link.setColor(ChatColor.GREEN);
        link.setBold(true);
        link.setUnderlined(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, URL));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent(URL)}));
        body.addExtra(link);

        TextComponent bottom = new TextComponent(line);
        bottom.setColor(ChatColor.GOLD);

        player.spigot().sendMessage(top);
        player.spigot().sendMessage(title);
        player.spigot().sendMessage(body);
        player.spigot().sendMessage(bottom);
    }

    public static String getLatest() {
        return latest;
    }

    public static boolean isServerEligible() {
        return eligible;
    }

    static boolean isEligible(String minecraftVersion, boolean paper, int java) {
        if (!paper || java < MIN_JAVA || minecraftVersion == null) {
            return false;
        }
        return Util.versionCompare(minecraftVersion, MIN_MINECRAFT) >= 0;
    }

    private static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.configuration.Configuration");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** "1.8" on Java 8, "21" on Java 21. */
    static int javaVersion() {
        String spec = System.getProperty("java.specification.version", "0");
        if (spec.startsWith("1.")) {
            spec = spec.substring(2);
        }
        int dot = spec.indexOf('.');
        if (dot != -1) {
            spec = spec.substring(0, dot);
        }
        try {
            return Integer.parseInt(spec);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static class Sale {
        final String name;
        final int percent;
        final Instant start;
        final Instant end;

        Sale(String name, int percent, LocalDateTime start, LocalDateTime end) {
            this.name = name;
            this.percent = percent;
            this.start = start.atZone(SALE_ZONE).toInstant();
            this.end = end.atZone(SALE_ZONE).toInstant();
        }

        String endText() {
            return SALE_END_FORMAT.format(end.atZone(SALE_ZONE));
        }
    }
}
