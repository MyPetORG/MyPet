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

import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglinBrute;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.player.EntityHuman;

import java.util.HashMap;

public class MyAttributeDefaults {

    private static final FastMap<EntityTypes<? extends EntityLiving>, AttributeProvider> defaultAttribute = new FastMap<>();

    static {
        defaultAttribute
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ARMOR_STAND, EntityLiving.cL().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.BAT, EntityBat.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.BEE, EntityBee.eZ().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.BLAZE, EntityBlaze.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.CAT, EntityCat.fa().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.CAVE_SPIDER, EntityCaveSpider.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.CHICKEN, EntityChicken.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.COD, EntityFish.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.COW, EntityCow.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.CREEPER, EntityCreeper.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.DOLPHIN, EntityDolphin.eM().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.DONKEY, EntityHorseChestedAbstract.eL().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.DROWNED, EntityZombie.eS().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ELDER_GUARDIAN, EntityGuardianElder.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ENDERMAN, EntityEnderman.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ENDERMITE, EntityEndermite.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ENDER_DRAGON, EntityEnderDragon.m().a()).
                putFast(com.sk89q.worldedit.world.entity.EntityTypes.EVOKER, EntityEvoker.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.FOX, EntityFox.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.GHAST, EntityGhast.eJ().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.GIANT, EntityGiantZombie.m().a())

                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.GUARDIAN, EntityGuardian.eM().a())
                .putFast(EntityTypes.HOGLIN, EntityHoglin.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.HORSE, EntityHorseAbstract.fi().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.HUSK, EntityZombie.eS().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ILLUSIONER, EntityIllagerIllusioner.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.IRON_GOLEM, EntityIronGolem.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.LLAMA, EntityLlama.fw().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.MAGMA_CUBE, EntityMagmaCube.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.MOOSHROOM, EntityCow.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.MULE, EntityHorseChestedAbstract.eL().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.OCELOT, EntityOcelot.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.PANDA, EntityPanda.eY().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.PARROT, EntityParrot.eU().a()
                ).putFast(com.sk89q.worldedit.world.entity.EntityTypes.PHANTOM, EntityMonster.eR().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.PIG, EntityPig.eK().a())
                .putFast(EntityTypes.PIGLIN, EntityPiglin.eT().a())
                .putFast(EntityTypes.PIGLIN_BRUTE, EntityPiglinBrute.eS().a()) //new 1.16.2
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.PILLAGER, EntityPillager.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.PLAYER, EntityHuman.ep().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.POLAR_BEAR, EntityPolarBear.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.PUFFERFISH, EntityFish.m().a()).
                putFast(com.sk89q.worldedit.world.entity.EntityTypes.RABBIT, EntityRabbit.eL().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.RAVAGER, EntityRavager.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SALMON, EntityFish.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SHEEP, EntitySheep.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SHULKER, EntityShulker.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SILVERFISH, EntitySilverfish.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SKELETON, EntitySkeletonAbstract.m().a()).
                putFast(com.sk89q.worldedit.world.entity.EntityTypes.SKELETON_HORSE, EntityHorseSkeleton.eL().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SLIME, EntityMonster.eR().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SNOW_GOLEM, EntitySnowman.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SPIDER, EntitySpider.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.SQUID, EntitySquid.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.STRAY, EntitySkeletonAbstract.m().a())
                .putFast(EntityTypes.STRIDER, EntityStrider.eM().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.TRADER_LLAMA, EntityLlama.fw().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.TROPICAL_FISH, EntityFish.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.TURTLE, EntityTurtle.eM().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.VEX, EntityVex.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.VILLAGER, EntityVillager.eY().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.VINDICATOR, EntityVindicator.eK()
                        .a()).putFast(com.sk89q.worldedit.world.entity.EntityTypes.WANDERING_TRADER, EntityInsentient.p().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.WITCH, EntityWitch.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.WITHER, EntityWither.eK().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.WITHER_SKELETON, EntitySkeletonAbstract.m().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.WOLF, EntityWolf.eU().a())
                .putFast(EntityTypes.ZOGLIN, EntityZoglin.m().a()).
                putFast(com.sk89q.worldedit.world.entity.EntityTypes.ZOMBIE, EntityZombie.eS().a())
                .putFast(com.sk89q.worldedit.world.entity.EntityTypes.ZOMBIE_HORSE, EntityHorseZombie.eL().a()).
                putFast(com.sk89q.worldedit.world.entity.EntityTypes.ZOMBIE_VILLAGER, EntityZombie.eS().a())
                .putFast(EntityTypes.ZOMBIFIED_PIGLIN, EntityPigZombie.eW().a());

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
