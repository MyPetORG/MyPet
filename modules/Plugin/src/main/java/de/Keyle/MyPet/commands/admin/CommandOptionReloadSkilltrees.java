/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.skill.skilltreeloader.SkillTreeLoader;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderJSON;
import de.Keyle.MyPet.skill.skilltreeloader.SkillTreeLoaderNBT;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class CommandOptionReloadSkilltrees implements CommandOption {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        SkillTreeMobType.clearMobTypes();
        String[] petTypes = new String[MyPetType.values().length + 1];
        petTypes[0] = "default";
        for (int i = 1; i <= MyPetType.values().length; i++) {
            petTypes[i] = MyPetType.values()[i - 1].name();
        }
        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            myPet.getSkills().reset();
        }

        SkillTreeMobType.clearMobTypes();
        SkillTreeLoaderNBT.getSkilltreeLoader().loadSkillTrees(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);
        SkillTreeLoaderJSON.getSkilltreeLoader().loadSkillTrees(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "skilltrees", petTypes);

        Set<String> skilltreeNames = new LinkedHashSet<>();
        for (MyPetType mobType : MyPetType.values()) {
            SkillTreeMobType skillTreeMobType = SkillTreeMobType.byPetType(mobType);
            SkillTreeLoader.addDefault(skillTreeMobType);
            SkillTreeLoader.manageInheritance(skillTreeMobType);
            skilltreeNames.addAll(skillTreeMobType.getSkillTreeNames());
        }
        // register skilltree permissions
        for (String skilltreeName : skilltreeNames) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.custom.skilltree." + skilltreeName));
            } catch (Exception ignored) {
            }
        }

        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            myPet.getSkills().reset();

            SkillTree skillTree = myPet.getSkilltree();
            if (skillTree != null) {
                String skilltreeName = skillTree.getName();
                if (SkillTreeMobType.hasMobType(myPet.getPetType())) {
                    SkillTreeMobType mobType = SkillTreeMobType.byPetType(myPet.getPetType());

                    if (mobType.hasSkillTree(skilltreeName)) {
                        skillTree = mobType.getSkillTree(skilltreeName);
                    } else {
                        skillTree = null;
                    }
                } else {
                    skillTree = null;
                }
            }
            myPet.setSkilltree(skillTree);
            if (skillTree != null) {
                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skills.Show", myPet.getOwner()), myPet.getPetName(), (myPet.getSkilltree() == null ? "-" : myPet.getSkilltree().getDisplayName())));
                for (SkillInstance skill : myPet.getSkills().getSkills()) {
                    if (skill.isActive()) {
                        myPet.getOwner().sendMessage("  " + ChatColor.GREEN + skill.getName(myPet.getOwner().getLanguage()) + ChatColor.RESET + " " + skill.getFormattedValue());
                    }
                }
            }
        }
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] skilltrees loaded!");

        return true;
    }
}