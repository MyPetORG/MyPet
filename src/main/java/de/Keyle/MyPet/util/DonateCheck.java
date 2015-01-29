/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2015 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class DonateCheck {
    public enum DonationRank {
        Creator(ChatColor.GOLD + "☣ " + ChatColor.UNDERLINE + "Creator of MyPet" + ChatColor.RESET + ChatColor.GOLD + " ☣" + ChatColor.RESET),
        Donator(ChatColor.GOLD + "❤ " + ChatColor.UNDERLINE + "Donator" + ChatColor.RESET + ChatColor.GOLD + " ❤" + ChatColor.RESET),
        Translator(ChatColor.GOLD + "✈ " + ChatColor.UNDERLINE + "Translator" + ChatColor.RESET + ChatColor.GOLD + " ✈" + ChatColor.RESET),
        Developer(ChatColor.GOLD + "✪ " + ChatColor.UNDERLINE + "Developer" + ChatColor.RESET + ChatColor.GOLD + " ✪" + ChatColor.RESET),
        Helper(ChatColor.GOLD + "☘ " + ChatColor.UNDERLINE + "Helper" + ChatColor.RESET + ChatColor.GOLD + " ☘" + ChatColor.RESET),
        None("");

        String displayText;

        DonationRank(String displayText) {
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    //donate-delete-start
    public static DonationRank getDonationRank(MyPetPlayer player) {
        try {
            // Check whether this player has donated or is a helper for the MyPet project
            // returns
            //   0 for nothing
            //   1 for donator
            //   2 for developer
            //   3 for translator
            //   4 for helper
            //   5 for creator
            // no data will be saved on the server
            String mode;
            if (Bukkit.getOnlineMode()) {
                mode = "userid=" + player.getPlayerUUID();
            } else {
                mode = "username=" + player.getName();
            }
            String donation = Util.readUrlContent("http://donation.keyle.de/donated.php?" + mode);
            if (donation.equals("1")) {
                return DonationRank.Donator;
            } else if (donation.equals("2")) {
                return DonationRank.Developer;
            } else if (donation.equals("3")) {
                return DonationRank.Translator;
            } else if (donation.equals("4")) {
                return DonationRank.Helper;
            } else if (donation.equals("5")) {
                return DonationRank.Creator;
            }
        } catch (Exception e) {
            DebugLogger.info("Can not connect to donation server.");
        }
        return DonationRank.None;
    }
    //donate-delete-end
}