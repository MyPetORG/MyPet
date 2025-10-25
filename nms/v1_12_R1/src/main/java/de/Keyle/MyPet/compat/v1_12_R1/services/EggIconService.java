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

package de.Keyle.MyPet.compat.v1_12_R1.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagString;
import org.bukkit.Material;

@Compat("v1_12_R1")
public class EggIconService extends de.Keyle.MyPet.api.util.service.types.EggIconService {
    @Override
    public void updateIcon(MyPetType type, IconMenuItem icon) {
        icon.setMaterial(Material.MONSTER_EGG);
        TagCompound entityTag = new TagCompound();

        switch (type) {
            case Bat:
                entityTag.put("id", new TagString("minecraft:bat"));
                break;
            case Blaze:
                entityTag.put("id", new TagString("minecraft:blaze"));
                break;
            case CaveSpider:
                entityTag.put("id", new TagString("minecraft:cave_spider"));
                break;
            case Chicken:
                entityTag.put("id", new TagString("minecraft:chicken"));
                break;
            case Cow:
                entityTag.put("id", new TagString("minecraft:cow"));
                break;
            case Creeper:
                entityTag.put("id", new TagString("minecraft:creeper"));
                break;
            case EnderDragon:
                entityTag.put("id", new TagString("minecraft:ender_dragon"));
                break;
            case Enderman:
                entityTag.put("id", new TagString("minecraft:enderman"));
                break;
            case Endermite:
                entityTag.put("id", new TagString("minecraft:endermite"));
                break;
            case Evoker:
                entityTag.put("id", new TagString("minecraft:evocation_illager"));
                break;
            case Ghast:
                entityTag.put("id", new TagString("minecraft:ghast"));
                break;
            case Giant:
                entityTag.put("id", new TagString("minecraft:giant"));
                break;
            case Guardian:
                entityTag.put("id", new TagString("minecraft:guardian"));
                break;
            case ElderGuardian:
                entityTag.put("id", new TagString("minecraft:elder_guardian"));
                break;
            case Horse:
                entityTag.put("id", new TagString("minecraft:horse"));
                break;
            case Donkey:
                entityTag.put("id", new TagString("minecraft:donkey"));
                break;
            case Mule:
                entityTag.put("id", new TagString("minecraft:mule"));
                break;
            case SkeletonHorse:
                entityTag.put("id", new TagString("minecraft:skeleton_horse"));
                break;
            case ZombieHorse:
                entityTag.put("id", new TagString("minecraft:zombie_horse"));
                break;
            case Illusioner:
                entityTag.put("id", new TagString("minecraft:squid"));
                icon.setGlowing(true);
                break;
            case IronGolem:
                entityTag.put("id", new TagString("minecraft:skeleton"));
                icon.setGlowing(true);
                break;
            case Llama:
                entityTag.put("id", new TagString("minecraft:llama"));
                break;
            case MagmaCube:
                entityTag.put("id", new TagString("minecraft:magma_cube"));
                break;
            case Mooshroom:
                entityTag.put("id", new TagString("minecraft:mooshroom"));
                break;
            case Ocelot:
                entityTag.put("id", new TagString("minecraft:ocelot"));
                break;
            case Parrot:
                entityTag.put("id", new TagString("minecraft:parrot"));
                break;
            case Pig:
                entityTag.put("id", new TagString("minecraft:pig"));
                break;
            case PigZombie:
                entityTag.put("id", new TagString("minecraft:zombie_pigman"));
                break;
            case PolarBear:
                entityTag.put("id", new TagString("minecraft:polar_bear"));
                break;
            case Rabbit:
                entityTag.put("id", new TagString("minecraft:rabbit"));
                break;
            case Sheep:
                entityTag.put("id", new TagString("minecraft:sheep"));
                break;
            case Silverfish:
                entityTag.put("id", new TagString("minecraft:silverfish"));
                break;
            case Skeleton:
                entityTag.put("id", new TagString("minecraft:skeleton"));
                break;
            case Stray:
                entityTag.put("id", new TagString("minecraft:stray"));
                break;
            case WitherSkeleton:
                entityTag.put("id", new TagString("minecraft:wither_skeleton"));
                break;
            case Slime:
                entityTag.put("id", new TagString("minecraft:slime"));
                break;
            case Snowman:
                entityTag.put("id", new TagString("minecraft:snowman"));
                break;
            case Spider:
                entityTag.put("id", new TagString("minecraft:spider"));
                break;
            case Squid:
                entityTag.put("id", new TagString("minecraft:squid"));
                break;
            case Witch:
                entityTag.put("id", new TagString("minecraft:witch"));
                break;
            case Wither:
                entityTag.put("id", new TagString("minecraft:endermite"));
                icon.setGlowing(true);
                break;
            case Wolf:
                entityTag.put("id", new TagString("minecraft:wolf"));
                break;
            case Vex:
                entityTag.put("id", new TagString("minecraft:vex"));
                break;
            case Villager:
                entityTag.put("id", new TagString("minecraft:villager"));
                break;
            case Vindicator:
                entityTag.put("id", new TagString("minecraft:vindication_illager"));
                break;
            case Zombie:
                entityTag.put("id", new TagString("minecraft:zombie"));
                break;
            case ZombieVillager:
                entityTag.put("id", new TagString("minecraft:zombie_villager"));
                break;
            case Husk:
                entityTag.put("id", new TagString("minecraft:husk"));
                break;
        }

        icon.addTag("EntityTag", entityTag);
    }
}