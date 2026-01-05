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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.ContributorCheck;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.PetInfoBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandInfo implements CommandTabCompleter {

    public static boolean canSee(boolean adminOnly, CommandSender sender, StoredMyPet storedMyPet) {
        if (sender instanceof Player player) {
            return !adminOnly || storedMyPet.getOwner().getPlayer() == player || Permissions.has(player, "MyPet.admin", false);
        } else {
            return true;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MyPetPlayer petOwner;

        if (args.length == 0 && sender instanceof Player player) {
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                player.sendMessage(Translation.getComponent("Message.No.AllowedHere", player));
                return true;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            } else {
                sender.sendMessage(Translation.getComponent("Message.No.HasPet", player));
                return true;
            }
        } else if (args.length > 0 && (!(sender instanceof Player) || Permissions.has((Player) sender, "MyPet.command.info.other"))) {
            Player p = Bukkit.getServer().getPlayer(args[0]);
            if (p == null || !p.isOnline()) {
                sender.sendMessage(Translation.getComponent("Message.No.PlayerOnline", sender));
                return true;
            }
            if (MyPetApi.getPlayerManager().isMyPetPlayer(args[0])) {
                petOwner = MyPetApi.getPlayerManager().getMyPetPlayer(args[0]);
            } else {
                sender.sendMessage(Util.formatTranslation("Message.No.UserHavePet", sender, args[0]));
                return true;
            }
        } else {
            if (sender instanceof Player) {
                sender.sendMessage(Translation.getComponent("Message.No.AllowedHere", sender));
            } else {
                sender.sendMessage("You can't use this command from server console!");
            }
            return true;
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
            if (myPet.getOwner().getContributorRank() != ContributorCheck.ContributorRank.None) {
                infoShown = true;
                String contributionMessage = "" + ChatColor.GOLD;
                contributionMessage += myPet.getOwner().getContributorRank().getDefaultIcon();
                contributionMessage += " " + Translation.getString("Name.Title." + myPet.getOwner().getContributorRank().name(), sender) + " ";
                contributionMessage += myPet.getOwner().getContributorRank().getDefaultIcon();
                sender.sendMessage("   " + contributionMessage);
            }

            if (!infoShown) {
                sender.sendMessage(Translation.getComponent("Message.CantViewPetInfo", sender));
            }
            return true;
        } else {
            if (args.length > 0) {
                sender.sendMessage(Util.formatTranslation("Message.No.UserHavePet", sender, args[0]));
            } else {
                sender.sendMessage(Translation.getComponent("Message.No.HasPet", sender));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            if (sender instanceof Player) {
                if (Permissions.has((Player) sender, "MyPet.command.info.other")) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return Collections.emptyList();
    }

    public enum PetInfoDisplay {
        Name(false), HP(false), Damage(false), Hunger(true), Exp(true), Level(true), Owner(false), Skilltree(true), RangedDamage(false), RespawnTime(true), Behavior(true);

        public boolean adminOnly;

        PetInfoDisplay(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }
    }
}