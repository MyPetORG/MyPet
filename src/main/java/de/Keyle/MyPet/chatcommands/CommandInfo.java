/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.skills.implementation.Damage;
import de.Keyle.MyPet.util.MyPetConfiguration;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandInfo implements CommandExecutor, TabCompleter
{
    private static List<String> emptyList = new ArrayList<String>();

    public enum PetInfoDisplay
    {
        Name(false), HP(false), Damage(false), Hunger(true), Exp(true), Level(true), Owner(false), Skilltree(true), RangedDamage(false);

        public boolean adminOnly = false;

        PetInfoDisplay(boolean adminOnly)
        {
            this.adminOnly = adminOnly;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            String playerName = sender.getName();
            if (args.length > 0 && MyPetPermissions.has(player, "MyPet.admin", false))
            {
                playerName = args[0];
            }

            Player petOwner = Bukkit.getServer().getPlayer(playerName);

            if (petOwner == null || !petOwner.isOnline())
            {
                sender.sendMessage(MyPetLocales.getString("Message.PlayerNotOnline", player));
            }
            else if (MyPetList.hasMyPet(playerName))
            {
                boolean infoShown = false;
                MyPetPlayer myPetPlayer = MyPetPlayer.getMyPetPlayer(player);
                MyPet myPet = MyPetList.getMyPet(playerName);

                if (canSee(PetInfoDisplay.Name.adminOnly, myPetPlayer, myPet))
                {
                    player.sendMessage(ChatColor.AQUA + myPet.getPetName() + ChatColor.RESET + ":");
                    infoShown = true;
                }
                if (!playerName.equalsIgnoreCase(sender.getName()) && canSee(!PetInfoDisplay.Owner.adminOnly, myPetPlayer, myPet))
                {
                    player.sendMessage("   " + MyPetLocales.getString("Name.Owner", player) + ": " + playerName);
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.HP.adminOnly, myPetPlayer, myPet))
                {
                    String msg;
                    if (myPet.getStatus() == PetState.Dead)
                    {
                        msg = ChatColor.RED + MyPetLocales.getString("Name.Dead", player);
                    }
                    else if (myPet.getHealth() > myPet.getMaxHealth() / 3 * 2)
                    {
                        msg = "" + ChatColor.GREEN;
                    }
                    else if (myPet.getHealth() > myPet.getMaxHealth() / 3)
                    {
                        msg = "" + ChatColor.YELLOW;
                    }
                    else
                    {
                        msg = "" + ChatColor.RED;
                    }
                    msg += String.format("%1.2f", myPet.getHealth()) + ChatColor.WHITE + "/" + String.format("%1.2f", myPet.getMaxHealth());
                    player.sendMessage("   " + MyPetLocales.getString("Name.HP", player) + ": " + msg);
                    infoShown = true;
                }
                if (!myPet.isPassiv() && canSee(PetInfoDisplay.Damage.adminOnly, myPetPlayer, myPet))
                {
                    double damage = (myPet.getSkills().isSkillActive("Damage") ? ((Damage) myPet.getSkills().getSkill("Damage")).getDamage() : 0);
                    player.sendMessage("   " + MyPetLocales.getString("Name.Damage", player) + ": " + String.format("%1.2f", damage));
                    infoShown = true;
                }
                if (myPet.getRangedDamage() > 0 && CommandInfo.canSee(PetInfoDisplay.RangedDamage.adminOnly, myPetPlayer, myPet))
                {
                    double damage = myPet.getRangedDamage();
                    player.sendMessage("   " + MyPetLocales.getString("Name.RangedDamage", player) + ": " + String.format("%1.2f", damage));
                    infoShown = true;
                }
                if (MyPetConfiguration.USE_HUNGER_SYSTEM && canSee(PetInfoDisplay.Hunger.adminOnly, myPetPlayer, myPet))
                {
                    player.sendMessage("   " + MyPetLocales.getString("Name.Hunger", player) + ": " + myPet.getHungerValue());
                    infoShown = true;
                }
                if (canSee(PetInfoDisplay.Skilltree.adminOnly, myPetPlayer, myPet) && myPet.getSkillTree() != null)
                {
                    player.sendMessage("   " + MyPetLocales.getString("Name.Skilltree", player) + ": " + myPet.getSkillTree().getName());
                    infoShown = true;
                }
                if (MyPetConfiguration.USE_LEVEL_SYSTEM)
                {
                    if (canSee(PetInfoDisplay.Level.adminOnly, myPetPlayer, myPet))
                    {
                        int lvl = myPet.getExperience().getLevel();
                        player.sendMessage("   " + MyPetLocales.getString("Name.Level", player) + ": " + lvl);
                        infoShown = true;
                    }
                    if (canSee(PetInfoDisplay.Exp.adminOnly, myPetPlayer, myPet))
                    {
                        double exp = myPet.getExperience().getCurrentExp();
                        double reqEXP = myPet.getExperience().getRequiredExp();
                        player.sendMessage("   " + MyPetLocales.getString("Name.Exp", player) + ": " + String.format("%1.2f", exp) + "/" + String.format("%1.2f", reqEXP));
                        infoShown = true;
                    }
                }
                if (myPet.getOwner().isDonator())
                {
                    infoShown = true;
                    player.sendMessage("   " + myPet.getOwner().getDonationRank().getDisplayText());
                }
                if (!infoShown)
                {
                    sender.sendMessage(MyPetLocales.getString("Message.CantViewPetInfo", player));
                }
                return true;
            }
            else
            {
                if (args != null && args.length > 0)
                {
                    sender.sendMessage(MyPetUtil.formatText(MyPetLocales.getString("Message.UserDontHavePet", player), playerName));
                }
                else
                {
                    sender.sendMessage(MyPetLocales.getString("Message.DontHavePet", player));
                }
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        if (strings.length == 1 && MyPetPermissions.has((Player) commandSender, "MyPet.admin", false))
        {
            return null;
        }
        return emptyList;
    }

    public static boolean canSee(boolean adminOnly, MyPetPlayer myPetPlayer, MyPet myPet)
    {
        return !adminOnly || myPet.getOwner() == myPetPlayer || myPetPlayer.isMyPetAdmin();
    }
}