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

package de.Keyle.MyPet.api.compat;

public class SoundCompat {

    public static Compat<String> ENDERMAN_TELEPORT = new Compat<String>()
            .d("mob.endermen.portal")
            .v("1.9", "entity.endermen.teleport")
            .v("1.13", "entity.enderman.teleport")
            .search();

    public static Compat<String> ITEM_PICKUP = new Compat<String>()
            .d("random.pop")
            .v("1.9", "entity.item.pickup")
            .search();

    public static Compat<String> THORNS_HIT = new Compat<String>()
            .d("damage.thorns")
            .v("1.9", "enchant.thorns.hit")
            .search();

    public static Compat<String> LEVEL_UP = new Compat<String>()
            .d("LEVEL_UP")
            .v("1.9", "ENTITY_PLAYER_LEVELUP")
            .search();

    public static Compat<String> FALL_BIG = new Compat<String>()
            .d("FALL_BIG")
            .v("1.9", "ENTITY_HOSTILE_BIG_FALL")
            .search();

    public static Compat<String> LEVEL_DOWN = new Compat<String>()
            .d("ANVIL_BREAK")
            .v("1.9", "ENTITY_WITHER_BREAK_BLOCK")
            .search();
}
