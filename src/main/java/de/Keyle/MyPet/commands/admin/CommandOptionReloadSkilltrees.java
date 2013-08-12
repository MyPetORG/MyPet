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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoader;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderNBT;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderYAML;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;

public class CommandOptionReloadSkilltrees implements CommandOption
{
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args)
    {
        SkillTreeMobType.clearMobTypes();
        String[] petTypes = new String[MyPetType.values().length + 1];
        petTypes[0] = "default";
        for (int i = 1 ; i <= MyPetType.values().length ; i++)
        {
            petTypes[i] = MyPetType.values()[i - 1].getTypeName();
        }
        for (MyPet myPet : MyPetList.getAllActiveMyPets())
        {
            myPet.getSkills().reset();
        }

        SkillTreeMobType.clearMobTypes();
        SkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        SkillTreeLoaderYAML.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        SkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);

        for (MyPetType mobType : MyPetType.values())
        {
            SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(mobType.getTypeName());
            SkillTreeLoader.addDefault(skillTreeMobType);
            SkillTreeLoader.manageInheritance(skillTreeMobType);
        }

        for (MyPet myPet : MyPetList.getAllActiveMyPets())
        {
            myPet.getSkills().reset();

            SkillTree skillTree = myPet.getSkillTree();
            if (skillTree != null)
            {
                String skilltreeName = skillTree.getName();
                if (SkillTreeMobType.hasMobType(myPet.getPetType().getTypeName()))
                {
                    SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByPetType(myPet.getPetType());

                    if (mobType.hasSkillTree(skilltreeName))
                    {
                        skillTree = mobType.getSkillTree(skilltreeName);
                    }
                    else
                    {
                        skillTree = null;
                    }
                }
                else
                {
                    skillTree = null;
                }
            }
            myPet.setSkilltree(skillTree);
            if (skillTree != null)
            {
                sender.sendMessage(Util.formatText(Locales.getString("Message.Command.Skills.Show", myPet.getOwner()), myPet.getPetName(), (myPet.getSkillTree() == null ? "-" : myPet.getSkillTree().getDisplayName())));
                for (ISkillInstance skill : myPet.getSkills().getSkills())
                {
                    if (skill.isActive())
                    {
                        myPet.sendMessageToOwner("  " + ChatColor.GREEN + skill.getName() + ChatColor.RESET + " " + skill.getFormattedValue());
                    }
                }
            }
        }
        for (InactiveMyPet myPet : MyPetList.getAllInactiveMyPets())
        {
            SkillTree skillTree = myPet.getSkillTree();
            if (skillTree != null)
            {
                String skilltreeName = skillTree.getName();
                if (SkillTreeMobType.getMobTypeByPetType(myPet.getPetType()) != null)
                {
                    SkillTreeMobType mobType = SkillTreeMobType.getMobTypeByPetType(myPet.getPetType());

                    if (mobType.hasSkillTree(skilltreeName))
                    {
                        skillTree = mobType.getSkillTree(skilltreeName);
                    }
                    else
                    {
                        skillTree = null;
                    }
                }
                else
                {
                    skillTree = null;
                }
            }
            myPet.setSkillTree(skillTree);
        }
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] skilltrees reloaded!");
        DebugLogger.info("Skilltrees reloaded.");

        return true;
    }
}