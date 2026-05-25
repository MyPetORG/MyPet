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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Registers MyPet's bundled Brigadier commands with Paper's command registrar.
 *
 * <p>Each entry in {@link #COMMANDS} constructs a single command handler and calls
 * its {@code register(Commands, HelpRegistry)} method. Help metadata is collected into
 * the supplied {@link HelpRegistry} so {@code /mypet help} can later enumerate them.</p>
 *
 * <p>Invoked from inside the Paper
 * {@code LifecycleEvents.COMMANDS} event handler — that is the only valid time to populate
 * the Brigadier dispatcher in the modern Paper plugin lifecycle.</p>
 */
public final class BuiltInCommands {

    private static final List<BiConsumer<Commands, HelpRegistry>> COMMANDS = List.of(
            (c, h) -> new CommandAdmin().register(c, h),
            (c, h) -> new CommandCall().register(c, h),
            (c, h) -> new CommandStop().register(c, h),
            (c, h) -> new CommandPetRide().register(c, h),
            (c, h) -> new CommandSendAway().register(c, h),
            (c, h) -> new CommandPickup().register(c, h),
            (c, h) -> new CommandCaptureHelper().register(c, h),
            (c, h) -> new CommandName().register(c, h),
            (c, h) -> new CommandRelease().register(c, h),
            (c, h) -> new CommandRespawn().register(c, h),
            (c, h) -> new CommandBehavior().register(c, h),
            (c, h) -> new CommandSettings().register(c, h),
            (c, h) -> new CommandInfo().register(c, h),
            (c, h) -> new CommandSkill().register(c, h),
            (c, h) -> new CommandList().register(c, h),
            (c, h) -> new CommandTrade().register(c, h),
            (c, h) -> new CommandBeacon().register(c, h),
            (c, h) -> new CommandChooseSkilltree().register(c, h),
            (c, h) -> new CommandSwitch().register(c, h),
            (c, h) -> new CommandStore().register(c, h),
            (c, h) -> new CommandInventory().register(c, h),
            (c, h) -> new CommandShop().register(c, h),
            (c, h) -> new CommandMyPet().register(c, h)
    );

    private BuiltInCommands() {
    }

    /**
     * Registers every built-in command with the given Brigadier registrar and populates
     * {@code helpRegistry} with each command's help metadata. Logs a single confirmation
     * line on completion.
     *
     * @param commands     the Brigadier registrar exposed by Paper's COMMANDS lifecycle event
     * @param helpRegistry collector for help-entry metadata used by {@code /mypet help}
     */
    public static void register(@NotNull Commands commands, @NotNull HelpRegistry helpRegistry) {
        for (BiConsumer<Commands, HelpRegistry> registrar : COMMANDS) {
            registrar.accept(commands, helpRegistry);
        }
        MyPetApi.getLogger().info("Brigadier commands registered");
    }
}
