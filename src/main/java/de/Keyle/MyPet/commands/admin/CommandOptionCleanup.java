/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.util.Util;
import org.bukkit.command.CommandSender;

public class CommandOptionCleanup implements CommandOption {
    @Override
    public boolean onCommandOption(final CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }

        if (Util.isInt(args[0])) {
            final int days = Integer.parseInt(args[0]);

            /*
            MyPetList.getAllInactiveMyPets(new RepositoryCallback<Collection<InactiveMyPet>>() {
                @Override
                public void callback(Collection<InactiveMyPet> value) {
                    boolean deleteOld = days == -1;
                    List<InactiveMyPet> deletionList = new ArrayList<>();
                    for (InactiveMyPet inactiveMyPet : value) {
                        if (inactiveMyPet.getLastUsed() != -1 && !deleteOld) {
                            if (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - inactiveMyPet.getLastUsed()) > days) {
                                deletionList.add(inactiveMyPet);
                            }
                        } else if (inactiveMyPet.getLastUsed() == -1 && deleteOld) {
                            deletionList.add(inactiveMyPet);
                        }
                    }
                    int deletedPetCount = deletionList.size();
                    if (deletedPetCount > 0) {
                        if (Backup.MAKE_BACKUPS) {
                            //ToDo
                            //sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] created backup -> " + MyPetPlugin.getPlugin().getBackupManager().createBackup());
                        }

                        for (InactiveMyPet inactiveMyPet : deletionList) {
                            MyPetList.removeInactiveMyPet(inactiveMyPet);
                        }
                    }
                    sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] removed " + deletedPetCount + " MyPets.");
                }
            });
            */
        }
        return true;
    }
}