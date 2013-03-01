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
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.MyPetSkillTreeLoaderNBT;
import de.Keyle.MyPet.util.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

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
            String petOwner = args[1];
            if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
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
            MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] new name is now: " + name);
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] new name is now: " + name);
        }
        else if (option.equalsIgnoreCase("exp") && args.length >= 3)
        {
            String petOwner = args[1];
            if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
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
                        MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set " + Exp + "exp. Pet is now level " + myPet.getExperience().getLevel() + ".");
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set " + Exp + "exp. Pet is now level " + myPet.getExperience().getLevel() + ".");
                    }
                    else
                    {
                        myPet.getExperience().addExp(Exp - myPet.getExperience().getExp());
                        MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set exp to " + Exp + "exp");
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
                    MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] added " + Exp + "exp.");
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
                        MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + Exp + "exp.");
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + Exp + "exp.");
                    }
                    else
                    {
                        Exp = myPet.getExperience().getExp() - Exp;
                        myPet.getSkills().reset();
                        myPet.getExperience().reset();
                        myPet.getExperience().addExp(Exp);
                        MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + Exp + "exp. Pet is now level " + myPet.getExperience().getLevel() + ".");
                        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + Exp + "exp. Pet is now level " + myPet.getExperience().getLevel() + ".");
                    }
                }
            }
        }
        else if (option.equalsIgnoreCase("respawn"))
        {
            String petOwner = args[1];
            if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
                return true;
            }
            MyPet myPet = MyPetList.getMyPet(petOwner);
            if (args.length >= 3 && args[2].equalsIgnoreCase("show"))
            {
                MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] respawn time: " + myPet.respawnTime + "sec.");
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
                MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set respawn time to: " + myPet.respawnTime + "sec.");
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set respawn time to: " + myPet.respawnTime + "sec.");
            }
            else
            {
                MyPetUtil.getDebugLogger().info("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] pet is not dead!");
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] pet is not dead!");
            }
        }
        else if (option.equalsIgnoreCase("reload"))
        {
            MyPetPlugin.getPlugin().reloadConfig();
            MyPetConfiguration.config = MyPetPlugin.getPlugin().getConfig();
            MyPetConfiguration.loadConfiguration();
            MyPetUtil.getDebugLogger().info("Config reloaded.");
            sender.sendMessage(MyPetUtil.setColors("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] config (config.yml) reloaded!"));
        }
        else if (option.equalsIgnoreCase("build"))
        {
            MyPetUtil.getDebugLogger().info("MyPet-" + MyPetPlugin.MyPetVersion + "-b#" + MyPetPlugin.MyPetBuild);
            sender.sendMessage("MyPet-" + MyPetPlugin.MyPetVersion + "-b#" + MyPetPlugin.MyPetBuild);
        }
        else if (option.equalsIgnoreCase("reloadskills"))
        {
            MyPetSkillTreeMobType.clearMobTypes();
            MyPetSkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees");
            MyPetSkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees");
            MyPetUtil.getDebugLogger().info("Skilltrees reloaded.");

            for(MyPet myPet : MyPetList.getAllMyPets())
            {
                myPet.getSkills().reset();

                short lvl = myPet.getExperience().getLevel();
                MyPetSkillTree skillTree = myPet.getSkillTree();
                if (skillTree != null && skillTree.hasLevel(lvl))
                {
                    List<MyPetSkillTreeSkill> skillList = skillTree.getLevel(lvl).getSkills();
                    for (MyPetSkillTreeSkill skill : skillList)
                    {
                        myPet.getSkills().getSkill(skill.getName()).upgrade(skill);
                    }
                }
            }
            sender.sendMessage(MyPetUtil.setColors("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] skilltrees reloaded!"));
        }
        return true;
    }
}