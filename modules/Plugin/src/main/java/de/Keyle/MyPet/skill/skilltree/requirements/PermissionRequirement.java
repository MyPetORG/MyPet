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

package de.Keyle.MyPet.skill.skilltree.requirements;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;
import de.Keyle.MyPet.api.skill.skilltree.requirements.RequirementName;
import de.Keyle.MyPet.api.util.configuration.settings.Setting;
import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import org.bukkit.entity.Player;

@RequirementName("Permission")
public class PermissionRequirement implements Requirement {

    @Override
    public boolean check(Skilltree skilltree, MyPet pet, Settings settings) {
        Player player = pet.getOwner().getPlayer();
        String permission = "MyPet.skilltree." + skilltree.getName();
        for (Setting setting : settings.all()) {
            if (setting.getValue() != null && !setting.getValue().isEmpty()) {
                permission = "MyPet.skilltree." + setting.getValue();
            }
        }
        return Permissions.has(player, permission);
    }
}
