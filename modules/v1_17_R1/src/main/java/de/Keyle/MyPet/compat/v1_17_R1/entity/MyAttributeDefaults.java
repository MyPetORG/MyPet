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

import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.EntityBee;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.animal.EntityChicken;
import net.minecraft.world.entity.animal.EntityCow;
import net.minecraft.world.entity.animal.EntityDolphin;
import net.minecraft.world.entity.animal.EntityFish;
import net.minecraft.world.entity.animal.EntityFox;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntityOcelot;
import net.minecraft.world.entity.animal.EntityPanda;
import net.minecraft.world.entity.animal.EntityParrot;
import net.minecraft.world.entity.animal.EntityPig;
import net.minecraft.world.entity.animal.EntityPolarBear;
import net.minecraft.world.entity.animal.EntityRabbit;
import net.minecraft.world.entity.animal.EntitySheep;
import net.minecraft.world.entity.animal.EntitySnowman;
import net.minecraft.world.entity.animal.EntitySquid;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.EntityHorse;
import net.minecraft.world.entity.animal.horse.EntityHorseDonkey;
import net.minecraft.world.entity.animal.horse.EntityHorseMule;
import net.minecraft.world.entity.animal.horse.EntityHorseSkeleton;
import net.minecraft.world.entity.animal.horse.EntityHorseZombie;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.monster.EntityBlaze;
import net.minecraft.world.entity.monster.EntityCaveSpider;
import net.minecraft.world.entity.monster.EntityCreeper;
import net.minecraft.world.entity.monster.EntityEnderman;
import net.minecraft.world.entity.monster.EntityEndermite;
import net.minecraft.world.entity.monster.EntityEvoker;
import net.minecraft.world.entity.monster.EntityGhast;
import net.minecraft.world.entity.monster.EntityGiantZombie;
import net.minecraft.world.entity.monster.EntityGuardian;
import net.minecraft.world.entity.monster.EntityGuardianElder;
import net.minecraft.world.entity.monster.EntityIllagerIllusioner;
import net.minecraft.world.entity.monster.EntityMagmaCube;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.EntityPigZombie;
import net.minecraft.world.entity.monster.EntityPillager;
import net.minecraft.world.entity.monster.EntityRavager;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.entity.monster.EntitySilverfish;
import net.minecraft.world.entity.monster.EntitySkeletonAbstract;
import net.minecraft.world.entity.monster.EntitySpider;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.monster.EntityVex;
import net.minecraft.world.entity.monster.EntityVindicator;
import net.minecraft.world.entity.monster.EntityWitch;
import net.minecraft.world.entity.monster.EntityZoglin;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglinBrute;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.player.EntityHuman;

public class MyAttributeDefaults {

    private static final FastMap<EntityTypes<? extends EntityLiving>, AttributeProvider> defaultAttribute = new FastMap<>();

