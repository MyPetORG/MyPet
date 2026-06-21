/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.commands.mypet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.util.ErrorUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Provides the {@code /mypet ticket} subcommand, which generates a diagnostic ZIP archive
 * ({@code ticket.zip}) in the MyPet data folder for use when submitting support tickets.
 *
 * <h3>Usage</h3>
 * <p>{@code /mypet ticket}</p>
 *
 * <h3>Permissions</h3>
 * <ul>
 *   <li>{@code MyPet.admin.ticket} -- required for players; console can always execute (granted by the {@code MyPet.admin} bundle)</li>
 * </ul>
 *
 * <p>The generated ZIP includes configuration files ({@code config.yml}, {@code pet-config.yml},
 * {@code hooks-config.yml}, {@code pet-shops.yml}, {@code worldgroups.yml}), the skilltrees
 * directory, the MyPet log, the server's latest log, the SQLite database ({@code pets.db}),
 * legacy data ({@code My.Pets.old}), and a snapshot of all online players' effective
 * permissions.</p>
 */
public class CommandOptionTicket {

    /**
     * Builds and returns the Brigadier {@code "ticket"} literal command node.
     * This node is intended to be attached as a child of the {@code /mypet} command tree.
     *
     * @return the built {@link LiteralCommandNode} for the ticket subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("ticket")
                .requires(AdminPermissions.requiresNode(AdminPermissions.TICKET))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    File ticketFile = new File(MyPetApi.getPlugin().getDataFolder(), "ticket.zip");
                    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(ticketFile.toPath()))) {
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "config.yml"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "pet-config.yml"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "hooks-config.yml"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "pet-shops.yml"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "My.Pets.old"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "pets.db"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "worldgroups.yml"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder(), "skilltrees"), out, "");
                        addFileToZip(new File(MyPetApi.getPlugin().getDataFolder().getParentFile().getParentFile(), "logs" + File.separator + "latest.log"), out, "");
                        writeStreamToZip(new ByteArrayInputStream(accumulatePermissions().getBytes()), "permissions.txt", out);

                        sender.sendMessage(Component.text("------------------------------------------------").color(NamedTextColor.RED));
                        sender.sendMessage("Ticket file created. Please upload this file somewhere and add the link to your ticket.");
                        sender.sendMessage("  " + ticketFile.getAbsoluteFile());
                        sender.sendMessage(Component.text("------------------------------------------------").color(NamedTextColor.RED));
                    } catch (IOException e) {
                        ErrorUtil.reportWarning("Failed to create debug ticket file", e);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /**
     * Collects the effective (granted) permissions for every online player into a
     * human-readable string, sorted alphabetically per player.
     *
     * @return a formatted string listing each online player's UUID and their granted permissions
     */
    private String accumulatePermissions() {
        StringBuilder retValue = new StringBuilder();
        for (Player p : Bukkit.getOnlinePlayers()) {
            retValue.append(p.getName()).append(" (").append(p.getUniqueId()).append(")\n");
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

    /**
     * Recursively adds a file or directory to the ZIP output stream.
     *
     * @param file   the file or directory to add
     * @param zip    the ZIP output stream to write to
     * @param folder the path prefix within the ZIP archive
     * @throws IOException if an I/O error occurs during writing
     */
    private void addFileToZip(File file, ZipOutputStream zip, String folder) throws IOException {
        if (file.isFile()) {
            try (InputStream in = Files.newInputStream(file.toPath())) {
                this.writeStreamToZip(in, folder + file.getName(), zip);
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File dirFile : files) {
                    addFileToZip(dirFile, zip, folder + file.getName() + File.separator);
                }
            }
        }
    }

    /**
     * Writes the contents of an input stream as a new entry in the ZIP output stream.
     *
     * @param in   the input stream to read from
     * @param file the entry name (path) within the ZIP archive
     * @param zip  the ZIP output stream to write to
     * @throws IOException if an I/O error occurs during writing
     */
    private void writeStreamToZip(InputStream in, String file, ZipOutputStream zip) throws IOException {
        zip.putNextEntry(new ZipEntry(file));
        in.transferTo(zip);
    }
}
