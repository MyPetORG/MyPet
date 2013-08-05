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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.skills.ISkillStorage;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.spout.nbt.CompoundTag;

public class CommandOptionClone implements CommandOption
{
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            return false;
        }

        String lang = BukkitUtil.getCommandSenderLanguage(sender);
        Player oldOwner = Bukkit.getPlayer(args[0]);
        if (oldOwner == null || !oldOwner.isOnline())
        {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Locales.getString("Message.No.PlayerOnline", lang));
            return true;
        }
        Player newOwner = Bukkit.getPlayer(args[1]);
        if (newOwner == null || !newOwner.isOnline())
        {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Locales.getString("Message.No.PlayerOnline", lang));
            return true;
        }

        MyPetPlayer oldPetOwner = MyPetPlayer.getMyPetPlayer(oldOwner);
        MyPetPlayer newPetOwner = MyPetPlayer.getMyPetPlayer(newOwner);

        if (!oldPetOwner.hasMyPet())
        {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Locales.getString("Message.No.UserHavePet", lang), oldOwner.getName()));
            return true;
        }
        if (newPetOwner.hasMyPet())
        {
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
        newPet.setInfo(oldPet.getExtendedInfo());
        newPet.setPetType(oldPet.getPetType());
        newPet.setSkillTree(oldPet.getSkillTree());
        newPet.setWorldGroup(oldPet.getWorldGroup());
        CompoundTag skillCompund = newPet.getSkills();
        for (ISkillInstance skill : oldPet.getSkills().getSkills())
        {
            if (skill instanceof ISkillStorage)
            {
                ISkillStorage storageSkill = (ISkillStorage) skill;
                CompoundTag s = storageSkill.save();
                if (s != null)
                {
                    skillCompund.getValue().put(skill.getName(), s);
                }
            }
        }

        MyPetList.addInactiveMyPet(newPet);
        MyPetList.setMyPetActive(newPet);

        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] MyPet owned by " + newOwner.getName() + " successfully cloned!");

        return true;
    }
}