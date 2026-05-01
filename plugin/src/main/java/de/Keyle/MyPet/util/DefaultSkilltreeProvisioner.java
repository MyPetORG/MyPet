package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Seeds the {@code skilltrees/} folder of a fresh installation with the bundled default
 * {@code .st.json} files (Combat, Farm, PvP, Ride, Utility) extracted from the plugin JAR.
 *
 * <p>This is opt-in: the copy only runs when the caller signals that the {@code skilltrees/}
 * folder did not exist prior to the current {@code mkdirs()} call. Server operators who have
 * deleted a default skilltree on purpose will not see it reappear after a restart, because by
 * then the folder already exists.</p>
 *
 * <p>Existing files are never overwritten, even on the first-run path — the per-file
 * {@code exists()} guard prevents clobbering a file the operator may have created manually.</p>
 */
public final class DefaultSkilltreeProvisioner {

    private static final List<String> DEFAULT_SKILLTREES = List.of(
            "Combat.st.json",
            "Farm.st.json",
            "PvP.st.json",
            "Ride.st.json",
            "Utility.st.json"
    );

    private DefaultSkilltreeProvisioner() {
    }

    /**
     * Copies the bundled default skilltree JSON files into {@code skilltreeFolder} if and only if
     * the folder did not already exist (i.e. {@code createdFolder} is true). Existing files are
     * never overwritten. Logs a single info line on completion when files are copied.
     *
     * @param skilltreeFolder the destination folder (typically {@code <pluginDataFolder>/skilltrees})
     * @param createdFolder   {@code true} if the caller's most recent {@code skilltreeFolder.mkdirs()}
     *                        actually created the folder; {@code false} if it already existed
     * @param plugin          the plugin instance whose JAR contains the bundled resources
     */
    public static void copyDefaultsIfFolderCreated(@NotNull File skilltreeFolder,
                                                   boolean createdFolder,
                                                   @NotNull Plugin plugin) {
        if (!createdFolder) {
            return;
        }
        for (String fileName : DEFAULT_SKILLTREES) {
            File target = new File(skilltreeFolder, fileName);
            if (!target.exists()) {
                ResourceUtil.copyResource(plugin, "skilltrees/" + fileName, target);
            }
        }
        MyPetApi.getLogger().info("Default skilltree files created.");
    }
}
