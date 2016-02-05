/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import org.bukkit.entity.*;

import java.util.List;

public class CaptureHelper {
    public static boolean checkTamable(LivingEntity leashTarget) {
        List<LeashFlag> leashFlags = MyPet.getLeashFlags(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass());

        boolean tamable = true;
        for (LeashFlag flag : leashFlags) {
            switch (flag) {
                case Impossible:
                    return false;
                case None:
                    return true;
                case Adult:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).isAdult()) {
                        tamable = false;
                    }
                    break;
                case Baby:
                    if (leashTarget instanceof Ageable && ((Ageable) leashTarget).isAdult()) {
                        tamable = false;
                    }
                    break;
                case LowHp:
                    if (((leashTarget.getHealth() - 1) * 100) / leashTarget.getMaxHealth() > 10) {
                        tamable = false;
                    }
                    break;
                case Angry:
                    if (leashTarget instanceof Wolf && !((Wolf) leashTarget).isAngry()) {
                        tamable = false;
                    }
                    break;
                case CanBreed:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).canBreed()) {
                        tamable = false;
                    }
                    break;
                case Tamed:
                    if (leashTarget instanceof Tameable && !((Tameable) leashTarget).isTamed()) {
                        tamable = false;
                    }
                    break;
                case UserCreated:
                    if (leashTarget instanceof IronGolem && !((IronGolem) leashTarget).isPlayerCreated()) {
                        tamable = false;
                    }
                    break;
                case Wild:
                    if (leashTarget instanceof IronGolem && ((IronGolem) leashTarget).isPlayerCreated()) {
                        tamable = false;
                    } else if (leashTarget instanceof Tameable && ((Tameable) leashTarget).isTamed()) {
                        tamable = false;
                    }
            }
        }
        return tamable;
    }
}