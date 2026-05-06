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

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Year;

/**
 * Renders the MyPet ASCII-art startup banner to the server console.
 *
 * <p>The banner shows the formatted version, the current copyright year range,
 * the configured database backend, and an optional one-line update notice.
 * Output is colorised via MiniMessage and sent to {@link Bukkit#getConsoleSender()}.</p>
 */
public final class SplashScreen {

    private SplashScreen() {
    }

    /**
     * Prints the splash screen to the server console.
     *
     * @param updateStatus a one-line update notice to embed in the banner (e.g. "An update is
     *                     available."), or {@code null} to omit the line entirely
     * @param dbType       the configured repository backend name to display (e.g. {@code "SQLite"},
     *                     {@code "MySQL"}); displayed verbatim in the "Connecting to ..." line
     */
    public static void print(@Nullable String updateStatus, @NotNull String dbType) {
        String version = VersionUtil.getFormattedVersion();

        String splash = String.join("\n",
                "",
                "<green>          ▄▄       </green>",
                "<green>    ▄██▄ ████      </green><green>  MyPet </green>" + version,
                "<green>    ████ ▀██▀      </green><green>  Created by Keyle | Maintained by UserDerezzed</green>",
                "<green>  ▄▄ ▀▀      ▄██▄  </green><green>  2011-" + Year.now() + "</green>",
                "<green> ████  ▄███▄ ▀██▀  </green>",
                "<green>  ▀▀ ▄███████▄     </green>" + (updateStatus != null ? "  " + updateStatus : ""),
                "<green>   ▄███████████▄   </green>",
                "<green>   ▀███▀▀▀▀▀███▀   </green>  Connecting to " + dbType + "...",
                "",
                "<green>Please consider supporting active development: https://ko-fi.com/userderezzed</green>"
        );

        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(splash));
    }
}
