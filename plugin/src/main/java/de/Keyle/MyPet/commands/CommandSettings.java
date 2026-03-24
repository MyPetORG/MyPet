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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.commands.settings.CommandSettingHealthbar;
import de.Keyle.MyPet.commands.settings.CommandSettingsPetLivingSound;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

/**
 * Handles the {@code /petsettings} command, which provides player-configurable pet options.
 * This is a parent command that delegates to subcommands:
 *
 * <ul>
 *   <li>{@code /petsettings healthbar} — toggles the pet health bar display
 *       (see {@link CommandSettingHealthbar})</li>
 *   <li>{@code /petsettings idle-volume <amount>} — sets the pet's idle/living sound volume
 *       (0-100, see {@link CommandSettingsPetLivingSound})</li>
 * </ul>
 *
 * <p><b>Usage:</b> {@code /petsettings <healthbar|idle-volume>}</p>
 * <p><b>Aliases:</b> {@code /po}, {@code /peto}, {@code /petoption}, {@code /petoptions},
 * {@code /psettings}</p>
 * <p><b>Help category:</b> {@link CommandCategory#PET PET} (priority 40)</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandSettings {

    /**
     * Registers the {@code /petsettings} Brigadier command and its help entry.
     *
     * <p>The command tree is composed of subcommand nodes built by
     * {@link CommandSettingHealthbar#buildNode()} and
     * {@link CommandSettingsPetLivingSound#buildNode()}. No root-level executor is defined;
     * one of the subcommands must be specified.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petsettings")
                        .then(new CommandSettingHealthbar().buildNode())
                        .then(new CommandSettingsPetLivingSound().buildNode())
                        .build(),
                "Pet settings",
                List.of("po", "peto", "petoption", "petoptions", "psettings")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Options",
                "/petoptions",
                CommandCategory.PET,
                40,
                null
        ));
    }
}
