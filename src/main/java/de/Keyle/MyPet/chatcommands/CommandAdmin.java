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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeLevel;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderNBT;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderYAML;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class CommandAdmin implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            if (!MyPetPermissions.has((Player) sender, "MyPet.admin", false))
            {
                return true;
            }
        }
        if (args.length < 1)
        {
            return false;
        }
        String option = args[0];

        if (option.equalsIgnoreCase("name") && args.length >= 3)
        {
            Player petOwner = MyPetBukkitUtil.getServer().getPlayer(args[1]);

            if (petOwner == null || !petOwner.isOnline())
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_PlayerNotOnline")));
                return true;
            }
            else if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner.getName())));
                return true;
            }
            MyPet myPet = MyPetList.getMyPet(petOwner);

            String name = "";
            for (int i = 2 ; i < args.length ; i++)
            {
                name += args[i] + " ";
            }
            name = name.substring(0, name.length() - 1);
            myPet.setPetName(name);
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] new name is now: " + name);
        }
        else if (option.equalsIgnoreCase("exp") && args.length >= 3)
        {
            Player petOwner = MyPetBukkitUtil.getServer().getPlayer(args[1]);

            if (petOwner == null || !petOwner.isOnline())
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_PlayerNotOnline")));
                return true;
            }
            else if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner.getName())));
                return true;
            }
            MyPet myPet = MyPetList.getMyPet(petOwner);
            String value = args[2];

            if (args.length == 3 || (args.length >= 4 && args[3].equalsIgnoreCase("set")))
            {
                if (MyPetUtil.isDouble(value))
                {
                    double Exp = Double.parseDouble(value);
                    Exp = Exp < 0 ? 0 : Exp;
                    if (myPet.getExperience().getExp() > Exp)
                    {
                        myPet.getSkills().reset();
                        myPet.getExperience().reset();
                        myPet.getExperience().addExp(Exp);
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set " + Exp + "exp. Pet is now level " + myPet.getExperience().getLevel() + ".");
                    }
                    else
                    {
                        myPet.getExperience().addExp(Exp - myPet.getExperience().getExp());
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set exp to " + Exp + "exp");
                    }
                }
            }
            else if (args.length >= 4 && args[3].equalsIgnoreCase("add"))
            {
                if (MyPetUtil.isDouble(value))
                {
                    double Exp = Double.parseDouble(value);
                    Exp = Exp < 0 ? 0 : Exp;
                    myPet.getExperience().addExp(Exp);
                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] added " + Exp + "exp.");
                }
            }
            else if (args.length >= 4 && args[3].equalsIgnoreCase("remove"))
            {
                if (MyPetUtil.isDouble(value))
                {
                    double Exp = Double.parseDouble(value);
                    Exp = Exp < 0 ? 0 : Exp;
                    Exp = Exp <= myPet.getExperience().getExp() ? Exp : myPet.getExperience().getExp();
                    if (Exp <= myPet.getExperience().getCurrentExp())
                    {
                        myPet.getExperience().removeExp(Exp);
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + value + "exp.");
                    }
                    else
                    {
                        Exp = myPet.getExperience().getExp() - Exp;
                        myPet.getSkills().reset();
                        myPet.getExperience().reset();
                        myPet.getExperience().addExp(Exp);
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + Exp + "exp. Pet is now level " + myPet.getExperience().getLevel() + ".");
                    }
                }
            }
        }
        else if (option.equalsIgnoreCase("respawn") && args.length >= 2)
        {
            Player petOwner = MyPetBukkitUtil.getServer().getPlayer(args[1]);

            if (petOwner == null || !petOwner.isOnline())
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_PlayerNotOnline")));
                return true;
            }
            else if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner.getName())));
                return true;
            }
            MyPet myPet = MyPetList.getMyPet(petOwner);
            if (args.length >= 3 && args[2].equalsIgnoreCase("show"))
            {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] respawn time: " + myPet.respawnTime + "sec.");
            }
            else if (myPet.getStatus() == PetState.Dead)
            {
                if (args.length >= 3 && MyPetUtil.isInt(args[2]))
                {
                    int respawnTime = Integer.parseInt(args[2]);
                    if (respawnTime >= 0)
                    {
                        myPet.respawnTime = respawnTime;
                    }
                }
                else
                {
                    myPet.respawnTime = 0;
                }
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set respawn time to: " + myPet.respawnTime + "sec.");
            }
            else
            {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] pet is not dead!");
            }
        }
        else if (option.equalsIgnoreCase("reload"))
        {
            MyPetPlugin.getPlugin().reloadConfig();
            MyPetConfiguration.config = MyPetPlugin.getPlugin().getConfig();
            MyPetConfiguration.loadConfiguration();
            DebugLogger.info("Config reloaded.");
            sender.sendMessage(MyPetBukkitUtil.setColors("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] config (config.yml) reloaded!"));
        }
        else if (option.equalsIgnoreCase("build"))
        {
            DebugLogger.info("MyPet-" + MyPetVersion.getMyPetVersion() + "-b#" + MyPetVersion.getMyPetBuild());
            sender.sendMessage("MyPet-" + MyPetVersion.getMyPetVersion() + "-b#" + MyPetVersion.getMyPetBuild());
        }
        else if (option.equalsIgnoreCase("reloadskills"))
        {
            MyPetSkillTreeMobType.clearMobTypes();
            String[] petTypes = new String[MyPetType.values().length];
            for (int i = 0 ; i < MyPetType.values().length ; i++)
            {
                petTypes[i] = MyPetType.values()[i].getTypeName();
            }

            MyPetSkillTreeMobType.clearMobTypes();
            MyPetSkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
            MyPetSkillTreeLoaderYAML.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
            MyPetSkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);

            for (MyPet myPet : MyPetList.getAllActiveMyPets())
            {
                myPet.getSkills().reset();

                short lvl = myPet.getExperience().getLevel();
                MyPetSkillTree skillTree = myPet.getSkillTree();

                if (skillTree != null)
                {
                    for (MyPetSkillTreeLevel level : skillTree.getLevelList())
                    {
                        if (level.getLevel() > lvl)
                        {
                            continue;
                        }
                        for (ISkillInfo skill : level.getSkills())
                        {
                            myPet.getSkills().getSkill(skill.getName()).upgrade(skill, true);
                        }
                    }
                    for (ISkillInstance skill : myPet.getSkills().getSkills())
                    {
                        if (skill.isActive())
                        {
                            myPet.sendMessageToOwner(MyPetBukkitUtil.setColors("%green%%skillname%%white% " + skill.getFormattedValue()).replace("%skillname%", skill.getName()));
                        }
                    }
                }
            }
            sender.sendMessage(MyPetBukkitUtil.setColors("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] skilltrees reloaded!"));
            DebugLogger.info("Skilltrees reloaded.");
        }
        return true;
    }
}