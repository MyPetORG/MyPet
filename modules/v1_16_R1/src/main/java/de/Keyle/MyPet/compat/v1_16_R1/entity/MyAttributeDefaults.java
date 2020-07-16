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

package de.Keyle.MyPet.compat.v1_16_R1.entity;

import net.minecraft.server.v1_16_R1.*;

import java.util.HashMap;

public class MyAttributeDefaults {

    public static MyMap<EntityTypes<? extends EntityLiving>, AttributeProvider> defaultAttribute = new MyMap<>();

    static {

        defaultAttribute = defaultAttribute
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ARMOR_STAND, EntityLiving.cK().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.BAT, EntityBat.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.BEE, EntityBee.fa().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.BLAZE, EntityBlaze.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.CAT, EntityCat.fb().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.CAVE_SPIDER, EntityCaveSpider.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.CHICKEN, EntityChicken.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.COD, EntityFish.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.COW, EntityCow.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.CREEPER, EntityCreeper.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.DOLPHIN, EntityDolphin.eN().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.DONKEY, EntityHorseChestedAbstract.eM().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.DROWNED, EntityZombie.eT().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ELDER_GUARDIAN, EntityGuardianElder.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ENDERMAN, EntityEnderman.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ENDERMITE, EntityEndermite.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ENDER_DRAGON, EntityEnderDragon.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.EVOKER, EntityEvoker.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.FOX, EntityFox.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.GHAST, EntityGhast.eK().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.GIANT, EntityGiantZombie.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.GUARDIAN, EntityGuardian.eN().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.HOGLIN, EntityHoglin.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.HORSE, EntityHorseAbstract.fj().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.HUSK, EntityZombie.eT().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ILLUSIONER, EntityIllagerIllusioner.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.IRON_GOLEM, EntityIronGolem.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.LLAMA, EntityLlama.fx().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.MAGMA_CUBE, EntityMagmaCube.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.MOOSHROOM, EntityCow.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.MULE, EntityHorseChestedAbstract.eM().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.OCELOT, EntityOcelot.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PANDA, EntityPanda.eZ().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PARROT, EntityParrot.eV().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PHANTOM, EntityMonster.eS().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PIG, EntityPig.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PIGLIN, EntityPiglin.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PILLAGER, EntityPillager.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PLAYER, EntityHuman.eo().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.POLAR_BEAR, EntityPolarBear.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.PUFFERFISH, EntityFish.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.RABBIT, EntityRabbit.eM().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.RAVAGER, EntityRavager.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SALMON, EntityFish.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SHEEP, EntitySheep.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SHULKER, EntityShulker.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SILVERFISH, EntitySilverfish.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SKELETON, EntitySkeletonAbstract.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SKELETON_HORSE, EntityHorseSkeleton.eM().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SLIME, EntityMonster.eS().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SNOW_GOLEM, EntitySnowman.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SPIDER, EntitySpider.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.SQUID, EntitySquid.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.STRAY, EntitySkeletonAbstract.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.STRIDER, EntityStrider.eN().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.TRADER_LLAMA, EntityLlama.fx().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.TROPICAL_FISH, EntityFish.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.TURTLE, EntityTurtle.eN().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.VEX, EntityVex.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.VILLAGER, EntityVillager.eX().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.VINDICATOR, EntityVindicator.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.WANDERING_TRADER, EntityInsentient.p().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.WITCH, EntityWitch.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.WITHER, EntityWither.eL().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.WITHER_SKELETON, EntitySkeletonAbstract.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.WOLF, EntityWolf.eV().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ZOGLIN, EntityZoglin.m().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ZOMBIE, EntityZombie.eT().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ZOMBIE_HORSE, EntityHorseZombie.eM().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ZOMBIE_VILLAGER, EntityZombie.eT().a())
                .putFast(net.minecraft.server.v1_16_R1.EntityTypes.ZOMBIFIED_PIGLIN, EntityPigZombie.eX().a());
    }

    public static class MyMap<K, V> extends HashMap<K, V> {


        public MyMap<K, V> putFast(K key, V value) {
            put(key, value);
            return this;
        }


    }

}
