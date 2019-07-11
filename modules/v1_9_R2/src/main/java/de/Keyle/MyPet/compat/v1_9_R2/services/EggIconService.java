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

package de.Keyle.MyPet.compat.v1_9_R2.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.Material;

@Compat("v1_9_R2")
public class EggIconService extends de.Keyle.MyPet.api.util.service.types.EggIconService {
    @Override
    public void updateIcon(MyPetType type, IconMenuItem icon) {
        icon.setMaterial(Material.MONSTER_EGG);
        TagCompound entityTag = new TagCompound();

        switch (type) {
            case Bat:
                entityTag.put("id", new TagString("Bat"));
                break;
            case Blaze:
                entityTag.put("id", new TagString("Blaze"));
                break;
            case CaveSpider:
                entityTag.put("id", new TagString("CaveSpider"));
                break;
            case Chicken:
                entityTag.put("id", new TagString("Chicken"));
                break;
            case Cow:
                entityTag.put("id", new TagString("Cow"));
                break;
            case Creeper:
                entityTag.put("id", new TagString("Creeper"));
                break;
            case EnderDragon:
                entityTag.put("id", new TagString("EnderDragon"));
                break;
            case Enderman:
                entityTag.put("id", new TagString("Enderman"));
                break;
            case Endermite:
                entityTag.put("id", new TagString("Endermite"));
                break;
            case Ghast:
                entityTag.put("id", new TagString("Ghast"));
                break;
            case Giant:
                entityTag.put("id", new TagString("Giant"));
                break;
            case Guardian:
                entityTag.put("id", new TagString("Guardian"));
                break;
            case Horse:
                entityTag.put("id", new TagString("EntityHorse"));
                break;
            case IronGolem:
                entityTag.put("id", new TagString("VillagerGolem"));
                icon.setGlowing(true);
                break;
            case MagmaCube:
                entityTag.put("id", new TagString("LavaSlime"));
                break;
            case Mooshroom:
                entityTag.put("id", new TagString("MushroomCow"));
                break;
            case Ocelot:
                entityTag.put("id", new TagString("Ozelot"));
                break;
            case Pig:
                entityTag.put("id", new TagString("pig"));
                break;
            case PigZombie:
                entityTag.put("id", new TagString("PigZombie"));
                break;
            case PolarBear:
                entityTag.put("id", new TagString("PolarBear"));
                break;
            case Rabbit:
                entityTag.put("id", new TagString("Rabbit"));
                break;
            case Sheep:
                entityTag.put("id", new TagString("Sheep"));
                break;
            case Silverfish:
                entityTag.put("id", new TagString("Silverfish"));
                break;
            case Skeleton:
                entityTag.put("id", new TagString("Skeleton"));
                break;
            case Slime:
                entityTag.put("id", new TagString("Slime"));
                break;
            case Snowman:
                entityTag.put("id", new TagString("Snowman"));
                break;
            case Spider:
                entityTag.put("id", new TagString("Spider"));
                break;
            case Squid:
                entityTag.put("id", new TagString("Squid"));
                break;
            case Witch:
                entityTag.put("id", new TagString("Witch"));
                break;
            case Wither:
                entityTag.put("id", new TagString("WitherBoss"));
                icon.setGlowing(true);
                break;
            case Wolf:
                entityTag.put("id", new TagString("Wolf"));
                break;
            case Villager:
                entityTag.put("id", new TagString("Villager"));
                break;
            case Zombie:
                entityTag.put("id", new TagString("Zombie"));
                break;
        }

        icon.addTag("EntityTag", entityTag);
    }
}