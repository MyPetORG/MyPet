/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_11_R1.entity.types.*;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.Keyle.MyPet.api.entity.MyPetType.*;

@Compat("v1_11_R1")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

    protected Map<MyPetType, Class<? extends MyPetMinecraftEntity>> entityClasses = new HashMap<>();
    private MyPetRegistryMaterials registry = null;

    public EntityRegistry() {
        replaceEntityRegistryMaterials();

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
            MyPetApi.getLogger().info(ChatColor.RED + entityClass.getName() + " is no valid MyPet(Entity)!");
            e.printStackTrace();
        }

        return petEntity;
    }

    @Override
    public boolean spawnMinecraftEntity(MyPetMinecraftEntity entity, org.bukkit.World bukkitWorld) {
        if (entity != null) {
            World world = ((CraftWorld) bukkitWorld).getHandle();
            registry.enableCustomEntities();
            boolean result = world.addEntity(((EntityMyPet) entity), CreatureSpawnEvent.SpawnReason.CUSTOM);
            registry.disableCustomEntities();
            return result;
        }
        return false;
    }

    private void replaceEntityRegistryMaterials() {
        registry = new MyPetRegistryMaterials(EntityTypes.b);
        try {
            Field registryField = EntityTypes.class.getDeclaredField("b");
            registryField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(registryField, registryField.getModifiers() & ~Modifier.FINAL);
            registryField.set(null, registry);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerEntityTypes() {
        for (MyPetType type : entityClasses.keySet()) {
            registry.addToRegistry(type.getTypeID(), new MinecraftKey(type.getMinecraftName()), (Class<? extends EntityMyPet>) entityClasses.get(type));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterEntityTypes() {
        try {
            Field registryField = EntityTypes.class.getDeclaredField("b");
            registryField.setAccessible(true);
            registryField.set(null, registry.original);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            ex.printStackTrace();
        }
    }

    private class MyPetRegistryMaterials extends RegistryMaterials {

        private RegistryMaterials<MinecraftKey, Class<? extends Entity>> original;
        private boolean useCustomEntities = false;
        private final BiMap<MinecraftKey, Class<? extends EntityMyPet>> key2Class = HashBiMap.create();
        private final BiMap<Class<? extends EntityMyPet>, MinecraftKey> class2Key = key2Class.inverse();
        private final BiMap<Class<? extends EntityMyPet>, Integer> class2ID = HashBiMap.create();
        private final BiMap<Integer, Class<? extends EntityMyPet>> ID2Class = class2ID.inverse();

        public MyPetRegistryMaterials(RegistryMaterials<MinecraftKey, Class<? extends Entity>> original) {
            this.original = original;
            if (this.original instanceof MyPetRegistryMaterials) {
                this.original = ((MyPetRegistryMaterials) this.original).original;
            }
        }

        public void enableCustomEntities() {
            useCustomEntities = true;
        }

        public void disableCustomEntities() {
            useCustomEntities = false;
        }

        public void addToRegistry(int id, MinecraftKey key, Class<? extends EntityMyPet> clazz) {
            key2Class.put(key, clazz);
            ID2Class.put(id, clazz);
        }

        @Nullable
        @Override
        public Class<? extends Entity> a(Random random) {
            return original.a(random);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void a(int i, Object minecraftKey, Object aClass) {
            original.a(i, (MinecraftKey) minecraftKey, (Class<? extends Entity>) aClass);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void a(Object minecraftKey, Object aClass) {
            original.a((MinecraftKey) minecraftKey, (Class<? extends Entity>) aClass);
        }

        @Override
        @SuppressWarnings("unchecked")
        public int a(@Nullable Object aClass) {
            if (class2ID.containsKey(aClass)) {
                return class2ID.get(aClass);
            }
            return original.a((Class<? extends Entity>) aClass);
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public MinecraftKey b(Object aClass) {
            if (useCustomEntities && class2Key.containsKey(aClass)) {
                return class2Key.get(aClass);
            }
            return original.b((Class<? extends Entity>) aClass);
        }

        @Override
        public boolean d(Object minecraftKey) {
            return original.d((MinecraftKey) minecraftKey);
        }

        @Nullable
        @Override
        public Class<? extends Entity> get(@Nullable Object minecraftKey) {
            if (useCustomEntities && key2Class.containsKey(minecraftKey)) {
                return key2Class.get(minecraftKey);
            }
            return original.get((MinecraftKey) minecraftKey);
        }

        @Nullable
        @Override
        public Class<? extends Entity> getId(int id) {
            if (useCustomEntities && ID2Class.containsKey(id)) {
                return ID2Class.get(id);
            }
            return original.getId(id);
        }

        @Override
        public Iterator<Class<? extends Entity>> iterator() {
            return original.iterator();
        }

        @Override
        public Set<MinecraftKey> keySet() {
            return original.keySet();
        }
    }
}