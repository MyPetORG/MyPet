/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.ErrorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

/**
 * Manager for Cloud Command Framework v2 integration.
 * <p>
 * Handles initialization and registration of commands using Cloud's
 * modern command API. Provides features like:
 * - Type-safe command definitions
 * - Automatic tab completion
 * - Flexible permission handling
 * - Exception handling
 * - Minecraft chat formatting support
 */
public class CloudCommandManager {

    private LegacyPaperCommandManager<CommandSender> commandManager;
    private AnnotationParser<CommandSender> annotationParser;

    public CloudCommandManager(Plugin plugin) {
    }

    /**
     * Initializes the Cloud command manager.
     * <p>
     * Creates a PaperCommandManager with:
     * - Simple SenderMapper (CommandSender → CommandSender)
     * - Synchronous execution coordinator (required for Bukkit events)
     * - Brigadier support for native Minecraft integration
     */
    public void initialize() {
        try {
            this.commandManager = LegacyPaperCommandManager.createNative(MyPetApi.getPlugin(), ExecutionCoordinator.simpleCoordinator());
            // Register Brigadier support for native Minecraft command integration
            if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                commandManager.registerBrigadier();
            } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }

            // Create annotation parser for annotation-based commands
            this.annotationParser = new AnnotationParser<>(commandManager, CommandSender.class);

            MyPetApi.getLogger().info("Cloud Command Framework v2 initialized");

        } catch (Exception e) {
            ErrorUtil.report("Failed to initialize Cloud Command Framework:", e);
        }
    }

    /**
     * Gets the underlying Cloud command manager.
     * <p>
     * Use this to register commands directly with Cloud's builder API.
     *
     * @return the Paper command manager instance
     */
    public @NonNull LegacyPaperCommandManager<CommandSender> getCommandManager() {
        if (commandManager == null) {
            throw new IllegalStateException("CloudCommandManager not initialized! Call initialize() first.");
        }
        return commandManager;
    }

    /**
     * Registers a command handler using the builder API.
     * <p>
     * Command handlers should build their command structure using the
     * command manager's builder API.
     *
     * @param handler the command handler to register
     */
    public void registerCommand(CloudCommand handler) {
        handler.register(this);
    }

    /**
     * Registers annotation-based commands from an object.
     * <p>
     * The object should contain methods annotated with @Command.
     * This is the preferred method for defining commands as it's
     * more concise and readable.
     *
     * @param commandObject the object containing @Command annotated methods
     */
    public void registerAnnotationCommands(Object commandObject) {
        if (annotationParser == null) {
            throw new IllegalStateException("AnnotationParser not initialized! Call initialize() first.");
        }
        annotationParser.parse(commandObject);
    }
}
