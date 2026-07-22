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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.gui.menus.ToolboxMenuHandler;
import de.Keyle.MyPet.skill.skills.ToolboxImpl;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /pettoolbox} command, which opens the Toolbox skill's workstations
 * for the player's active pet — directly when one station is unlocked, via a chooser
 * menu when several are.
 *
 * <p><b>Usage:</b> {@code /pettoolbox}</p>
 * <p><b>Aliases:</b> {@code /ptoolbox}</p>
 * <p><b>Permissions:</b> {@code MyPet.extended.toolbox} — required to open the toolbox</p>
 *
 * @see ToolboxImpl
 */
public class CommandToolbox {

    /** Registers the {@code /pettoolbox} Brigadier command and its help entry. */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("pettoolbox")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Opens your pet's toolbox workstations",
                List.of("ptoolbox")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Toolbox",
                "/pettoolbox",
                CommandCategory.SKILLS,
                210,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && MyPetApi.getPetManager().getPet(player).getSkills().isActive(ToolboxImpl.class)
        ));
    }

    /**
     * Validates world group, pet presence/state, permission, and skill activity,
     * then delegates to {@link ToolboxMenuHandler#open}.
     */
    private void execute(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (MyPetApi.getPetManager().hasActivePet(player)) {
            Pet pet = MyPetApi.getPetManager().getPet(player);
            if (!Permissions.hasExtended(player, "MyPet.extended.toolbox")) {
                pet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", player));
                return;
            }
            if (pet.getStatus() == PetState.Despawned) {
                player.sendMessage(Locale.getFormattedComponent("Message.Call.First", player, pet.getDisplayName()));
                return;
            }
            if (pet.getStatus() == PetState.Dead) {
                player.sendMessage(Locale.getFormattedComponent("Message.Action.Dead", player, pet.getDisplayName()));
                return;
            }
            if (pet.getSkills().isActive(ToolboxImpl.class)) {
                ToolboxMenuHandler.open(player, pet);
            } else {
                player.sendMessage(Locale.getFormattedComponent("Message.No.Skill", player, pet.getDisplayName(), Locale.getComponent("Name.Skill.Toolbox", player)));
            }
        } else {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
        }
    }
}
