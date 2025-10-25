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

package de.Keyle.MyPet.api.player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DonateCheck {
    private static Map<String, Character> donMap = new HashMap<>();

    public enum DonationRank {
        Creator("☣"),
        Donator("❤"),
        Translator("✈"),
        Developer("✪"),
        Helper("☘"),
        Premium("$"),
        None("");

        String defaultIcon;

        DonationRank(String defaultIcon) {
            this.defaultIcon = defaultIcon;
        }

        public String getDefaultIcon() {
            return defaultIcon;
        }
    }

    private static void fillDonMap() {
        BufferedReader donation = null;
        int timeout = 2000;
        try {
            URL url = new URL("https://raw.githubusercontent.com/MyPetORG/MyPet/particles/particles.csv");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setConnectTimeout(timeout);
            huc.setReadTimeout(timeout);
            huc.setRequestMethod("GET");
            huc.connect();
            donation = new BufferedReader(new InputStreamReader(huc.getInputStream()));

            String line;
            while ((line = donation.readLine()) != null) {
                donMap.put(line.substring(0,line.length()-1), line.charAt(line.length() - 1));
            }
        } catch(Exception ignored) {
        }
    }

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
            //   6 for premium
            // no data will be saved on the server
            String check;
            if (player.getMojangUUID() != null) {
                check = player.getName()+","+player.getMojangUUID()+",";
            } else {
                check = player.getName()+",,";
            }

            if(donMap.isEmpty()) {
                fillDonMap();
            }

            Character donCheck = '0';
            if(donMap.containsKey(check)) {
                donCheck = donMap.get(check);
            }
            switch (donCheck) {
                case '1':
                    return DonationRank.Donator;
                case '2':
                    return DonationRank.Developer;
                case '3':
                    return DonationRank.Translator;
                case '4':
                    return DonationRank.Helper;
                case '5':
                    return DonationRank.Creator;
                case '6':
                    return DonationRank.Premium;
            }
        } catch (Exception ignored) {
        } finally {
            return DonationRank.None;
        }
    }
}