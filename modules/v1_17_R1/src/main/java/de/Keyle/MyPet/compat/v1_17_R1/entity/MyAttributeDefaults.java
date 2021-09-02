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

package de.Keyle.MyPet.compat.v1_17_R1.entity;

import java.util.HashMap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;

public class MyAttributeDefaults {

    private static final FastMap<EntityType<? extends LivingEntity>, AttributeSupplier> defaultAttribute = new FastMap<>();

    static {
        defaultAttribute
                .putFast(EntityType.ARMOR_STAND, ArmorStand.createLivingAttributes().build()) 				//Armor-Stand
                .putFast(EntityType.AXOLOTL, Axolotl.createAttributes().build()) 							//Axolotl
                .putFast(EntityType.BAT, Bat.createAttributes().build())									//Bat
                .putFast(EntityType.BEE, Bee.createAttributes().build())									//Bee
                .putFast(EntityType.BLAZE, Blaze.createAttributes().build())								//Blaze
                .putFast(EntityType.CAT, Cat.createAttributes().build())									//Cat
                .putFast(EntityType.CAVE_SPIDER, CaveSpider.createAttributes().build())						//CaveSpider
                .putFast(EntityType.CHICKEN, Chicken.createAttributes().build())							//Chicken
                .putFast(EntityType.COD, Cod.createAttributes().build())									//Cod
                .putFast(EntityType.COW, Cow.createAttributes().build())									//Cow
                .putFast(EntityType.CREEPER, Creeper.createAttributes().build())							//Creeper
                .putFast(EntityType.DOLPHIN, Dolphin.createAttributes().build())							//Dolphin
                .putFast(EntityType.DONKEY, Donkey.createBaseChestedHorseAttributes().build())				//Donkey
                .putFast(EntityType.ZOMBIE, Zombie.createAttributes().build())								//Drowned
                .putFast(EntityType.ELDER_GUARDIAN, ElderGuardian.createAttributes().build())				//Elder Guardian
                .putFast(EntityType.ENDERMAN, EnderMan.createAttributes().build())							//Enderman
                .putFast(EntityType.ENDERMITE, Endermite.createAttributes().build())						//Endermite
                .putFast(EntityType.ENDER_DRAGON, EnderDragon.createAttributes().build())					//Ender Dragon
                .putFast(EntityType.EVOKER, Evoker.createAttributes().build())								//Evoker
                .putFast(EntityType.FOX, Fox.createAttributes().build())									//Fox
                .putFast(EntityType.GHAST, Ghast.createAttributes().build())								//Ghast
                .putFast(EntityType.GIANT, Giant.createAttributes().build())								//Giant
                .putFast(EntityType.GUARDIAN, Guardian.createAttributes().build())							//Guardian
                .putFast(EntityType.GOAT, Goat.createAttributes().build())									//Goat
                .putFast(EntityType.GLOW_SQUID, GlowSquid.createAttributes().build())						//GlowSquid
                .putFast(EntityType.HOGLIN, Hoglin.createAttributes().build())								//Hoglin
                .putFast(EntityType.HORSE, Horse.createBaseHorseAttributes().build())						//Horse
                .putFast(EntityType.HUSK, Husk.createAttributes().build())									//Husk
                .putFast(EntityType.ILLUSIONER, Illusioner.createAttributes().build())						//Illusioner
                .putFast(EntityType.IRON_GOLEM, IronGolem.createAttributes().build())						//Iron Golem
                .putFast(EntityType.LLAMA, Llama.createAttributes().build())								//Llama
                .putFast(EntityType.MAGMA_CUBE, MagmaCube.createAttributes().build())						//Magma Cube
                .putFast(EntityType.MOOSHROOM, MushroomCow.createAttributes().build())						//Mooshroom
                .putFast(EntityType.MULE, Mule.createBaseChestedHorseAttributes().build())					//Mule
                .putFast(EntityType.OCELOT, Ocelot.createAttributes().build())								//Ocelot
                .putFast(EntityType.PANDA, Panda.createAttributes().build())								//Panda
                .putFast(EntityType.PARROT, Parrot.createAttributes().build())								//Parrot
                .putFast(EntityType.PHANTOM, Phantom.createMobAttributes().build())							//Phantom
                .putFast(EntityType.PIG, Pig.createAttributes().build())									//Pig
                .putFast(EntityType.PIGLIN, Piglin.createAttributes().build())								//Piglin
                .putFast(EntityType.PIGLIN_BRUTE, PiglinBrute.createAttributes().build()) 					//Piglin Brute
                .putFast(EntityType.PILLAGER, Pillager.createAttributes().build())							//Pillager
                .putFast(EntityType.PLAYER, Player.createAttributes().build())								//Player
                .putFast(EntityType.POLAR_BEAR, PolarBear.createAttributes().build())						//PolarBear
                .putFast(EntityType.PUFFERFISH, Pufferfish.createAttributes().build())						//Pufferfish
                .putFast(EntityType.RABBIT, Rabbit.createAttributes().build())								//Rabbit
                .putFast(EntityType.RAVAGER, Ravager.createAttributes().build())							//Ravager
                .putFast(EntityType.SALMON, Salmon.createAttributes().build())								//Salmon
                .putFast(EntityType.SHEEP, Sheep.createAttributes().build())								//Sheep
                .putFast(EntityType.SHULKER, Shulker.createAttributes().build())							//Shulker
                .putFast(EntityType.SILVERFISH, Silverfish.createAttributes().build())						//Silverfish
                .putFast(EntityType.SKELETON, Skeleton.createAttributes().build())							//Skeleton
                .putFast(EntityType.SKELETON_HORSE, SkeletonHorse.createAttributes().build())				//Skeleton Horse
                .putFast(EntityType.SLIME, Slime.createMobAttributes().build())								//Slime
                .putFast(EntityType.SNOW_GOLEM, SnowGolem.createAttributes().build())						//Snow Golem
                .putFast(EntityType.SPIDER, Spider.createAttributes().build())								//Spider
                .putFast(EntityType.SQUID, Squid.createAttributes().build())								//Squid
                .putFast(EntityType.STRAY, Stray.createAttributes().build())								//Stray
                .putFast(EntityType.STRIDER, Strider.createAttributes().build())							//Strider
                .putFast(EntityType.TRADER_LLAMA, TraderLlama.createAttributes().build())					//Trader_Llama
                .putFast(EntityType.TROPICAL_FISH, TropicalFish.createAttributes().build())					//Tropical Fish
                .putFast(EntityType.TURTLE, Turtle.createAttributes().build())								//Turtle
                .putFast(EntityType.VEX, Vex.createAttributes().build())									//Vex
                .putFast(EntityType.VILLAGER, Villager.createAttributes().build())							//Villager
                .putFast(EntityType.VINDICATOR, Vindicator.createAttributes().build())						//Vindicator
                .putFast(EntityType.WANDERING_TRADER, WanderingTrader.createMobAttributes().build())		//Wandering Trader
                .putFast(EntityType.WITCH, Witch.createAttributes().build())								//Witch
                .putFast(EntityType.WITHER, WitherBoss.createAttributes().build())							//Wither
                .putFast(EntityType.WITHER_SKELETON, WitherSkeleton.createAttributes().build())				//Wither Skeleton
                .putFast(EntityType.WOLF, Wolf.createAttributes().build())									//Wolf
                .putFast(EntityType.ZOGLIN, Zoglin.createAttributes().build())								//Zoglin
                .putFast(EntityType.ZOMBIE, Zombie.createAttributes().build())								//Zombie
                .putFast(EntityType.ZOMBIE_HORSE, ZombieHorse.createAttributes().build())					//Zombie Horse
                .putFast(EntityType.ZOMBIE_VILLAGER, ZombieVillager.createAttributes().build())				//Zombie Villager
                .putFast(EntityType.ZOMBIFIED_PIGLIN, ZombifiedPiglin.createAttributes().build());			//Zombie Piglin

    }

    public static AttributeSupplier getAttribute(EntityType<?> types) {
        return defaultAttribute.get(types);
    }

    public static void registerCustomEntityType(EntityType<? extends LivingEntity> customType, EntityType<? extends LivingEntity> rootType) {
        defaultAttribute.put(customType, getAttribute(rootType));
    }

    static class FastMap<K, V> extends HashMap<K, V> {

        public FastMap<K, V> putFast(K key, V value) {
            put(key, value);
            return this;
        }
    }
}
