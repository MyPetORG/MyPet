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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.util.hooks.PluginHook;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.hooks.NCPHook;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@PluginHookName("NoCheatPlus")
public class NoCheatPlusHook implements PluginHook {

    List<Integer> hookIds = new ArrayList<>();

    @Override
    public boolean onEnable() {
        NCPHook hook = new NCPHook() {
            @Override
            public String getHookName() {
                return "MyPet";
            }

            @Override
            public String getHookVersion() {
                return "1.1";
            }

            @Override
            public boolean onCheckFailure(CheckType checkType, Player player, IViolationInfo iViolationInfo) {
                switch (checkType.name()) {
                    case "MOVING_VEHICLE_ENVELOPE":
                    case "MOVING_MOREPACKETS":
                    case "MOVING_VEHICLE_MOREPACKETS":
                        if (player.isInsideVehicle()) {
                            return player.getVehicle() instanceof MyPetBukkitEntity;
                        }
                }
                return false;
            }
        };

        try {
            CheckType.valueOf("MOVING_VEHICLE_ENVELOPE");
            hookIds.add(NCPHookManager.addHook(CheckType.MOVING_VEHICLE_ENVELOPE, hook));
        } catch (IllegalArgumentException e) {
            return false;
        }

        try {
            CheckType.valueOf("MOVING_VEHICLE_ENVELOPE");
            hookIds.add(NCPHookManager.addHook(CheckType.MOVING_VEHICLE_MOREPACKETS, hook));
        } catch (IllegalArgumentException e) {
            return false;
        }

        try {
            CheckType.valueOf("MOVING_MOREPACKETS");
            hookIds.add(NCPHookManager.addHook(CheckType.MOVING_MOREPACKETS, hook));
        } catch (IllegalArgumentException e) {
            return false;
        }

        return hookIds.size() > 0;
    }

    @Override
    public void onDisable() {
        hookIds.forEach(NCPHookManager::removeHook);
    }
}