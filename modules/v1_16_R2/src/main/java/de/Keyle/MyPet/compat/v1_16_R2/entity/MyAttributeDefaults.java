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

package de.Keyle.MyPet.compat.v1_16_R2.entity;

import net.minecraft.server.v1_16_R2.*;

import java.util.HashMap;

public class MyAttributeDefaults {

    private static final FastMap<EntityTypes<? extends EntityLiving>, AttributeProvider> defaultAttribute = new FastMap<>();

    static {
        defaultAttribute
                .putFast(EntityTypes.ARMOR_STAND, EntityLiving.cK().a())
                .putFast(EntityTypes.BAT, EntityBat.m().a())
                .putFast(EntityTypes.BEE, EntityBee.eZ().a())
                .putFast(EntityTypes.BLAZE, EntityBlaze.m().a())
                .putFast(EntityTypes.CAT, EntityCat.fa().a())
                .putFast(EntityTypes.CAVE_SPIDER, EntityCaveSpider.m().a())
                .putFast(EntityTypes.CHICKEN, EntityChicken.eK().a())
                .putFast(EntityTypes.COD, EntityFish.m().a())
                .putFast(EntityTypes.COW, EntityCow.eK().a())
                .putFast(EntityTypes.CREEPER, EntityCreeper.m().a())
                .putFast(EntityTypes.DOLPHIN, EntityDolphin.eM().a())
                .putFast(EntityTypes.DONKEY, EntityHorseChestedAbstract.eL().a())
                .putFast(EntityTypes.DROWNED, EntityZombie.eS().a())
                .putFast(EntityTypes.ELDER_GUARDIAN, EntityGuardianElder.m().a())
                .putFast(EntityTypes.ENDERMAN, EntityEnderman.m().a())
                .putFast(EntityTypes.ENDERMITE, EntityEndermite.m().a())
                .putFast(EntityTypes.ENDER_DRAGON, EntityEnderDragon.m().a()).
                putFast(EntityTypes.EVOKER, EntityEvoker.eK().a())
                .putFast(EntityTypes.FOX, EntityFox.eK().a())
                .putFast(EntityTypes.GHAST, EntityGhast.eJ().a())
                .putFast(EntityTypes.GIANT, EntityGiantZombie.m().a())

                .putFast(EntityTypes.GUARDIAN, EntityGuardian.eM().a())
                .putFast(EntityTypes.HOGLIN, EntityHoglin.eK().a())
                .putFast(EntityTypes.HORSE, EntityHorseAbstract.fi().a())
                .putFast(EntityTypes.HUSK, EntityZombie.eS().a())
                .putFast(EntityTypes.ILLUSIONER, EntityIllagerIllusioner.eK().a())
                .putFast(EntityTypes.IRON_GOLEM, EntityIronGolem.m().a())
                .putFast(EntityTypes.LLAMA, EntityLlama.fw().a())
                .putFast(EntityTypes.MAGMA_CUBE, EntityMagmaCube.m().a())
                .putFast(EntityTypes.MOOSHROOM, EntityCow.eK().a())
                .putFast(EntityTypes.MULE, EntityHorseChestedAbstract.eL().a())
                .putFast(EntityTypes.OCELOT, EntityOcelot.eK().a())
                .putFast(EntityTypes.PANDA, EntityPanda.eY().a())
                .putFast(EntityTypes.PARROT, EntityParrot.eU().a()
                ).putFast(EntityTypes.PHANTOM, EntityMonster.eR().a())
                .putFast(EntityTypes.PIG, EntityPig.eK().a())
                .putFast(EntityTypes.PIGLIN, EntityPiglin.eT().a())
                .putFast(EntityTypes.PIGLIN_BRUTE, EntityPiglinBrute.eS().a()) //new 1.16.2
                .putFast(EntityTypes.PILLAGER, EntityPillager.eK().a())
                .putFast(EntityTypes.PLAYER, EntityHuman.eo().a())
                .putFast(EntityTypes.POLAR_BEAR, EntityPolarBear.eK().a())
                .putFast(EntityTypes.PUFFERFISH, EntityFish.m().a()).
                putFast(EntityTypes.RABBIT, EntityRabbit.eL().a())
                .putFast(EntityTypes.RAVAGER, EntityRavager.m().a())
                .putFast(EntityTypes.SALMON, EntityFish.m().a())
                .putFast(EntityTypes.SHEEP, EntitySheep.eK().a())
                .putFast(EntityTypes.SHULKER, EntityShulker.m().a())
                .putFast(EntityTypes.SILVERFISH, EntitySilverfish.m().a())
                .putFast(EntityTypes.SKELETON, EntitySkeletonAbstract.m().a()).
                putFast(EntityTypes.SKELETON_HORSE, EntityHorseSkeleton.eL().a())
                .putFast(EntityTypes.SLIME, EntityMonster.eR().a())
                .putFast(EntityTypes.SNOW_GOLEM, EntitySnowman.m().a())
                .putFast(EntityTypes.SPIDER, EntitySpider.eK().a())
                .putFast(EntityTypes.SQUID, EntitySquid.m().a())
                .putFast(EntityTypes.STRAY, EntitySkeletonAbstract.m().a())
                .putFast(EntityTypes.STRIDER, EntityStrider.eM().a())
                .putFast(EntityTypes.TRADER_LLAMA, EntityLlama.fw().a())
                .putFast(EntityTypes.TROPICAL_FISH, EntityFish.m().a())
                .putFast(EntityTypes.TURTLE, EntityTurtle.eM().a())
                .putFast(EntityTypes.VEX, EntityVex.m().a())
                .putFast(EntityTypes.VILLAGER, EntityVillager.eY().a())
                .putFast(EntityTypes.VINDICATOR, EntityVindicator.eK()
                        .a()).putFast(EntityTypes.WANDERING_TRADER, EntityInsentient.p().a())
                .putFast(EntityTypes.WITCH, EntityWitch.eK().a())
                .putFast(EntityTypes.WITHER, EntityWither.eK().a())
                .putFast(EntityTypes.WITHER_SKELETON, EntitySkeletonAbstract.m().a())
                .putFast(EntityTypes.WOLF, EntityWolf.eU().a())
                .putFast(EntityTypes.ZOGLIN, EntityZoglin.m().a()).
                putFast(EntityTypes.ZOMBIE, EntityZombie.eS().a())
                .putFast(EntityTypes.ZOMBIE_HORSE, EntityHorseZombie.eL().a()).
                putFast(EntityTypes.ZOMBIE_VILLAGER, EntityZombie.eS().a())
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
