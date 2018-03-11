/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLoaderJSON;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import java.io.File;

public class CommandOptionReloadSkilltrees implements CommandOption {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        MyPetApi.getSkilltreeManager().clearSkilltrees();

        SkillTreeLoaderJSON.loadSkilltrees(new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"));

        // register skilltree permissions
        for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getSkilltrees()) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission(skilltree.getPermission()));
            } catch (Exception ignored) {
            }
        }

        for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
            Skilltree skilltree = myPet.getSkilltree();
            if (skilltree != null) {
                String skilltreeName = skilltree.getName();
                if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
                    skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
                    if (!skilltree.getMobTypes().contains(myPet.getPetType())) {
                        skilltree = null;
                    }
                } else {
                    skilltree = null;
                }
            }
            myPet.setSkilltree(skilltree);
            if (skilltree != null) {
                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skills.Show", myPet.getOwner()), myPet.getPetName(), (myPet.getSkilltree() == null ? "-" : myPet.getSkilltree().getDisplayName())));
                for (Skill skill : myPet.getSkills().all()) {
                    if (skill.isActive()) {
                        myPet.getOwner().sendMessage("  " + ChatColor.GREEN + skill.getName(myPet.getOwner().getLanguage()) + ChatColor.RESET + " " + skill.toPrettyString());
                    }
                }
            }
        }
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] skilltrees loaded!");

        return true;
    }
}