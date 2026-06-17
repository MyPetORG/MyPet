/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.compat.v26_2_R1.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.*;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.*;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.equine.*;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;

public class MyAttributeDefaults {

    private static final FastMap<EntityType<? extends LivingEntity>, AttributeSupplier> defaultAttribute = new FastMap<>();

    static {
        defaultAttribute
                .putFast(EntityTypes.ARMOR_STAND, ArmorStand.createLivingAttributes().build()) 				//Armor-Stand
                .putFast(EntityTypes.ALLAY, Allay.createAttributes().build()) 						    	//Allay
                .putFast(EntityTypes.ARMADILLO, Armadillo.createAttributes().build()) 						//Armadillo
                .putFast(EntityTypes.AXOLOTL, Axolotl.createAttributes().build()) 							//Axolotl
                .putFast(EntityTypes.BAT, Bat.createAttributes().build())									//Bat
                .putFast(EntityTypes.BEE, Bee.createAttributes().build())									//Bee
                .putFast(EntityTypes.BLAZE, Blaze.createAttributes().build())								//Blaze
                .putFast(EntityTypes.BOGGED, Bogged.createAttributes().build())                              //Bogged
                .putFast(EntityTypes.BREEZE, Breeze.createAttributes().build())                              //Breeze
                .putFast(EntityTypes.CAMEL, Camel.createAttributes().build())								//Camel
                .putFast(EntityTypes.CAT, Cat.createAttributes().build())									//Cat
                .putFast(EntityTypes.CAVE_SPIDER, CaveSpider.createAttributes().build())						//CaveSpider
                .putFast(EntityTypes.CHICKEN, Chicken.createAttributes().build())							//Chicken
                .putFast(EntityTypes.COD, Cod.createAttributes().build())									//Cod
                .putFast(EntityTypes.COPPER_GOLEM, CopperGolem.createAttributes().build())					//Copper Golem
                .putFast(EntityTypes.COW, Cow.createAttributes().build())									//Cow
                .putFast(EntityTypes.CREEPER, Creeper.createAttributes().build())							//Creeper
                .putFast(EntityTypes.CREAKING, Creaking.createAttributes().build())							//Creaking
                .putFast(EntityTypes.DOLPHIN, Dolphin.createAttributes().build())							//Dolphin
                .putFast(EntityTypes.DONKEY, Donkey.createBaseChestedHorseAttributes().build())				//Donkey
                .putFast(EntityTypes.DROWNED, Drowned.createAttributes().build())							//Drowned
                .putFast(EntityTypes.ELDER_GUARDIAN, ElderGuardian.createAttributes().build())				//Elder Guardian
                .putFast(EntityTypes.ENDERMAN, EnderMan.createAttributes().build())							//Enderman
                .putFast(EntityTypes.ENDERMITE, Endermite.createAttributes().build())						//Endermite
                .putFast(EntityTypes.ENDER_DRAGON, EnderDragon.createAttributes().build())					//Ender Dragon
                .putFast(EntityTypes.EVOKER, Evoker.createAttributes().build())								//Evoker
                .putFast(EntityTypes.FOX, Fox.createAttributes().build())									//Fox
                .putFast(EntityTypes.FROG, Frog.createAttributes().build())									//Frog
                .putFast(EntityTypes.GHAST, Ghast.createAttributes().build())								//Ghast
                .putFast(EntityTypes.GIANT, Giant.createAttributes().build())								//Giant
                .putFast(EntityTypes.GUARDIAN, Guardian.createAttributes().build())							//Guardian
                .putFast(EntityTypes.GOAT, Goat.createAttributes().build())									//Goat
                .putFast(EntityTypes.GLOW_SQUID, GlowSquid.createAttributes().build())						//GlowSquid
                .putFast(EntityTypes.HOGLIN, Hoglin.createAttributes().build())								//Hoglin
                .putFast(EntityTypes.HORSE, Horse.createBaseHorseAttributes().build())						//Horse
                .putFast(EntityTypes.HUSK, Husk.createAttributes().build())									//Husk
                .putFast(EntityTypes.ILLUSIONER, Illusioner.createAttributes().build())						//Illusioner
                .putFast(EntityTypes.IRON_GOLEM, IronGolem.createAttributes().build())						//Iron Golem
                .putFast(EntityTypes.LLAMA, Llama.createAttributes().build())								//Llama
                .putFast(EntityTypes.MAGMA_CUBE, MagmaCube.createAttributes().build())						//Magma Cube
                .putFast(EntityTypes.MOOSHROOM, MushroomCow.createAttributes().build())						//Mooshroom
                .putFast(EntityTypes.MULE, Mule.createBaseChestedHorseAttributes().build())					//Mule
                .putFast(EntityTypes.OCELOT, Ocelot.createAttributes().build())								//Ocelot
                .putFast(EntityTypes.PANDA, Panda.createAttributes().build())								//Panda
                .putFast(EntityTypes.PARROT, Parrot.createAttributes().build())								//Parrot
                .putFast(EntityTypes.PHANTOM, Phantom.createMobAttributes().build())							//Phantom
                .putFast(EntityTypes.PIG, Pig.createAttributes().build())									//Pig
                .putFast(EntityTypes.PIGLIN, Piglin.createAttributes().build())								//Piglin
                .putFast(EntityTypes.PIGLIN_BRUTE, PiglinBrute.createAttributes().build()) 					//Piglin Brute
                .putFast(EntityTypes.PILLAGER, Pillager.createAttributes().build())							//Pillager
                .putFast(EntityTypes.PLAYER, Player.createAttributes().build())								//Player
                .putFast(EntityTypes.POLAR_BEAR, PolarBear.createAttributes().build())						//PolarBear
                .putFast(EntityTypes.PUFFERFISH, Pufferfish.createAttributes().build())						//Pufferfish
                .putFast(EntityTypes.RABBIT, Rabbit.createAttributes().build())								//Rabbit
                .putFast(EntityTypes.RAVAGER, Ravager.createAttributes().build())							//Ravager
                .putFast(EntityTypes.SALMON, Salmon.createAttributes().build())								//Salmon
                .putFast(EntityTypes.SHEEP, Sheep.createAttributes().build())								//Sheep
                .putFast(EntityTypes.SHULKER, Shulker.createAttributes().build())							//Shulker
                .putFast(EntityTypes.SILVERFISH, Silverfish.createAttributes().build())						//Silverfish
                .putFast(EntityTypes.SKELETON, Skeleton.createAttributes().build())							//Skeleton
                .putFast(EntityTypes.SKELETON_HORSE, SkeletonHorse.createAttributes().build())				//Skeleton Horse
                .putFast(EntityTypes.SLIME, Slime.createMobAttributes().build())								//Slime
                .putFast(EntityTypes.SNIFFER, Sniffer.createAttributes().build())						    //Sniffer
                .putFast(EntityTypes.SNOW_GOLEM, SnowGolem.createAttributes().build())						//Snow Golem
                .putFast(EntityTypes.SPIDER, Spider.createAttributes().build())								//Spider
                .putFast(EntityTypes.SQUID, Squid.createAttributes().build())								//Squid
                .putFast(EntityTypes.STRAY, Stray.createAttributes().build())								//Stray
                .putFast(EntityTypes.STRIDER, Strider.createAttributes().build())							//Strider
                .putFast(EntityTypes.TADPOLE, Tadpole.createAttributes().build())							//Tadpole
                .putFast(EntityTypes.TRADER_LLAMA, TraderLlama.createAttributes().build())					//Trader_Llama
                .putFast(EntityTypes.TROPICAL_FISH, TropicalFish.createAttributes().build())					//Tropical Fish
                .putFast(EntityTypes.TURTLE, Turtle.createAttributes().build())								//Turtle
                .putFast(EntityTypes.VEX, Vex.createAttributes().build())									//Vex
                .putFast(EntityTypes.VILLAGER, Villager.createAttributes().build())							//Villager
                .putFast(EntityTypes.VINDICATOR, Vindicator.createAttributes().build())						//Vindicator
                .putFast(EntityTypes.WANDERING_TRADER, WanderingTrader.createMobAttributes().build())		//Wandering Trader
                .putFast(EntityTypes.WARDEN, Warden.createMobAttributes().build())		                    //Warden
                .putFast(EntityTypes.WITCH, Witch.createAttributes().build())								//Witch
                .putFast(EntityTypes.WITHER, WitherBoss.createAttributes().build())							//Wither
                .putFast(EntityTypes.WITHER_SKELETON, WitherSkeleton.createAttributes().build())				//Wither Skeleton
                .putFast(EntityTypes.WOLF, Wolf.createAttributes().build())									//Wolf
                .putFast(EntityTypes.ZOGLIN, Zoglin.createAttributes().build())								//Zoglin
                .putFast(EntityTypes.ZOMBIE, Zombie.createAttributes().build())								//Zombie
                .putFast(EntityTypes.ZOMBIE_HORSE, ZombieHorse.createAttributes().build())					//Zombie Horse
                .putFast(EntityTypes.ZOMBIE_VILLAGER, ZombieVillager.createAttributes().build())				//Zombie Villager
                .putFast(EntityTypes.ZOMBIFIED_PIGLIN, ZombifiedPiglin.createAttributes().build());			//Zombie Piglin

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
