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

package de.Keyle.MyPet.compat.v1_13_R2.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_13_R2.entity.types.*;
import net.minecraft.server.v1_13_R2.World;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static de.Keyle.MyPet.api.entity.MyPetType.*;

@Compat("v1_13_R2")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

    protected static Map<MyPetType, Class<? extends EntityMyPet>> entityClasses = new HashMap<>();

    public EntityRegistry() {
        entityClasses.put(Bat, EntityMyBat.class);
        entityClasses.put(Blaze, EntityMyBlaze.class);
        entityClasses.put(CaveSpider, EntityMyCaveSpider.class);
        entityClasses.put(Chicken, EntityMyChicken.class);
        entityClasses.put(Cod, EntityMyCod.class);
        entityClasses.put(Cow, EntityMyCow.class);
        entityClasses.put(Creeper, EntityMyCreeper.class);
        entityClasses.put(Donkey, EntityMyDonkey.class);
        entityClasses.put(Dolphin, EntityMyDolphin.class);
        entityClasses.put(Drowned, EntityMyDrowned.class);
        entityClasses.put(ElderGuardian, EntityMyElderGuardian.class);
        entityClasses.put(EnderDragon, EntityMyEnderDragon.class);
        entityClasses.put(Enderman, EntityMyEnderman.class);
        entityClasses.put(Endermite, EntityMyEndermite.class);
        entityClasses.put(Evoker, EntityMyEvoker.class);
        entityClasses.put(Ghast, EntityMyGhast.class);
        entityClasses.put(Giant, EntityMyGiant.class);
        entityClasses.put(Guardian, EntityMyGuardian.class);
        entityClasses.put(Horse, EntityMyHorse.class);
        entityClasses.put(Husk, EntityMyHusk.class);
        entityClasses.put(Illusioner, EntityMyIllusioner.class);
        entityClasses.put(IronGolem, EntityMyIronGolem.class);
        entityClasses.put(Llama, EntityMyLlama.class);
        entityClasses.put(MagmaCube, EntityMyMagmaCube.class);
        entityClasses.put(Mooshroom, EntityMyMooshroom.class);
        entityClasses.put(Mule, EntityMyMule.class);
        entityClasses.put(Ocelot, EntityMyOcelot.class);
        entityClasses.put(Parrot, EntityMyParrot.class);
        entityClasses.put(Phantom, EntityMyPhantom.class);
        entityClasses.put(Pig, EntityMyPig.class);
        entityClasses.put(PigZombie, EntityMyPigZombie.class);
        entityClasses.put(PolarBear, EntityMyPolarBear.class);
        entityClasses.put(Pufferfish, EntityMyPufferfish.class);
        entityClasses.put(Rabbit, EntityMyRabbit.class);
        entityClasses.put(Salmon, EntityMySalmon.class);
        entityClasses.put(Sheep, EntityMySheep.class);
        entityClasses.put(Silverfish, EntityMySilverfish.class);
        entityClasses.put(Skeleton, EntityMySkeleton.class);
        entityClasses.put(SkeletonHorse, EntityMySkeletonHorse.class);
        entityClasses.put(Slime, EntityMySlime.class);
        entityClasses.put(Snowman, EntityMySnowman.class);
        entityClasses.put(Spider, EntityMySpider.class);
        entityClasses.put(Squid, EntityMySquid.class);
        entityClasses.put(Stray, EntityMyStray.class);
        entityClasses.put(TropicalFish, EntityMyTropicalFish.class);
        entityClasses.put(Turtle, EntityMyTurtle.class);
        entityClasses.put(Witch, EntityMyWitch.class);
        entityClasses.put(Wither, EntityMyWither.class);
        entityClasses.put(WitherSkeleton, EntityMyWitherSkeleton.class);
        entityClasses.put(Wolf, EntityMyWolf.class);
        entityClasses.put(Vex, EntityMyVex.class);
        entityClasses.put(Villager, EntityMyVillager.class);
        entityClasses.put(Vindicator, EntityMyVindicator.class);
        entityClasses.put(Zombie, EntityMyZombie.class);
        entityClasses.put(ZombieHorse, EntityMyZombieHorse.class);
        entityClasses.put(ZombieVillager, EntityMyZombieVillager.class);
    }

    @Override
    public MyPetMinecraftEntity createMinecraftEntity(MyPet pet, org.bukkit.World bukkitWorld) {
        EntityMyPet petEntity = null;
        Class<? extends MyPetMinecraftEntity> entityClass = entityClasses.get(pet.getPetType());
        World world = ((CraftWorld) bukkitWorld).getHandle();

        try {
            Constructor<?> ctor = entityClass.getConstructor(World.class, MyPet.class);
            Object obj = ctor.newInstance(world, pet);
            if (obj instanceof EntityMyPet) {
                petEntity = (EntityMyPet) obj;
            }
        } catch (Exception e) {
            MyPetApi.getLogger().info(ChatColor.RED + Util.getClassName(entityClass) + "(" + pet.getPetType() + ") is no valid MyPet(Entity)!");
            e.printStackTrace();
        }

        return petEntity;
    }

    @Override
    public boolean spawnMinecraftEntity(MyPetMinecraftEntity entity, org.bukkit.World bukkitWorld) {
        if (entity != null) {
            World world = ((CraftWorld) bukkitWorld).getHandle();
            return world.addEntity(((EntityMyPet) entity), CreatureSpawnEvent.SpawnReason.CUSTOM);
        }
        return false;
    }

    @Override
    public void registerEntityTypes() {
    }

    @Override
    public void unregisterEntityTypes() {
    }
}