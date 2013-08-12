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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.commands.admin.*;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.MyPetVersion;
import de.Keyle.MyPet.util.Permissions;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandAdmin implements CommandExecutor, TabCompleter
{
    private static List<String> optionsList = new ArrayList<String>();
    private static List<String> emptyList = new ArrayList<String>();
    private static List<String> addSetRemoveList = new ArrayList<String>();
    private static List<String> showList = new ArrayList<String>();
    private static List<String> petTypeList = new ArrayList<String>();
    private static Map<String, List<String>> petTypeOptionMap = new HashMap<String, List<String>>();
    private static Map<String, CommandOption> commandOptions = new HashMap<String, CommandOption>();

    static
    {
        optionsList.add("name");
        optionsList.add("exp");
        optionsList.add("respawn");
        optionsList.add("reload");
        optionsList.add("reloadskills");
        optionsList.add("skilltree");
        optionsList.add("build");
        optionsList.add("create");
        optionsList.add("clone");
        optionsList.add("remove");
        optionsList.add("cleanup");

        addSetRemoveList.add("add");
        addSetRemoveList.add("set");
        addSetRemoveList.add("remove");

        showList.add("show");
        showList.add("<number>");

        List<String> petTypeOptionList = new ArrayList<String>();

        petTypeOptionList.add("fire");
        petTypeOptionMap.put("blaze", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionMap.put("chicken", petTypeOptionList);
        petTypeOptionMap.put("cow", petTypeOptionList);
        petTypeOptionMap.put("mooshroom", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("powered");
        petTypeOptionMap.put("creeper", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("block:");
        petTypeOptionMap.put("enderman", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("size:");
        petTypeOptionMap.put("magmacube", petTypeOptionList);
        petTypeOptionMap.put("slime", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("cat:");
        petTypeOptionMap.put("ocelot", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("saddle");
        petTypeOptionMap.put("pig", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("color:");
        petTypeOptionMap.put("sheep", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("wither");
        petTypeOptionMap.put("skeleton", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("profession:");
        petTypeOptionMap.put("villager", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("angry");
        petTypeOptionList.add("tamed");
        petTypeOptionList.add("collar:");
        petTypeOptionMap.put("wolf", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("villager");
        petTypeOptionMap.put("zombie", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionMap.put("pigzombie", petTypeOptionList);

        petTypeOptionList = new ArrayList<String>();
        petTypeOptionList.add("baby");
        petTypeOptionList.add("chest");
        petTypeOptionList.add("saddle");
        petTypeOptionList.add("horse:");
        petTypeOptionList.add("variant:");
        petTypeOptionMap.put("horse", petTypeOptionList);

        for (MyPetType petType : MyPetType.values())
        {
            petTypeList.add(petType.getTypeName());
        }
    }

    public CommandAdmin()
    {
        commandOptions.put("name", new CommandOptionName());
        commandOptions.put("exp", new CommandOptionExp());
        commandOptions.put("respawn", new CommandOptionRespawn());
        commandOptions.put("reload", new CommandOptionReload());
        commandOptions.put("reloadskills", new CommandOptionReloadSkilltrees());
        commandOptions.put("skilltree", new CommandOptionSkilltree());
        commandOptions.put("create", new CommandOptionCreate());
        commandOptions.put("clone", new CommandOptionClone());
        commandOptions.put("remove", new CommandOptionRemove());
        commandOptions.put("cleanup", new CommandOptionCleanup());
        commandOptions.put("test", new CommandOptionTest());

        commandOptions.put("build", new CommandOption()
        {
            @Override
            public boolean onCommandOption(CommandSender sender, String[] parameter)
            {
                DebugLogger.info("MyPet-" + MyPetVersion.getMyPetVersion() + "-b#" + MyPetVersion.getMyPetBuild());
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] MyPet-" + MyPetVersion.getMyPetVersion() + "-b#" + MyPetVersion.getMyPetBuild());
                return true;
            }
        });
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            if (!Permissions.has((Player) sender, "MyPet.admin", false))
            {
                return true;
            }
        }

        if (args.length < 1)
        {
            return false;
        }

        String[] parameter = Arrays.copyOfRange(args, 1, args.length);
        CommandOption option = commandOptions.get(args[0].toLowerCase());

        if (option != null)
        {
            return option.onCommandOption(sender, parameter);
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        if (!Permissions.has((Player) commandSender, "MyPet.admin", false))
        {
            return emptyList;
        }
        if (strings.length == 1)
        {
            return optionsList;
        }
        else if (strings.length >= 1)
        {
            if (strings[0].equalsIgnoreCase("name"))
            {
                if (strings.length == 2)
                {
                    return null;
                }
                if (strings.length > 2)
                {
                    return emptyList;
                }
            }
            else if (strings[0].equalsIgnoreCase("exp"))
            {
                if (strings.length == 2)
                {
                    return null;
                }
                else if (strings.length == 3)
                {
                    return emptyList;
                }
                else if (strings.length == 4)
                {
                    return addSetRemoveList;
                }
            }
            else if (strings[0].equalsIgnoreCase("respawn"))
            {
                if (strings.length == 2)
                {
                    return null;
                }
                if (strings.length == 3)
                {
                    return showList;
                }
            }
            else if (strings[0].equalsIgnoreCase("skilltree"))
            {
                if (strings.length == 2)
                {
                    return null;
                }
                if (strings.length == 3)
                {
                    Player player = Bukkit.getServer().getPlayer(strings[1]);
                    if (player == null || !player.isOnline())
                    {
                        return emptyList;
                    }
                    if (MyPetList.hasMyPet(player))
                    {
                        MyPet myPet = MyPetList.getMyPet(player);
                        SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());

                        List<String> skilltreeList = new ArrayList<String>();
                        for (SkillTree skillTree : skillTreeMobType.getSkillTrees())
                        {
                            skilltreeList.add(skillTree.getName());
                        }
                        return skilltreeList;
                    }
                    return emptyList;
                }
            }
            else if (strings[0].equalsIgnoreCase("create"))
            {
                int forceOffset = 0;
                if (strings.length >= 2 && strings[1].equalsIgnoreCase("-f"))
                {
                    forceOffset = 1;
                }
                if (strings.length == 2 + forceOffset)
                {
                    return null;
                }
                if (strings.length == 3 + forceOffset)
                {
                    return petTypeList;
                }
                if (strings.length >= 4 + forceOffset)
                {
                    if (petTypeOptionMap.containsKey(strings[2 + forceOffset].toLowerCase()))
                    {
                        return petTypeOptionMap.get(strings[2 + forceOffset].toLowerCase());
                    }
                    return emptyList;
                }
            }
            else if (strings[0].equalsIgnoreCase("clone"))
            {
                if (strings.length == 2)
                {
                    return null;
                }
                if (strings.length == 3)
                {
                    return null;
                }
            }
            else if (strings[0].equalsIgnoreCase("remove"))
            {
                if (strings.length == 2)
                {
                    return null;
                }
            }
        }
        return emptyList;
    }
}