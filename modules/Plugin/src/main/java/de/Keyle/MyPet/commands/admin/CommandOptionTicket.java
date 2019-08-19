/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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
import de.Keyle.MyPet.api.commands.CommandOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CommandOptionTicket implements CommandOption {

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        try {
            File ticketFile = new File(MyPetApi.getPlugin().getDataFolder(), "ticket.zip");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(ticketFile));

            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "config.yml"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "pet-config.yml"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "pet-shops.yml"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "My.Pets.old"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "pets.db"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "worldgroups.yml"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "logs" + File.separator + "MyPet.log"), out, "");
            addFileToZip(new File(MyPetApi.getPlugin().getDataFolder().getParentFile().getParentFile(), "logs" + File.separator + "latest.log"), out, "");
            writeStreamToZip(new ByteArrayInputStream(accumulatePermissions().getBytes()), "permissions.txt", out);

            out.close();

            sender.sendMessage(ChatColor.RED + "------------------------------------------------");
            sender.sendMessage("Ticket file created. Please upload this file somewhere and add the link to your ticket.");
            sender.sendMessage("  " + ticketFile.getAbsoluteFile());
            sender.sendMessage(ChatColor.RED + "------------------------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private String accumulatePermissions() {
        StringBuilder retValue = new StringBuilder();
        for (Player p : Bukkit.getOnlinePlayers()) {
            retValue.append(p.getName()).append(" (").append(p.getUniqueId().toString()).append(")\n");
            List<String> permList = new ArrayList<>();
            for (PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
                if (perm.getValue()) {
                    permList.add(perm.getPermission());
                }
            }
            Collections.sort(permList);
            for (String perm : permList) {
                retValue.append("    ").append(perm).append("\n");
            }
            retValue.append("\n\n\n");
        }
        return retValue.toString();
    }

    private void addFileToZip(File file, ZipOutputStream zip, String folder) throws IOException {
        if (file.isFile()) {
            FileInputStream in = new FileInputStream(file);
            this.writeStreamToZip(in, folder + file.getName(), zip);
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File dirFile : files) {
                    addFileToZip(dirFile, zip, folder + file.getName() + File.separator);
                }
            }
        }
    }

    private void writeStreamToZip(InputStream in, String file, ZipOutputStream zip) throws IOException {
        zip.putNextEntry(new ZipEntry(file));

        byte[] b = new byte[1024];
        int count;

        while ((count = in.read(b)) > 0) {
            zip.write(b, 0, count);
        }
        in.close();
    }
}