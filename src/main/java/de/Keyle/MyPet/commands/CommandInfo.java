/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Damage;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.DonateCheck;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandInfo implements CommandExecutor, TabCompleter {
    private static List<String> emptyList = new ArrayList<String>();

    public enum PetInfoDisplay {
        Name(false), HP(false), Damage(false), Hunger(true), Exp(true), Level(true), Owner(false), Skilltree(true), RangedDamage(false), RespawnTime(true), Behavior(true);

        public boolean adminOnly = false;

        PetInfoDisplay(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            MyPetPlayer petOwner;

            if (args.length == 0) {
                if (PlayerList.isMyPetPlayer(player)) {
                    petOwner = PlayerList.getMyPetPlayer(player);
                } else {
                    sender.sendMessage(Locales.getString("Message.No.HasPet", player));
                    return true;
                }
            } else if (Permissions.has(player, "MyPet.admin", false)) {
                if (PlayerList.isMyPetPlayer(args[0])) {
                    petOwner = PlayerList.getMyPetPlayer(args[0]);
                } else {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.No.UserHavePet", player), args[0]));
                    return true;
                }
            } else {
                sender.sendMessage(Locales.getString("Message.No.HasPet", player));
                return true;
            }

            if (petOwner.hasMyPet()) {
                boolean infoShown = false;
                MyPet myPet = petOwner.getMyPet();

                if (canSee(PetInfoDisplay.Name.adminOnly, player, myPet)) {
                    player.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                    infoShown = true;
                }
                if (player != petOwner && canSee(!PetInfoDisplay.Owner.adminOnly, player, myPet)) {
                    player.sendMessage("   " + Locales.getString("Name.Owner", player) + ": " + myPet.getOwner().getName());
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.HP.adminOnly, player, myPet)) {
                    String msg;
                    if (myPet.getStatus() == PetState.Dead) {
                        msg = ChatColor.RED + Locales.getString("Name.Dead", player);
                    } else {
                        if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2) {
                            msg = "" + ChatColor.GREEN;
                        } else if (myPet.getHealth() > myPet.getMaxHealth() / 3) {
                            msg = "" + ChatColor.YELLOW;
                        } else {
                            msg = "" + ChatColor.RED;
                        }
                        msg += String.format("%1.2f", myPet.getHealth()) + ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                    }
                    player.sendMessage("   " + Locales.getString("Name.HP", player) + ": " + msg);
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.RespawnTime.adminOnly, player, myPet)) {
                    if (myPet.getStatus() == PetState.Dead) {
                        player.sendMessage("   " + Locales.getString("Name.Respawntime", player) + ": " + myPet.getRespawnTime());
                        infoShown = true;
                    }
                }
                if (!myPet.isPassiv() && canSee(PetInfoDisplay.Damage.adminOnly, player, myPet)) {
                    double damage = (myPet.getSkills().isSkillActive(Damage.class) ? myPet.getSkills().getSkill(Damage.class).getDamage() : 0);
                    player.sendMessage("   " + Locales.getString("Name.Damage", player) + ": " + String.format("%1.2f", damage));
                    infoShown = true;
                }
                if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, player, myPet)) {
                    double damage = myPet.getRangedDamage();
                    player.sendMessage("   " + Locales.getString("Name.RangedDamage", player) + ": " + String.format("%1.2f", damage));
                    infoShown = true;
                }
                if (Configuration.USE_HUNGER_SYSTEM && canSee(PetInfoDisplay.Hunger.adminOnly, player, myPet)) {
                    player.sendMessage("   " + Locales.getString("Name.Hunger", player) + ": " + myPet.getHungerValue());
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.Behavior.adminOnly, player, myPet)) {
                    if (myPet.getSkills().hasSkill(Behavior.class)) {
                        Behavior behavior = myPet.getSkills().getSkill(Behavior.class);
                        player.sendMessage("   Behavior: " + Locales.getString("Name." + behavior.getBehavior().name(), player));
                        infoShown = true;
                    }
                }
                if (canSee(PetInfoDisplay.Skilltree.adminOnly, player, myPet) && myPet.getSkillTree() != null) {
                    player.sendMessage("   " + Locales.getString("Name.Skilltree", player) + ": " + myPet.getSkillTree().getName());
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.Level.adminOnly, player, myPet)) {
                    int lvl = myPet.getExperience().getLevel();
                    player.sendMessage("   " + Locales.getString("Name.Level", player) + ": " + lvl);
                    infoShown = true;
                }
                int maxLevel = myPet.getSkillTree() != null ? myPet.getSkillTree().getMaxLevel() : Configuration.LEVEL_CAP;
                if (canSee(PetInfoDisplay.Exp.adminOnly, player, myPet) && myPet.getExperience().getLevel() < maxLevel) {
                    double exp = myPet.getExperience().getCurrentExp();
                    double reqEXP = myPet.getExperience().getRequiredExp();
                    player.sendMessage("   " + Locales.getString("Name.Exp", player) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                    infoShown = true;
                }
                if (myPet.getOwner().getDonationRank() != DonateCheck.DonationRank.None) {
                    infoShown = true;
                    sender.sendMessage("   " + myPet.getOwner().getDonationRank().getDisplayText());
                }
                if (!infoShown) {
                    sender.sendMessage(Locales.getString("Message.CantViewPetInfo", player));
                }
                return true;
            } else {
                if (args.length > 0) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.No.UserHavePet", player), args[0]));
                } else {
                    sender.sendMessage(Locales.getString("Message.No.HasPet", player));
                }
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1 && Permissions.has((Player) commandSender, "MyPet.admin", false)) {
            return null;
        }
        return emptyList;
    }

    public static boolean canSee(boolean adminOnly, Player player, MyPet myPet) {
        return !adminOnly || myPet.getOwner().getPlayer() == player || Permissions.has(player, "MyPet.admin", false);
    }
}