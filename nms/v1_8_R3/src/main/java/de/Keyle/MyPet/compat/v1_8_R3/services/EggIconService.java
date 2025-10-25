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

package de.Keyle.MyPet.compat.v1_8_R3.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.Material;

@Compat("v1_8_R3")
public class EggIconService extends de.Keyle.MyPet.api.util.service.types.EggIconService {
    @Override
    public void updateIcon(MyPetType type, IconMenuItem icon) {
        icon.setMaterial(Material.MONSTER_EGG);

        switch (type) {
            case Bat:
                icon.setData(65);
                break;
            case Blaze:
                icon.setData(61);
                break;
            case CaveSpider:
                icon.setData(59);
                break;
            case Chicken:
                icon.setData(93);
                break;
            case Cow:
                icon.setData(92);
                break;
            case Creeper:
                icon.setData(50);
                break;
            case EnderDragon:
                icon.setData(59);
                break;
            case Enderman:
                icon.setData(58);
                break;
            case Endermite:
                icon.setData(67);
            case Ghast:
                icon.setData(56);
                break;
            case Giant:
                icon.setData(54);
                break;
            case Guardian:
                icon.setData(68);
                break;
            case Horse:
                icon.setData(100);
                break;
            case IronGolem:
                icon.setData(60);
                icon.setGlowing(true);
                break;
            case MagmaCube:
                icon.setData(62);
                break;
            case Mooshroom:
                icon.setData(96);
                break;
            case Ocelot:
                icon.setData(98);
                break;
            case Pig:
                icon.setData(90);
                break;
            case PigZombie:
                icon.setData(57);
                break;
            case Rabbit:
                icon.setData(101);
                break;
            case Sheep:
                icon.setData(91);
                break;
            case Silverfish:
                icon.setData(60);
                break;
            case Skeleton:
                icon.setData(51);
                break;
            case Slime:
                icon.setData(55);
                break;
            case Snowman:
                icon.setData(97);
                break;
            case Spider:
                icon.setData(52);
                break;
            case Squid:
                icon.setData(94);
                break;
            case Witch:
                icon.setData(66);
                break;
            case Wither:
                icon.setData(58);
                icon.setGlowing(true);
                break;
            case Wolf:
                icon.setData(95);
                break;
            case Villager:
                icon.setData(120);
                break;
            case Zombie:
                icon.setData(54);
                break;
        }
    }
}