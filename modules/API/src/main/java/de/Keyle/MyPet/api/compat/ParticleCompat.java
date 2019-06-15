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

public class ParticleCompat {
    // https://mcreator.net/wiki/particles-ids

    public static Compat<String> VILLAGER_HAPPY = new Compat<String>()
            .d("happyVillager")
            .v("1.8", "VILLAGER_HAPPY")
            .v("1.13", "happy_villager")
            .search();

    public static Compat<String> VILLAGER_ANGRY = new Compat<String>()
            .d("angryVillager")
            .v("1.8", "VILLAGER_ANGRY")
            .v("1.13", "angry_villager")
            .search();

    public static Compat<String> BARRIER = new Compat<String>()
            .d("spell")
            .v("1.8", "BARRIER")
            .v("1.13", "barrier")
            .search();

    public static Compat<String> ITEM_CRACK = new Compat<String>()
            .d("iconcrack_")
            .v("1.8", "ITEM_CRACK")
            .v("1.13", "item")
            .search();

    public static Compat<String> BLOCK_CRACK = new Compat<String>()
            .d("blockcrack")
            .v("1.8", "BLOCK_CRACK")
            .v("1.13", "block")
            .search();

    public static Compat<String> SPELL_WITCH = new Compat<String>()
            .d("spell")
            .v("1.8", "SPELL_WITCH")
            .v("1.13", "witch")
            .search();

    public static Compat<String> SPELL_INSTANT = new Compat<String>()
            .d("instantSpell")
            .v("1.8", "SPELL_INSTANT")
            .v("1.13", "instant_effect")
            .search();

    public static Compat<String> CRIT_MAGIC = new Compat<String>()
            .d("magicCrit")
            .v("1.8", "CRIT_MAGIC")
            .v("1.13", "enchanted_hit")
            .search();

    public static Compat<String> CRIT = new Compat<String>()
            .d("crit")
            .v("1.8", "CRIT")
            .v("1.13", "crit")
            .search();

    public static Compat<String> HEART = new Compat<String>()
            .d("heart")
            .v("1.8", "HEART")
            .v("1.13", "heart")
            .search();

    public static Compat<String> WATER_SPLASH = new Compat<String>()
            .d("splash")
            .v("1.8", "WATER_SPLASH")
            .v("1.13", "splash")
            .search();

    public static Compat<String> SMOKE_LARGE = new Compat<String>()
            .d("largesmoke")
            .v("1.8", "SMOKE_LARGE")
            .v("1.13", "large_smoke")
            .search();

    /*
            Block Data
     */

    public static Compat<Object> RED_WOOL_DATA = new Compat<>()
            .d(4447) // (351+(1*4096))
            .v("1.8", new int[]{351, 1})
            .v("1.13", "rose_red")
            .v("1.14", "red_dye")
            .search();

    public static Compat<Object> LIME_GREEN_WOOL_DATA = new Compat<>()
            .d(41311) // (351+(10*4096))
            .v("1.8", new int[]{351, 10})
            .v("1.13", "lime_dye")
            .search();

    public static Compat<Object> REDSTONE_BLOCK_DATA = new Compat<>()
            .d(152)
            .v("1.8", new int[]{152})
            .v("1.13", "redstone_block")
            .search();
}
