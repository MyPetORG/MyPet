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

import static net.minecraft.server.v1_16_R1.EntityTypes.*;

public class MyAttributeDefaults {

    private static final FastMap<EntityTypes<? extends EntityLiving>, AttributeProvider> defaultAttribute = new FastMap<>();

    static {
        defaultAttribute
                .putFast(ARMOR_STAND, EntityLiving.cK().a())
                .putFast(BAT, EntityBat.m().a())
                .putFast(BEE, EntityBee.fa().a())
                .putFast(BLAZE, EntityBlaze.m().a())
                .putFast(CAT, EntityCat.fb().a())
                .putFast(CAVE_SPIDER, EntityCaveSpider.m().a())
                .putFast(CHICKEN, EntityChicken.eL().a())
                .putFast(COD, EntityFish.m().a())
                .putFast(COW, EntityCow.eL().a())
                .putFast(CREEPER, EntityCreeper.m().a())
                .putFast(DOLPHIN, EntityDolphin.eN().a())
                .putFast(DONKEY, EntityHorseChestedAbstract.eM().a())
                .putFast(DROWNED, EntityZombie.eT().a())
                .putFast(ELDER_GUARDIAN, EntityGuardianElder.m().a())
                .putFast(ENDERMAN, EntityEnderman.m().a())
                .putFast(ENDERMITE, EntityEndermite.m().a())
                .putFast(ENDER_DRAGON, EntityEnderDragon.m().a())
                .putFast(EVOKER, EntityEvoker.eL().a())
                .putFast(FOX, EntityFox.eL().a())
                .putFast(GHAST, EntityGhast.eK().a())
                .putFast(GIANT, EntityGiantZombie.m().a())
                .putFast(GUARDIAN, EntityGuardian.eN().a())
                .putFast(HOGLIN, EntityHoglin.eL().a())
                .putFast(HORSE, EntityHorseAbstract.fj().a())
                .putFast(HUSK, EntityZombie.eT().a())
                .putFast(ILLUSIONER, EntityIllagerIllusioner.eL().a())
                .putFast(IRON_GOLEM, EntityIronGolem.m().a())
                .putFast(LLAMA, EntityLlama.fx().a())
                .putFast(MAGMA_CUBE, EntityMagmaCube.m().a())
                .putFast(MOOSHROOM, EntityCow.eL().a())
                .putFast(MULE, EntityHorseChestedAbstract.eM().a())
                .putFast(OCELOT, EntityOcelot.eL().a())
                .putFast(PANDA, EntityPanda.eZ().a())
                .putFast(PARROT, EntityParrot.eV().a())
                .putFast(PHANTOM, EntityMonster.eS().a())
                .putFast(PIG, EntityPig.eL().a())
                .putFast(PIGLIN, EntityPiglin.eL().a())
                .putFast(PILLAGER, EntityPillager.eL().a())
                .putFast(PLAYER, EntityHuman.eo().a())
                .putFast(POLAR_BEAR, EntityPolarBear.eL().a())
                .putFast(PUFFERFISH, EntityFish.m().a())
                .putFast(RABBIT, EntityRabbit.eM().a())
                .putFast(RAVAGER, EntityRavager.m().a())
                .putFast(SALMON, EntityFish.m().a())
                .putFast(SHEEP, EntitySheep.eL().a())
                .putFast(SHULKER, EntityShulker.m().a())
                .putFast(SILVERFISH, EntitySilverfish.m().a())
                .putFast(SKELETON, EntitySkeletonAbstract.m().a())
                .putFast(SKELETON_HORSE, EntityHorseSkeleton.eM().a())
                .putFast(SLIME, EntityMonster.eS().a())
                .putFast(SNOW_GOLEM, EntitySnowman.m().a())
                .putFast(SPIDER, EntitySpider.eL().a())
                .putFast(SQUID, EntitySquid.m().a())
                .putFast(STRAY, EntitySkeletonAbstract.m().a())
                .putFast(STRIDER, EntityStrider.eN().a())
                .putFast(TRADER_LLAMA, EntityLlama.fx().a())
                .putFast(TROPICAL_FISH, EntityFish.m().a())
                .putFast(TURTLE, EntityTurtle.eN().a())
                .putFast(VEX, EntityVex.m().a())
                .putFast(VILLAGER, EntityVillager.eX().a())
                .putFast(VINDICATOR, EntityVindicator.eL().a())
                .putFast(WANDERING_TRADER, EntityInsentient.p().a())
                .putFast(WITCH, EntityWitch.eL().a())
                .putFast(WITHER, EntityWither.eL().a())
                .putFast(WITHER_SKELETON, EntitySkeletonAbstract.m().a())
                .putFast(WOLF, EntityWolf.eV().a())
                .putFast(ZOGLIN, EntityZoglin.m().a())
                .putFast(ZOMBIE, EntityZombie.eT().a())
                .putFast(ZOMBIE_HORSE, EntityHorseZombie.eM().a())
                .putFast(ZOMBIE_VILLAGER, EntityZombie.eT().a())
                .putFast(ZOMBIFIED_PIGLIN, EntityPigZombie.eX().a());
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
