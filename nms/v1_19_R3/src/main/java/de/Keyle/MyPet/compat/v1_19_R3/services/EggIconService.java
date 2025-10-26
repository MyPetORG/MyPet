/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_19_R3.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.Material;

@Compat("v1_19_R3")
public class EggIconService extends de.Keyle.MyPet.api.util.service.types.EggIconService {

    @Override
    public void updateIcon(MyPetType type, IconMenuItem icon) {
        switch (type) {
            case Allay:
                icon.setMaterial(Material.ALLAY_SPAWN_EGG);
                break;
            case Axolotl:
                icon.setMaterial(Material.AXOLOTL_SPAWN_EGG);
                break;
            case Bat:
                icon.setMaterial(Material.BAT_SPAWN_EGG);
                break;
            case Bee:
                icon.setMaterial(Material.BEE_SPAWN_EGG);
                break;
            case Blaze:
                icon.setMaterial(Material.BLAZE_SPAWN_EGG);
                break;
            case CaveSpider:
                icon.setMaterial(Material.CAVE_SPIDER_SPAWN_EGG);
                break;
            case Chicken:
                icon.setMaterial(Material.CHICKEN_SPAWN_EGG);
                break;
            case Cod:
                icon.setMaterial(Material.COD_SPAWN_EGG);
                break;
            case Cow:
                icon.setMaterial(Material.COW_SPAWN_EGG);
                break;
            case PiglinBrute:
                icon.setMaterial(Material.PIGLIN_BRUTE_SPAWN_EGG);
                break;
            case Creeper:
                icon.setMaterial(Material.CREEPER_SPAWN_EGG);
                break;
            case Dolphin:
                icon.setMaterial(Material.DOLPHIN_SPAWN_EGG);
                break;
            case Drowned:
                icon.setMaterial(Material.DROWNED_SPAWN_EGG);
                break;
            case EnderDragon:
                icon.setMaterial(Material.DRAGON_EGG);
                break;
            case Enderman:
                icon.setMaterial(Material.ENDERMAN_SPAWN_EGG);
                break;
            case Endermite:
                icon.setMaterial(Material.ENDERMITE_SPAWN_EGG);
                break;
            case Evoker:
                icon.setMaterial(Material.EVOKER_SPAWN_EGG);
                break;
            case Ghast:
                icon.setMaterial(Material.GHAST_SPAWN_EGG);
                break;
            case Frog:
                icon.setMaterial(Material.FROG_SPAWN_EGG);
                break;
            case Giant:
                icon.setMaterial(Material.ZOMBIE_SPAWN_EGG);
                break;
            case GlowSquid:
				icon.setMaterial(Material.GLOW_SQUID_SPAWN_EGG);
				break;
            case Goat:
                icon.setMaterial(Material.GOAT_SPAWN_EGG);
                break;
            case Guardian:
                icon.setMaterial(Material.GUARDIAN_SPAWN_EGG);
                break;
            case ElderGuardian:
                icon.setMaterial(Material.ELDER_GUARDIAN_SPAWN_EGG);
                break;
            case Horse:
                icon.setMaterial(Material.HORSE_SPAWN_EGG);
                break;
            case Husk:
                icon.setMaterial(Material.HUSK_SPAWN_EGG);
                break;
            case Donkey:
                icon.setMaterial(Material.DONKEY_SPAWN_EGG);
                break;
            case Mule:
                icon.setMaterial(Material.MULE_SPAWN_EGG);
                break;
            case SkeletonHorse:
                icon.setMaterial(Material.SKELETON_HORSE_SPAWN_EGG);
                break;
            case ZombieHorse:
                icon.setMaterial(Material.ZOMBIE_HORSE_SPAWN_EGG);
                break;
            case Illusioner:
                icon.setMaterial(Material.SQUID_SPAWN_EGG);
                icon.setGlowing(true);
                break;
            case IronGolem:
                icon.setMaterial(Material.SKELETON_SPAWN_EGG);
                icon.setGlowing(true);
                break;
            case Llama:
                icon.setMaterial(Material.LLAMA_SPAWN_EGG);
                break;
            case MagmaCube:
                icon.setMaterial(Material.MAGMA_CUBE_SPAWN_EGG);
                break;
            case Mooshroom:
                icon.setMaterial(Material.MOOSHROOM_SPAWN_EGG);
                break;
            case Ocelot:
                icon.setMaterial(Material.OCELOT_SPAWN_EGG);
                break;
            case Parrot:
                icon.setMaterial(Material.PARROT_SPAWN_EGG);
                break;
            case Phantom:
                icon.setMaterial(Material.PHANTOM_SPAWN_EGG);
                break;
            case Pig:
                icon.setMaterial(Material.PIG_SPAWN_EGG);
                break;
            case PolarBear:
                icon.setMaterial(Material.POLAR_BEAR_SPAWN_EGG);
                break;
            case Pufferfish:
                icon.setMaterial(Material.PUFFERFISH_SPAWN_EGG);
                break;
            case Rabbit:
                icon.setMaterial(Material.RABBIT_SPAWN_EGG);
                break;
            case Sheep:
                icon.setMaterial(Material.SHEEP_SPAWN_EGG);
                break;
            case Salmon:
                icon.setMaterial(Material.SALMON_SPAWN_EGG);
                break;
            case Silverfish:
                icon.setMaterial(Material.SILVERFISH_SPAWN_EGG);
                break;
            case Skeleton:
                icon.setMaterial(Material.SKELETON_SPAWN_EGG);
                break;
            case Stray:
                icon.setMaterial(Material.STRAY_SPAWN_EGG);
                break;
            case TropicalFish:
                icon.setMaterial(Material.TROPICAL_FISH_SPAWN_EGG);
                break;
            case Turtle:
                icon.setMaterial(Material.TURTLE_EGG);
                break;
            case WitherSkeleton:
                icon.setMaterial(Material.WITHER_SKELETON_SPAWN_EGG);
                break;
            case Slime:
                icon.setMaterial(Material.SLIME_SPAWN_EGG);
                break;
            case Snowman:
                icon.setMaterial(Material.PUMPKIN);
                break;
            case Spider:
                icon.setMaterial(Material.SPIDER_SPAWN_EGG);
                break;
            case Squid:
                icon.setMaterial(Material.SQUID_SPAWN_EGG);
                break;
            case Tadpole:
                icon.setMaterial(Material.TADPOLE_SPAWN_EGG);
                break;
            case Warden:
                icon.setMaterial(Material.WARDEN_SPAWN_EGG);
                icon.setGlowing(true);
                break;
            case Witch:
                icon.setMaterial(Material.WITCH_SPAWN_EGG);
                break;
            case Wither:
                icon.setMaterial(Material.ENDERMITE_SPAWN_EGG);
                icon.setGlowing(true);
                break;
            case Wolf:
                icon.setMaterial(Material.WOLF_SPAWN_EGG);
                break;
            case Vex:
                icon.setMaterial(Material.VEX_SPAWN_EGG);
                break;
            case Villager:
                icon.setMaterial(Material.VILLAGER_SPAWN_EGG);
                break;
            case Vindicator:
                icon.setMaterial(Material.VINDICATOR_SPAWN_EGG);
                break;
            case Zombie:
                icon.setMaterial(Material.ZOMBIE_SPAWN_EGG);
                break;
            case ZombieVillager:
                icon.setMaterial(Material.ZOMBIE_VILLAGER_SPAWN_EGG);
                break;
            case Cat:
                icon.setMaterial(Material.CAT_SPAWN_EGG);
                break;
            case Fox:
                icon.setMaterial(Material.FOX_SPAWN_EGG);
                break;
            case Panda:
                icon.setMaterial(Material.PANDA_SPAWN_EGG);
                break;
            case Pillager:
                icon.setMaterial(Material.PILLAGER_SPAWN_EGG);
                break;
            case Ravager:
                icon.setMaterial(Material.RAVAGER_SPAWN_EGG);
                break;
            case WanderingTrader:
                icon.setMaterial(Material.WANDERING_TRADER_SPAWN_EGG);
                break;
            case TraderLlama:
                icon.setMaterial(Material.TRADER_LLAMA_SPAWN_EGG);
                break;
            case Zoglin:
                icon.setMaterial(Material.ZOGLIN_SPAWN_EGG);
                break;
            case Piglin:
                icon.setMaterial(Material.PIGLIN_SPAWN_EGG);
                break;
            case Hoglin:
                icon.setMaterial(Material.HOGLIN_SPAWN_EGG);
                break;
            case ZombifiedPiglin:
                icon.setMaterial(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG);
                break;
            case Strider:
                icon.setMaterial(Material.STRIDER_SPAWN_EGG);
                break;
        }
    }
}
