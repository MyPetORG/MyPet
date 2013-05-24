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
import de.Keyle.MyPet.entity.types.InactiveMyPet;
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
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.IntTag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandAdmin implements CommandExecutor, TabCompleter
{
    private static List<String> optionsList = new ArrayList<String>();
    private static List<String> emptyList = new ArrayList<String>();
    private static List<String> addSetRemoveList = new ArrayList<String>();
    private static List<String> showList = new ArrayList<String>();
    private static List<String> petTypeList = new ArrayList<String>();
    private static Map<String, List<String>> petTypeOptionMap = new HashMap<String, List<String>>();

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

        for (MyPetType petType : MyPetType.values())
        {
            petTypeList.add(petType.getTypeName());
        }
    }

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
            Player petOwner = Bukkit.getServer().getPlayer(args[1]);

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
            for (String arg : args)
            {
                if (!name.equals(""))
                {
                    name += " ";
                }
                name += arg;
            }
            name = MyPetBukkitUtil.setColors(name);

            Pattern regex = Pattern.compile("§[abcdefklmnor0-9]");
            Matcher regexMatcher = regex.matcher(name);
            if (regexMatcher.find())
            {
                name += MyPetBukkitUtil.setColors("%reset%");
            }

            myPet.setPetName(name);
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] new name is now: " + name);
        }
        else if (option.equalsIgnoreCase("exp") && args.length >= 3)
        {
            Player petOwner = Bukkit.getServer().getPlayer(args[1]);

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
            Player petOwner = Bukkit.getServer().getPlayer(args[1]);

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

                int lvl = myPet.getExperience().getLevel();
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
        else if (option.equalsIgnoreCase("skilltree"))
        {
            if (args.length < 3)
            {
                return false;
            }
            Player petOwner = Bukkit.getServer().getPlayer(args[1]);

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

            MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());
            if (skillTreeMobType.hasSkillTree(args[2]))
            {
                MyPetSkillTree skillTree = skillTreeMobType.getSkillTree(args[2]);
                if (myPet.setSkilltree(skillTree))
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SkilltreeSwitchedTo").replace("%name%", skillTree.getName())));
                }
                else
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_SkilltreeNotSwitched")));
                }
            }
            else
            {
                sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_CantFindSkilltree").replace("%name%", args[2])));
            }
        }
        else if (option.equalsIgnoreCase("create"))
        {
            if (args.length < 3)
            {
                return false;
            }
            MyPetType myPetType = MyPetType.getMyPetTypeByName(args[2]);
            if (myPetType != null)
            {
                Player owner = Bukkit.getPlayer(args[1]);
                if (owner == null || !owner.isOnline())
                {
                    sender.sendMessage(MyPetBukkitUtil.setColors(MyPetLanguage.getString("Msg_PlayerNotOnline")));
                    return true;
                }

                MyPetPlayer newOwner = MyPetPlayer.getMyPetPlayer(sender.getName());
                if (!newOwner.hasMyPet())
                {
                    InactiveMyPet inactiveMyPet = new InactiveMyPet(newOwner);
                    inactiveMyPet.setPetType(myPetType);
                    inactiveMyPet.setPetName(myPetType.getTypeName());
                    inactiveMyPet.setLocation(owner.getLocation());

                    CompoundTag compoundTag = inactiveMyPet.getInfo();
                    if (args.length > 3)
                    {
                        for (int i = 3 ; i < args.length ; i++)
                        {
                            if (args[i].equalsIgnoreCase("baby"))
                            {
                                compoundTag.getValue().put("Baby", new ByteTag("Baby", true));
                            }
                            else if (args[i].equalsIgnoreCase("fire"))
                            {
                                compoundTag.getValue().put("Fire", new ByteTag("Fire", true));
                            }
                            else if (args[i].equalsIgnoreCase("powered"))
                            {
                                compoundTag.getValue().put("Powered", new ByteTag("Powered", true));
                            }
                            else if (args[i].equalsIgnoreCase("saddle"))
                            {
                                compoundTag.getValue().put("Saddle", new ByteTag("Saddle", true));
                            }
                            else if (args[i].equalsIgnoreCase("sheared"))
                            {
                                compoundTag.getValue().put("Sheared", new ByteTag("Sheared", true));
                            }
                            else if (args[i].equalsIgnoreCase("wither"))
                            {
                                compoundTag.getValue().put("Wither", new ByteTag("Wither", true));
                            }
                            else if (args[i].equalsIgnoreCase("tamed"))
                            {
                                compoundTag.getValue().put("Tamed", new ByteTag("Tamed", true));
                            }
                            else if (args[i].equalsIgnoreCase("angry"))
                            {
                                compoundTag.getValue().put("Angry", new ByteTag("Angry", true));
                            }
                            else if (args[i].equalsIgnoreCase("villager"))
                            {
                                compoundTag.getValue().put("Villager", new ByteTag("Villager", true));
                            }
                            else if (args[i].startsWith("size:"))
                            {
                                String size = args[i].replace("size:", "");
                                if (MyPetUtil.isInt(size))
                                {
                                    compoundTag.getValue().put("Size", new IntTag("Size", Integer.parseInt(size)));
                                }
                            }
                            else if (args[i].startsWith("cat:"))
                            {
                                String catTypeString = args[i].replace("cat:", "");
                                if (MyPetUtil.isInt(catTypeString))
                                {
                                    int catType = Integer.parseInt(catTypeString);
                                    catType = Math.min(Math.max(0, catType), 3);
                                    compoundTag.getValue().put("CatType", new IntTag("CatType", catType));
                                }
                            }
                            else if (args[i].startsWith("profession:"))
                            {
                                String professionString = args[i].replace("profession:", "");
                                if (MyPetUtil.isInt(professionString))
                                {
                                    int profession = Integer.parseInt(professionString);
                                    profession = Math.min(Math.max(0, profession), 5);
                                    compoundTag.getValue().put("Profession", new IntTag("Profession", profession));
                                }
                            }
                            else if (args[i].startsWith("color:"))
                            {
                                String colorString = args[i].replace("color:", "");
                                if (MyPetUtil.isByte(colorString))
                                {
                                    byte color = Byte.parseByte(colorString);
                                    color = color > 15 ? 15 : color < 0 ? 0 : color;
                                    compoundTag.getValue().put("Color", new ByteTag("Color", color));
                                }
                            }
                            else if (args[i].startsWith("collar:"))
                            {
                                String colorString = args[i].replace("collar:", "");
                                if (MyPetUtil.isByte(colorString))
                                {
                                    byte color = Byte.parseByte(colorString);
                                    color = color > 15 ? 15 : color < 0 ? 0 : color;
                                    compoundTag.getValue().put("CollarColor", new ByteTag("CollarColor", color));
                                }
                            }
                            else if (args[i].startsWith("block:"))
                            {
                                String blocks = args[i].replace("block:", "");
                                String[] blockInfo = blocks.split(":");
                                if (blockInfo.length >= 1 && MyPetUtil.isInt(blockInfo[0]) && MyPetBukkitUtil.isValidMaterial(Integer.parseInt(blockInfo[0])))
                                {
                                    compoundTag.getValue().put("BlockID", new IntTag("BlockID", Integer.parseInt(blockInfo[0])));
                                }
                                if (blockInfo.length >= 2 && MyPetUtil.isInt(blockInfo[1]))
                                {
                                    int blockData = Integer.parseInt(blockInfo[1]);
                                    blockData = Math.min(Math.max(0, blockData), 15);
                                    MyPetLogger.write("bd:" + blockData);
                                    compoundTag.getValue().put("BlockData", new IntTag("BlockData", blockData));
                                }
                            }
                        }
                    }

                    MyPet myPet = MyPetList.setMyPetActive(inactiveMyPet);
                    myPet.createPet();
                }
                else
                {
                    sender.sendMessage(newOwner.getName() + " has an active MyPet already!");
                }
            }
        }
        /*
        else if (option.equalsIgnoreCase("test"))
        {

        }
        */
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        if (!MyPetPermissions.has((Player) commandSender, "MyPet.admin", false))
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
                        MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());

                        List<String> skilltreeList = new ArrayList<String>();
                        for (MyPetSkillTree skillTree : skillTreeMobType.getSkillTrees())
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
                if (strings.length == 2)
                {
                    return null;
                }
                if (strings.length == 3)
                {
                    return petTypeList;
                }
                if (strings.length >= 4)
                {
                    if (petTypeOptionMap.containsKey(strings[2].toLowerCase()))
                    {
                        return petTypeOptionMap.get(strings[2].toLowerCase());
                    }
                    return emptyList;
                }
            }
        }
        return emptyList;
    }
}