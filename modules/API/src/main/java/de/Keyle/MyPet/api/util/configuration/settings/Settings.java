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

package de.Keyle.MyPet.api.util.configuration.settings;

import java.util.*;

public class Settings {

    String name;
    Map<String, Setting> settingsMap = new HashMap<>();
    Set<Setting> settings = new HashSet<>();

    public Settings(String flagName) {
        this.name = flagName;
    }

    public void load(String settingsString) {
        String[] settingStrings = settingsString.split(":");
        for (String settingString : settingStrings) {
            if (settingString.contains("=")) {
                String[] keyValue = settingString.split("=", 2);
                Setting setting = new Setting(keyValue[0], keyValue[1]);
                this.settingsMap.put(setting.key.toLowerCase(), setting);
                settings.add(setting);
            } else {
                Setting setting = new Setting(settingString);
                settings.add(setting);
            }
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, Setting> map() {
        return Collections.unmodifiableMap(settingsMap);
    }

    public Set<Setting> all() {
        return Collections.unmodifiableSet(settings);
    }

    @Override
    public String toString() {
        return "Settings{" +
                "name='" + name + '\'' +
                ", settings=" + settings +
                '}';
    }
}
