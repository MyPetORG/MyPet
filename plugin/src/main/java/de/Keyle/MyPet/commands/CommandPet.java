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

import com.mojang.brigadier.Command;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.gui.context.PetMenuContext;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /pet} command — opens the pet management menu for the player's
 * active pet, or delegates to {@link CommandSwitch#openSwitchMenu(Player)} when the
 * player has no active pet.
 */
/*
 * Multi-Pet Phase 2 (MyPetORG/MyPet#1435): this command resolves the player to a
 * single Pet via the manager. That has no unambiguous answer once a player can
 * have several out -- it needs the optional pet-name argument the issue calls for,
 * so it is deliberately left alone until that argument exists.
 */
public class CommandPet {

    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("pet")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            executeOpen((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Opens the pet management menu",
                List.of()
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Pet",
                "/pet",
                null,
                10,
                player -> true
        ));
    }

    private void executeOpen(Player viewer) {
        ActivePetChooser.withActivePet(viewer,
                pet -> openPetMenu(viewer, pet),
                () -> CommandSwitch.openSwitchMenu(viewer));
    }

    @SuppressWarnings("unchecked")
    private void openPetMenu(Player viewer, Pet pet) {
        MyPetApi.getGuiService().openMenu(
                viewer,
                (MenuId<PetMenuContext>) (MenuId<?>) MenuIds.PET_MENU,
                new PetMenuContext(viewer, pet)
        );
    }
}
