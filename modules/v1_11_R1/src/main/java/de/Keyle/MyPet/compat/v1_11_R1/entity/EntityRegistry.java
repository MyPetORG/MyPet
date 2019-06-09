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

package de.Keyle.MyPet.compat.v1_11_R1.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_11_R1.entity.types.*;
import net.minecraft.server.v1_11_R1.EntityTypes;
import net.minecraft.server.v1_11_R1.RegistryID;
import net.minecraft.server.v1_11_R1.RegistryMaterials;
import net.minecraft.server.v1_11_R1.World;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static de.Keyle.MyPet.api.entity.MyPetType.*;

@Compat("v1_11_R1")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

    protected static Map<MyPetType, Class<? extends EntityMyPet>> entityClasses = new HashMap<>();

    Field RegistryMaterials_a = ReflectionUtil.getField(RegistryMaterials.class, "a");
    Field RegistryID_b = ReflectionUtil.getField(RegistryID.class, "b");
    Field RegistryID_c = ReflectionUtil.getField(RegistryID.class, "c");
    Field RegistryID_d = ReflectionUtil.getField(RegistryID.class, "d");

    public EntityRegistry() {
        entityClasses.put(Bat, EntityMyBat.class);
        entityClasses.put(Blaze, EntityMyBlaze.class);
        entityClasses.put(CaveSpider, EntityMyCaveSpider.class);
        entityClasses.put(Chicken, EntityMyChicken.class);
        entityClasses.put(Cow, EntityMyCow.class);
        entityClasses.put(Creeper, EntityMyCreeper.class);
        entityClasses.put(Donkey, EntityMyDonkey.class);
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
        entityClasses.put(IronGolem, EntityMyIronGolem.class);
        entityClasses.put(Llama, EntityMyLlama.class);
        entityClasses.put(MagmaCube, EntityMyMagmaCube.class);
        entityClasses.put(Mooshroom, EntityMyMooshroom.class);
        entityClasses.put(Mule, EntityMyMule.class);
        entityClasses.put(Ocelot, EntityMyOcelot.class);
        entityClasses.put(Pig, EntityMyPig.class);
        entityClasses.put(PigZombie, EntityMyPigZombie.class);
        entityClasses.put(PolarBear, EntityMyPolarBear.class);
        entityClasses.put(Rabbit, EntityMyRabbit.class);
        entityClasses.put(Sheep, EntityMySheep.class);
        entityClasses.put(Silverfish, EntityMySilverfish.class);
        entityClasses.put(Skeleton, EntityMySkeleton.class);
        entityClasses.put(SkeletonHorse, EntityMySkeletonHorse.class);
        entityClasses.put(Slime, EntityMySlime.class);
        entityClasses.put(Snowman, EntityMySnowman.class);
        entityClasses.put(Spider, EntityMySpider.class);
        entityClasses.put(Squid, EntityMySquid.class);
        entityClasses.put(Stray, EntityMyStray.class);
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
    @SuppressWarnings("unchecked")
    public void registerEntityTypes() {
        RegistryMaterials registry = getRegistry();
        Object[] backup = backupRegistryID(registry);

        for (MyPetType type : entityClasses.keySet()) {
            try {
                registry.a((Integer) type.getTypeID(), null, entityClasses.get(type));
            } catch (NullPointerException ignored) {
                // NPE means that the entity was registered successfully but the key was not
            }
        }
        restoreRegistryID(registry, backup);
    }

    protected Object[] backupRegistryID(RegistryMaterials registry) {
        RegistryID a = (RegistryID) ReflectionUtil.getFieldValue(RegistryMaterials_a, registry);
        Object[] d = (Object[]) ReflectionUtil.getFieldValue(RegistryID_d, a);

        return Arrays.copyOf(d, d.length);
    }

    protected void restoreRegistryID(RegistryMaterials registry, Object[] backup) {
        RegistryID a = (RegistryID) ReflectionUtil.getFieldValue(RegistryMaterials_a, registry);
        Object[] d = (Object[]) ReflectionUtil.getFieldValue(RegistryID_d, a);

        if (d != null) {
            for (int i = 0; i < backup.length; i++) {
                if (backup[i] != null) {
                    d[i] = backup[i];
                }
            }
        }
    }

    protected RegistryMaterials getRegistry() {
        if (EntityTypes.b.getClass() != RegistryMaterials.class) {
            return getCustomRegistry(EntityTypes.b);
        }
        return EntityTypes.b;
    }

    protected RegistryMaterials getCustomRegistry(RegistryMaterials registryMaterials) {
        MyPetApi.getLogger().info("Custom entity registry found: " + registryMaterials.getClass().getName());
        for (Field field : registryMaterials.getClass().getDeclaredFields()) {
            if (field.getType() == RegistryMaterials.class) {
                field.setAccessible(true);
                try {
                    RegistryMaterials reg = (RegistryMaterials) field.get(registryMaterials);

                    if (reg.getClass() != RegistryMaterials.class) {
                        reg = getCustomRegistry(reg);
                    }

                    return reg;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return registryMaterials;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterEntityTypes() {
        RegistryMaterials registry = getRegistry();

        RegistryID registryID = (RegistryID) ReflectionUtil.getFieldValue(RegistryMaterials_a, registry);
        Object[] entityClasses = (Object[]) ReflectionUtil.getFieldValue(RegistryID_b, registryID);
        int[] entityIDs = (int[]) ReflectionUtil.getFieldValue(RegistryID_c, registryID);

        if (entityClasses != null && entityIDs != null) {
            for (int i = 0; i < entityClasses.length; i++) {
                if (entityClasses[i] != null) {
                    if (EntityMyPet.class.isAssignableFrom((Class<?>) entityClasses[i])) {
                        entityClasses[i] = null;
                        entityIDs[i] = 0;
                    }
                }
            }
        }
    }
}