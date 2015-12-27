/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.commands.CommandAdmin;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.WorldGroup;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandOptionClone implements CommandOptionTabCompleter {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        String lang = BukkitUtil.getCommandSenderLanguage(sender);
        Player oldOwner = Bukkit.getPlayer(args[0]);
        if (oldOwner == null || !oldOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Locales.getString("Message.No.PlayerOnline", lang));
            return true;
        }
        Player newOwner = Bukkit.getPlayer(args[1]);
        if (newOwner == null || !newOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Locales.getString("Message.No.PlayerOnline", lang));
            return true;
        }

        if (!PlayerList.isMyPetPlayer(oldOwner)) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Locales.getString("Message.No.UserHavePet", lang), oldOwner.getName()));
            return true;
        }

        MyPetPlayer oldPetOwner = PlayerList.getMyPetPlayer(oldOwner);

        if (!oldPetOwner.hasMyPet()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Locales.getString("Message.No.UserHavePet", lang), oldOwner.getName()));
            return true;
        }

        MyPetPlayer newPetOwner;
        if (PlayerList.isMyPetPlayer(newOwner)) {
            newPetOwner = PlayerList.getMyPetPlayer(newOwner);
        } else {
            newPetOwner = PlayerList.registerMyPetPlayer(newOwner);
        }

        if (newPetOwner.hasMyPet()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + newOwner.getName() + " has already an active MyPet!");
            return true;
        }

        MyPet oldPet = oldPetOwner.getMyPet();
        InactiveMyPet newPet = new InactiveMyPet(newPetOwner);
        newPet.setPetName(oldPet.getPetName());
        newPet.setExp(oldPet.getExperience().getExp());
        newPet.setHealth(oldPet.getHealth());
        newPet.setHungerValue(oldPet.getHungerValue());
        newPet.setRespawnTime(oldPet.getRespawnTime());
        newPet.setInfo(oldPet.writeExtendedInfo());
        newPet.setPetType(oldPet.getPetType());
        newPet.setSkillTree(oldPet.getSkillTree());
        newPet.setWorldGroup(oldPet.getWorldGroup());
        TagCompound skillCompund = newPet.getSkills();
        for (ISkillInstance skill : oldPet.getSkills().getSkills()) {
            if (skill instanceof ISkillStorage) {
                ISkillStorage storageSkill = (ISkillStorage) skill;
                TagCompound s = storageSkill.save();
                if (s != null) {
                    skillCompund.getCompoundData().put(skill.getName(), s);
                }
            }
        }

        MyPetList.addInactiveMyPet(newPet);
        MyPet myPet = MyPetList.activateMyPet(newPet);

        if (myPet != null) {
            WorldGroup worldGroup = WorldGroup.getGroupByWorld(newPet.getOwner().getPlayer().getWorld().getName());
            newPet.setWorldGroup(worldGroup.getName());
            newPet.getOwner().setMyPetForWorldGroup(worldGroup.getName(), newPet.getUUID());
        }


        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] MyPet owned by " + newOwner.getName() + " successfully cloned!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        if (strings.length == 3) {
            return null;
        }
        return CommandAdmin.emptyList;
    }
}