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
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.locale.Locale;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petskill} command (alias: {@code /pskill}).
 *
 * <p>Displays all active skills for the sender's pet, or another player's pet when a
 * target name is provided by an admin. Each skill is listed with its localized name
 * and a formatted description of its current level/properties.</p>
 *
 * <p><b>Usage:</b> {@code /petskill [player]}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.command.skill.other} -- required to view another player's pet skills (granted by the {@code MyPet.admin} bundle)</li>
 * </ul>
 */
public class CommandSkill {

    /**
     * Registers the {@code /petskill} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petskill")
                        .executes(ctx -> {
                            execute(ctx.getSource().getSender(), null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    execute(ctx.getSource().getSender(), StringArgumentType.getString(ctx, "player"));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Shows your pet's skills",
                List.of("pskill")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Skill",
                "/petskill",
                CommandCategory.SKILLS,
                150,
                player -> MyPetApi.getPetManager().hasActivePet(player)
        ));
    }

    /**
     * Executes the petskill command logic. Resolves the pet owner, auto-assigns the
     * skilltree if needed, then iterates over all active skills and sends their
     * localized descriptions to the sender.
     *
     * @param sender     the command sender (player or console)
     * @param targetName the name of the target player whose pet skills to view,
     *                   or {@code null} to view the sender's own pet skills
     */
    private void execute(CommandSender sender, String targetName) {
        Player petOwner;
        if (targetName == null && sender instanceof Player) {
            petOwner = (Player) sender;
        } else if (targetName != null && (!(sender instanceof Player) || Permissions.has((Player) sender, AdminPermissions.SKILL_OTHER))) {
            petOwner = Bukkit.getServer().getPlayer(targetName);

            if (petOwner == null || !petOwner.isOnline()) {
                sender.sendMessage(Locale.getComponent("Message.No.PlayerOnline", sender));
                return;
            } else if (!MyPetApi.getPetManager().hasActivePet(petOwner)) {
                sender.sendMessage(Locale.getFormattedComponent("Message.No.UserHavePet", sender, petOwner.getName()));
                return;
            }
        } else {
            if (sender instanceof Player) {
                sender.sendMessage(Locale.getComponent("Message.No.AllowedHere", sender));
            } else {
                sender.sendMessage("You can't use this command from server console!");
            }
            return;
        }

        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            sender.sendMessage(Locale.getComponent("Message.No.AllowedHere", sender));
        }

        if (MyPetApi.getPetManager().hasActivePet(petOwner)) {
            Pet pet = MyPetApi.getPetManager().getPet(petOwner);
            pet.autoAssignSkilltree();
            String skilltreeDisplay = pet.getSkilltree() == null ? "-" : pet.getSkilltree().getDisplayName();
            sender.sendMessage(Locale.getFormattedComponent("Message.Command.Skills.Show", sender, pet.getDisplayName(), Util.SANITIZED_MINIMESSAGE.deserialize(skilltreeDisplay)));

            String locale = Locale.getCommandSenderLanguage(sender);
            for (Skill skill : pet.getSkills().all()) {
                if (skill.isActive()) {
                    sender.sendMessage(Component.text()
                            .append(Component.text("  "))
                            .append(skill.getName(locale).color(NamedTextColor.GREEN))
                            .append(Component.space())
                            .append(skill.toPrettyComponent(locale))
                            .asComponent());
                }
            }
        } else {
            sender.sendMessage(Locale.getComponent("Message.No.HasPet", sender));
        }
    }
}
