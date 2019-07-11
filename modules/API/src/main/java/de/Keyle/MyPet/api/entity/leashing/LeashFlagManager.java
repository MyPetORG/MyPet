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

package de.Keyle.MyPet.api.entity.leashing;

import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;

import java.util.HashMap;
import java.util.Map;

@ServiceName("LeashFlagManager")
@Load(Load.State.OnLoad)
public class LeashFlagManager implements ServiceContainer {

    Map<String, LeashFlag> leashFlags = new HashMap<>();

    public void onDisable() {
        leashFlags.clear();
    }

    public void registerLeashFlag(LeashFlag leashFlag) {
        String flagName = getLeashFlagName(leashFlag.getClass());
        leashFlags.put(flagName.toLowerCase(), leashFlag);
    }

    public LeashFlag getLeashFlag(String flagName) {
        return leashFlags.get(flagName.toLowerCase());
    }

    public String getLeashFlagName(Class clazz) {
        if (clazz == Object.class) {
            return null;
        }
        if (LeashFlag.class.isAssignableFrom(clazz)) {
            LeashFlagName flagName = (LeashFlagName) clazz.getAnnotation(LeashFlagName.class);
            if (flagName != null) {
                return flagName.value();
            }
        }
        String flagName = getLeashFlagName(clazz.getSuperclass());
        if (flagName != null) {
            return flagName;
        }
        for (Class c : clazz.getInterfaces()) {
            flagName = getLeashFlagName(c);
            if (flagName != null) {
                return flagName;
            }
        }
        return null;
    }

    public void removeFlag(String flagName) {
        leashFlags.remove(flagName);
    }

    public void removeFlag(LeashFlag flag) {
        String flagName = getLeashFlagName(flag.getClass());
        removeFlag(flagName);
    }
}
