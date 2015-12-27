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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.commands.CommandOption;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CommandOptionTicket implements CommandOption {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        try {
            File ticketFile = new File(MyPetPlugin.getPlugin().getDataFolder(), "ticket.zip");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(ticketFile));

            addFileToZip(new File(MyPetPlugin.getPlugin().getDataFolder(), "config.yml"), out, "");
            addFileToZip(new File(MyPetPlugin.getPlugin().getDataFolder(), "My.Pets"), out, "");
            addFileToZip(new File(MyPetPlugin.getPlugin().getDataFolder(), "worldgroups.yml"), out, "");
            addFileToZip(new File(MyPetPlugin.getPlugin().getDataFolder(), "skilltrees"), out, "");
            addFileToZip(new File(MyPetPlugin.getPlugin().getDataFolder(), "logs" + File.separator + "MyPet.log"), out, "");
            addFileToZip(new File(MyPetPlugin.getPlugin().getDataFolder().getParentFile().getParentFile(), "logs" + File.separator + "latest.log"), out, "");

            out.close();

            sender.sendMessage(ChatColor.RED + "------------------------------------------------");
            sender.sendMessage("Ticket file created. Please upload this file somewhere and add the link to your ticket.");
            sender.sendMessage("  " + ticketFile.getAbsoluteFile());
            sender.sendMessage(ChatColor.RED + "------------------------------------------------");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void addFileToZip(File file, ZipOutputStream zip, String folder) throws IOException {
        if (file.isFile()) {
            FileInputStream in = new FileInputStream(file);
            zip.putNextEntry(new ZipEntry(folder + file.getName()));

            byte[] b = new byte[1024];
            int count;

            while ((count = in.read(b)) > 0) {
                System.out.println();
                zip.write(b, 0, count);
            }
            in.close();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File dirFile : files) {
                    addFileToZip(dirFile, zip, folder + file.getName() + File.separator);
                }
            }
        }
    }
}