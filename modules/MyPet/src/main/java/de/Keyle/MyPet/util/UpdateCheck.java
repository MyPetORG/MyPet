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

package de.Keyle.MyPet.util;

import com.google.common.base.Optional;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.Util;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UpdateCheck {
    public static Optional<String> checkForUpdate(String plugin) {
        try {
            String parameter = "";
            parameter += "plugin=" + plugin;
            parameter += "&package=" + MyPetApi.getCompatUtil().getInternalVersion();
            parameter += "&build=" + MyPetVersion.getBuild();

            // no data will be saved on the server
            String content = Util.readUrlContent("http://update.mypet-plugin.de/check.php?" + parameter);
            JSONParser parser = new JSONParser();
            JSONObject result = (JSONObject) parser.parse(content);

            if (result.containsKey("latest")) {
                return Optional.of(result.get("latest").toString());
            }
        } catch (Exception ignored) {
        }
        return Optional.absent();
    }
}