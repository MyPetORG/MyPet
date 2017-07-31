/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

package de.Keyle.MyPet.api.util;

public enum ResourcePackIcons {
    // http://unicode-table.com/en/#A800

    // Logo -----------------------
    Logo("\uA817"),
    // Titles ---------------------
    Title_Creator("\uA817"),
    Title_Translator("\uA818"),
    Title_Premium("\uA819"),
    Title_Donator("\uA81A"),
    Title_Developer("\uA81B"),
    Title_Helper("\uA81C"),
    // Skills ---------------------
    Skill_Fire("\uA800"),
    Skill_Damage("\uA801"),
    Skill_Behavior("\uA803"),
    Skill_Beacon("\uA804"),
    Skill_Wither("\uA805"),
    Skill_Sprint("\uA807"),
    Skill_Stomp("\uA808"),
    Skill_Slow("\uA809"),
    Skill_Shield("\uA80A"),
    Skill_Poison("\uA80C"),
    Skill_Pickup("\uA80D"),
    Skill_Lightning("\uA80E"),
    Skill_Knockback("\uA80F"),
    Skill_Inventory("\uA810"),
    Skill_Ranged("\uA811"),
    Skill_HPregeneration("\uA812"),
    Skill_HP("\uA813"),
    Skill_Control("\uA814"),
    Skill_Thorns("\uA815"),
    Skill_Ride("\uA816");

    final String code;

    ResourcePackIcons(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}