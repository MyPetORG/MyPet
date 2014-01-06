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

package de.Keyle.MyPet.util;


import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.LeashFlag;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.entity.*;

import java.util.List;

public class CaptureHelper {
    public static boolean checkTamable(LivingEntity leashTarget, double damage, Player attacker) {
        double newHealth = leashTarget.getHealth() - damage;

        boolean tameNow = true;
        List<LeashFlag> leashFlags = MyPet.getLeashFlags(MyPetType.getMyPetTypeByEntityType(leashTarget.getType()).getMyPetClass());

        flagloop:
        for (LeashFlag flag : leashFlags) {
            switch (flag) {
                case Impossible:
                    tameNow = false;
                    attacker.sendMessage(Locales.getString("Message.NotLeashable", attacker));
                    break flagloop;
                case Adult:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).isAdult()) {
                        tameNow = false;
                        attacker.sendMessage(Locales.getString("Message.NotAdult", attacker));
                    }
                    break;
                case Baby:
                    if (leashTarget instanceof Ageable && ((Ageable) leashTarget).isAdult()) {
                        tameNow = false;
                        attacker.sendMessage(Locales.getString("Message.NotBaby", attacker));
                    }
                    break;
                case LowHp:
                    if (newHealth > 2) {
                        tameNow = false;
                    }
                    if (newHealth <= leashTarget.getMaxHealth() && newHealth > 2) {
                        attacker.sendMessage(String.format("%1.2f", newHealth) + "/" + String.format("%1.2f", leashTarget.getMaxHealth()) + " " + Locales.getString("Name.HP", attacker));
                    }
                    break;
                case Angry:
                    if (leashTarget instanceof Wolf && !((Wolf) leashTarget).isAngry()) {
                        attacker.sendMessage(Locales.getString("Message.NotAngry", attacker));
                    }
                    break;
                case CanBreed:
                    if (leashTarget instanceof Ageable && !((Ageable) leashTarget).canBreed()) {
                        attacker.sendMessage(Locales.getString("Message.CanNotBreed", attacker));
                    }
                    break;
                case None:
                    tameNow = false;
                    break flagloop;
                case Tamed:
                    if (leashTarget instanceof Tameable && !((Tameable) leashTarget).isTamed()) {
                        tameNow = false;
                        attacker.sendMessage(Locales.getString("Message.CaptureHelper.NotTamed", attacker));
                    }
                    break;
                case UserCreated:
                    if (leashTarget instanceof IronGolem && !((IronGolem) leashTarget).isPlayerCreated()) {
                        tameNow = false;
                        attacker.sendMessage(Locales.getString("Message.CaptureHelper.NotUserCreated", attacker));
                    }
                    break;
                case Wild:
                    if (leashTarget instanceof IronGolem && ((IronGolem) leashTarget).isPlayerCreated()) {
                        tameNow = false;
                        attacker.sendMessage(Locales.getString("Message.NotWild", attacker));
                    } else if (leashTarget instanceof Tameable && ((Tameable) leashTarget).isTamed()) {
                        tameNow = false;
                        attacker.sendMessage(Locales.getString("Message.NotWild", attacker));
                    }
            }
        }
        if (tameNow) {
            attacker.sendMessage(Locales.getString("Message.CaptureHelper.TameNow", attacker));
        }
        return tameNow;
    }
}