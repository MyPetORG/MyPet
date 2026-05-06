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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.PetInfoBuilder;
import de.Keyle.MyPet.util.player.ContributorCheck;
import de.Keyle.MyPet.util.player.MyPetPlayerImpl;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petinfo} command (alias: {@code /pinfo}).
 *
 * <p>Displays detailed information about the sender's active pet, or another player's pet
 * when a target player name is provided. The information shown includes pet name, HP,
 * damage, ranged damage, hunger, food, behavior, skilltree, level, experience, and
 * respawn time (if dead). Each info line has configurable visibility controlled by
 * {@link PetInfoDisplay} which determines whether a field is admin-only.</p>
 *
 * <p><b>Usage:</b> {@code /petinfo [player]}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.command.info.other} -- required to view another player's pet info</li>
 *   <li>{@code MyPet.admin} -- bypasses admin-only display restrictions</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandInfo {

    /**
     * Determines whether the given sender is allowed to see a particular info field.
     *
     * <p>If {@code adminOnly} is {@code true}, the field is only visible to the pet owner
     * themselves or to players with the {@code MyPet.admin} permission. Console senders
     * can always see all fields.</p>
     *
     * @param adminOnly   whether the field requires admin or owner status to view
     * @param sender      the command sender requesting the information
     * @param storedPet the pet whose info is being displayed
     * @return {@code true} if the sender is allowed to see the field
     */
    public static boolean canSee(boolean adminOnly, CommandSender sender, StoredPet storedPet) {
        if (sender instanceof Player player) {
            return !adminOnly || storedPet.getOwner().getPlayer() == player || Permissions.has(player, "MyPet.admin");
        } else {
            return true;
        }
    }

    /**
     * Registers the {@code /petinfo} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petinfo")
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
                "Shows info about your or another player's pet",
                List.of("pinfo")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Info",
                "/petinfo",
                null,
                20,
                null
        ));
    }

    /**
     * Executes the petinfo command logic, resolving the pet owner and displaying
     * all applicable info lines for their active pet.
     *
     * @param sender     the command sender (player or console)
     * @param targetName the name of the target player whose pet info to view,
     *                   or {@code null} to view the sender's own pet
     */
    private void execute(CommandSender sender, String targetName) {
        MyPetPlayer petOwner;

        if (targetName == null && sender instanceof Player player) {
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
                return;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            } else {
                sender.sendMessage(Locale.getComponent("Message.No.HasPet", player));
                return;
            }
        } else if (targetName != null && (!(sender instanceof Player) || Permissions.has((Player) sender, "MyPet.command.info.other"))) {
            Player p = Bukkit.getServer().getPlayer(targetName);
            if (p == null || !p.isOnline()) {
                sender.sendMessage(Locale.getComponent("Message.No.PlayerOnline", sender));
                return;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(targetName)) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(targetName);
            } else {
                sender.sendMessage(Locale.getFormattedComponent("Message.No.UserHavePet", sender, targetName));
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

        if (petOwner.hasMyPet()) {
            boolean infoShown = false;
            MyPet myPet = petOwner.getMyPet();

            // Pet name header
            if (canSee(PetInfoDisplay.Name.adminOnly, sender, myPet)) {
                sender.sendMessage(PetInfoBuilder.petNameHeader(myPet));
                infoShown = true;
            }

            // Owner line (only show if viewing someone else's pet)
            if (!petOwner.equals(sender) && canSee(!PetInfoDisplay.Owner.adminOnly, sender, myPet)) {
                sender.sendMessage(PetInfoBuilder.ownerLine(myPet, sender));
                infoShown = true;
            }

            // HP line
            if (canSee(PetInfoDisplay.HP.adminOnly, sender, myPet)) {
                sender.sendMessage(PetInfoBuilder.hpLine(myPet, sender));
                infoShown = true;
            }

            // Respawn time (if dead)
            if (canSee(PetInfoDisplay.RespawnTime.adminOnly, sender, myPet)) {
                Component respawnTime = PetInfoBuilder.respawnTimeLine(myPet, sender);
                if (respawnTime != null) {
                    sender.sendMessage(respawnTime);
                    infoShown = true;
                }
            }

            // Damage line
            if (canSee(PetInfoDisplay.Damage.adminOnly, sender, myPet)) {
                Component damage = PetInfoBuilder.damageLine(myPet, sender);
                if (damage != null) {
                    sender.sendMessage(damage);
                    infoShown = true;
                }
            }

            // Ranged damage line
            if (canSee(PetInfoDisplay.RangedDamage.adminOnly, sender, myPet)) {
                Component rangedDamage = PetInfoBuilder.rangedDamageLine(myPet, sender);
                if (rangedDamage != null) {
                    sender.sendMessage(rangedDamage);
                    infoShown = true;
                }
            }

            // Hunger system
            if (canSee(PetInfoDisplay.Hunger.adminOnly, sender, myPet)) {
                Component hunger = PetInfoBuilder.hungerLine(myPet, sender);
                if (hunger != null) {
                    sender.sendMessage(hunger);
                    infoShown = true;
                }

                Component food = PetInfoBuilder.foodLine(myPet, sender);
                if (food != null) {
                    sender.sendMessage(food);
                    infoShown = true;
                }
            }

            // Behavior line
            if (canSee(PetInfoDisplay.Behavior.adminOnly, sender, myPet)) {
                Component behavior = PetInfoBuilder.behaviorLine(myPet, sender);
                if (behavior != null) {
                    sender.sendMessage(behavior);
                    infoShown = true;
                }
            }

            // Skilltree line
            if (canSee(PetInfoDisplay.Skilltree.adminOnly, sender, myPet)) {
                Component skilltree = PetInfoBuilder.skilltreeLine(myPet, sender);
                if (skilltree != null) {
                    sender.sendMessage(skilltree);
                    infoShown = true;
                }
            }

            // Level line
            if (canSee(PetInfoDisplay.Level.adminOnly, sender, myPet)) {
                sender.sendMessage(PetInfoBuilder.levelLine(myPet, sender));
                infoShown = true;
            }

            // Experience line
            if (canSee(PetInfoDisplay.Exp.adminOnly, sender, myPet)) {
                Component exp = PetInfoBuilder.expLine(myPet, sender);
                if (exp != null) {
                    sender.sendMessage(exp);
                    infoShown = true;
                }
            }
            ContributorCheck.ContributorRank rank = ((MyPetPlayerImpl) myPet.getOwner()).getContributorRank();
            if (rank != ContributorCheck.ContributorRank.None) {
                infoShown = true;
                String icon = rank.getDefaultIcon();
                sender.sendMessage(Component.text()
                        .append(Component.text("   " + icon + " ").color(NamedTextColor.GOLD))
                        .append(Locale.getComponent("Name.Title." + rank.name(), sender).color(NamedTextColor.GOLD))
                        .append(Component.text(" " + icon).color(NamedTextColor.GOLD))
                        .build());
            }

            if (!infoShown) {
                sender.sendMessage(Locale.getComponent("Message.CantViewPetInfo", sender));
            }
        } else {
            if (targetName != null) {
                sender.sendMessage(Locale.getFormattedComponent("Message.No.UserHavePet", sender, targetName));
            } else {
                sender.sendMessage(Locale.getComponent("Message.No.HasPet", sender));
            }
        }
    }

    /**
     * Enum controlling the visibility of each pet info field.
     * When {@code adminOnly} is {@code true}, the field is only visible to the pet owner
     * or players with {@code MyPet.admin} permission.
     */
    public enum PetInfoDisplay {
        Name(false), HP(false), Damage(false), Hunger(true), Exp(true), Level(true), Owner(false), Skilltree(true), RangedDamage(false), RespawnTime(true), Behavior(true);

        public boolean adminOnly;

        PetInfoDisplay(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }
    }
}
