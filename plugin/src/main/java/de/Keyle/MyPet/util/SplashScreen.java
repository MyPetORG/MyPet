package de.Keyle.MyPet.util;

import de.Keyle.MyPet.api.MyPetVersion;
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
        String version = MyPetVersion.getFormattedVersion();

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