    static {
        defaultAttribute
                .putFast(EntityTypes.c, EntityArmorStand.dq().a()) 				//Armor-Stand
                .putFast(EntityTypes.e, Axolotl.fE().a()) 						//Axolotl
                .putFast(EntityTypes.f, EntityBat.n().a())						//Bat
                .putFast(EntityTypes.g, EntityBee.fJ().a())						//Bee
                .putFast(EntityTypes.h, EntityBlaze.n().a())					//Blaze
                .putFast(EntityTypes.j, EntityCat.fK().a())						//Cat
                .putFast(EntityTypes.k, EntityCaveSpider.n().a())				//CaveSpider
                .putFast(EntityTypes.l, EntityChicken.p().a())					//Chicken
                .putFast(EntityTypes.m, EntityFish.n().a())						//Cod
                .putFast(EntityTypes.n, EntityCow.p().a())						//Cow
                .putFast(EntityTypes.o, EntityCreeper.n().a())					//Creeper
                .putFast(EntityTypes.p, EntityDolphin.fw().a())					//Dolphin
                .putFast(EntityTypes.q, EntityHorseDonkey.t().a())				//Donkey
                .putFast(EntityTypes.s, EntityZombie.fB().a())					//Drowned
                .putFast(EntityTypes.t, EntityGuardianElder.n().a())			//Elder Guardian
                .putFast(EntityTypes.w, EntityEnderman.n().a())					//Enderman
                .putFast(EntityTypes.x, EntityEndermite.n().a())				//Endermite
                .putFast(EntityTypes.v, EntityEnderDragon.n().a())				//Ender Dragon
                .putFast(EntityTypes.y, EntityEvoker.p().a())					//Evoker
                .putFast(EntityTypes.E, EntityFox.p().a())						//Fox
                .putFast(EntityTypes.F, EntityGhast.t().a())					//Ghast
                .putFast(EntityTypes.G, EntityGiantZombie.n().a())				//Giant
                .putFast(EntityTypes.K, EntityGuardian.fw().a())				//Guardian
                .putFast(EntityTypes.J, Goat.p().a())							//Goat
                .putFast(EntityTypes.I, GlowSquid.fw().a())						//GlowSquid
                .putFast(EntityTypes.L, EntityHoglin.p().a())					//Hoglin
                .putFast(EntityTypes.M, EntityHorse.fS().a())					//Horse
                .putFast(EntityTypes.N, EntityZombie.fB().a())					//Husk
                .putFast(EntityTypes.O, EntityIllagerIllusioner.p().a())		//Illusioner
                .putFast(EntityTypes.P, EntityIronGolem.n().a())				//Iron Golem
                .putFast(EntityTypes.V, EntityLlama.gg().a())					//Llama
                .putFast(EntityTypes.X, EntityMagmaCube.n().a())				//Magma Cube
                .putFast(EntityTypes.ah, EntityCow.p().a())						//Mooshroom
                .putFast(EntityTypes.ag, EntityHorseMule.t().a())				//Mule
                .putFast(EntityTypes.ai, EntityOcelot.p().a())					//Ocelot
                .putFast(EntityTypes.ak, EntityPanda.fI().a())					//Panda
                .putFast(EntityTypes.al, EntityParrot.fE().a())					//Parrot
                .putFast(EntityTypes.am, EntityMonster.fB().a())				//Phantom
                .putFast(EntityTypes.an, EntityPig.p().a())						//Pig
                .putFast(EntityTypes.ao, EntityPiglin.fB().a())					//Piglin
                .putFast(EntityTypes.ap, EntityPiglinBrute.fB().a()) 			//Piglin Brute
                .putFast(EntityTypes.aq, EntityPillager.p().a())				//Pillager
                .putFast(EntityTypes.bi, EntityHuman.eY().a())					//Player
                .putFast(EntityTypes.ar, EntityPolarBear.p().a())				//PolarBear
                .putFast(EntityTypes.at, EntityFish.n().a())					//Pufferfish
                .putFast(EntityTypes.au, EntityRabbit.t().a())					//Rabbit
                .putFast(EntityTypes.av, EntityRavager.n().a())					//Ravager
                .putFast(EntityTypes.aw, EntityFish.n().a())					//Salmon
                .putFast(EntityTypes.ax, EntitySheep.p().a())					//Sheep
                .putFast(EntityTypes.ay, EntityShulker.n().a())					//Shulker
                .putFast(EntityTypes.aA, EntitySilverfish.n().a())				//Silverfish
                .putFast(EntityTypes.aB, EntitySkeletonAbstract.n().a())		//Skeleton
                .putFast(EntityTypes.aC, EntityHorseSkeleton.t().a())			//Skeleton Horse
                .putFast(EntityTypes.aD, EntityMonster.fB().a())				//Slime
                .putFast(EntityTypes.aF, EntitySnowman.n().a())					//Snow Golem
                .putFast(EntityTypes.aI, EntitySpider.p().a())					//Spider
                .putFast(EntityTypes.aJ, EntitySquid.fw().a())					//Squid
                .putFast(EntityTypes.aK, EntitySkeletonAbstract.n().a())		//Stray
                .putFast(EntityTypes.aL, EntityStrider.fw().a())				//Strider
                .putFast(EntityTypes.aR, EntityLlama.gg().a())					//Trader_Llama
                .putFast(EntityTypes.aS, EntityFish.n().a())					//Tropical Fish
                .putFast(EntityTypes.aT, EntityTurtle.fw().a())					//Turtle
                .putFast(EntityTypes.aU, EntityVex.n().a())						//Vex
                .putFast(EntityTypes.aV, EntityVillager.fI().a())				//Villager
                .putFast(EntityTypes.aW, EntityVindicator.p().a())				//Vindicator
                .putFast(EntityTypes.aX, EntityInsentient.w().a())				//Wandering Trader
                .putFast(EntityTypes.aY, EntityWitch.p().a())					//Witch
                .putFast(EntityTypes.aZ, EntityWither.p().a())					//Wither
                .putFast(EntityTypes.ba, EntitySkeletonAbstract.n().a())		//Wither Skeleton
                .putFast(EntityTypes.bc, EntityWolf.fE().a())					//Wolf
                .putFast(EntityTypes.bd, EntityZoglin.n().a())					//Zoglin
                .putFast(EntityTypes.be, EntityZombie.fB().a())					//Zombie
                .putFast(EntityTypes.bf, EntityHorseZombie.t().a())				//Zombie Horse
                .putFast(EntityTypes.bg, EntityZombie.fB().a())					//Zombie Villager
                .putFast(EntityTypes.bh, EntityPigZombie.fG().a());				//Zombie Piglin

    }

    public static AttributeProvider getAttribute(EntityTypes<?> types) {
        return defaultAttribute.get(types);
    }

    public static void registerCustomEntityTypes(EntityTypes<? extends EntityLiving> customType, EntityTypes<? extends EntityLiving> rootType) {
        defaultAttribute.put(customType, getAttribute(rootType));
    }

    static class FastMap<K, V> extends HashMap<K, V> {

        public FastMap<K, V> putFast(K key, V value) {
            put(key, value);
            return this;
        }
    }
}
