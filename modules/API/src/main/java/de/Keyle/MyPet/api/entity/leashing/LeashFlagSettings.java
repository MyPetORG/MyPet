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

package de.Keyle.MyPet.api.entity.leashing;

import java.util.*;

public class LeashFlagSettings {
    String flagName;
    Map<String, LeashFlagSetting> settingsMap = new HashMap<>();
    Set<LeashFlagSetting> settings = new HashSet<>();

    public LeashFlagSettings(String flagName) {
        this.flagName = flagName;
    }

    public void load(String settingsString) {
        String[] settingStrings = settingsString.split(":");
        for (String settingString : settingStrings) {
            if (settingString.contains("=")) {
                String[] keyValue = settingString.split("=", 1);
                LeashFlagSetting setting = new LeashFlagSetting(keyValue[0], keyValue[1]);
                this.settingsMap.put(setting.key.toLowerCase(), setting);
                settings.add(setting);
            } else {
                LeashFlagSetting setting = new LeashFlagSetting(settingString);
                settings.add(setting);
            }
        }
    }

    public String getFlagName() {
        return flagName;
    }

    public Map<String, LeashFlagSetting> map() {
        return Collections.unmodifiableMap(settingsMap);
    }

    public Set<LeashFlagSetting> all() {
        return Collections.unmodifiableSet(settings);
    }
}
