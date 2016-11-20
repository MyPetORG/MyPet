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
import de.Keyle.MyPet.api.entity.types.MyGuardian;
import de.Keyle.MyPet.api.entity.types.MyHorse;
import de.Keyle.MyPet.api.entity.types.MySkeleton;
import de.Keyle.MyPet.api.entity.types.MyZombie;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_11_R1.entity.types.*;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

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
        entityClasses.put(EnderDragon, EntityMyEnderDragon.class);
        entityClasses.put(Enderman, EntityMyEnderman.class);
        entityClasses.put(Endermite, EntityMyEndermite.class);
        entityClasses.put(Evoker, EntityMyEvoker.class);
        entityClasses.put(Ghast, EntityMyGhast.class);
        entityClasses.put(Giant, EntityMyGiant.class);
        entityClasses.put(Guardian, EntityMyGuardian.class);
        entityClasses.put(Horse, EntityMyHorse.class);
        entityClasses.put(IronGolem, EntityMyIronGolem.class);
        entityClasses.put(Llama, EntityMyLlama.class);
        entityClasses.put(MagmaCube, EntityMyMagmaCube.class);
        entityClasses.put(Mooshroom, EntityMyMooshroom.class);
        entityClasses.put(Ocelot, EntityMyOcelot.class);
        entityClasses.put(Pig, EntityMyPig.class);
        entityClasses.put(PigZombie, EntityMyPigZombie.class);
        entityClasses.put(PolarBear, EntityMyPolarBear.class);
        entityClasses.put(Rabbit, EntityMyRabbit.class);
        entityClasses.put(Sheep, EntityMySheep.class);
        entityClasses.put(Silverfish, EntityMySilverfish.class);
        entityClasses.put(Skeleton, EntityMySkeleton.class);
        entityClasses.put(Slime, EntityMySlime.class);
        entityClasses.put(Snowman, EntityMySnowman.class);
        entityClasses.put(Spider, EntityMySpider.class);
        entityClasses.put(Squid, EntityMySquid.class);
        entityClasses.put(Witch, EntityMyWitch.class);
        entityClasses.put(Wither, EntityMyWither.class);
        entityClasses.put(Wolf, EntityMyWolf.class);
        entityClasses.put(Vex, EntityMyVex.class);
        entityClasses.put(Villager, EntityMyVillager.class);
        entityClasses.put(Vindicator, EntityMyVindicator.class);
        entityClasses.put(Zombie, EntityMyZombie.class);
    }

    @Override
    public MyPetMinecraftEntity createMinecraftEntity(MyPet pet, org.bukkit.World bukkitWorld) {
        EntityMyPet petEntity = null;

        Class<? extends MyPetMinecraftEntity> entityClass = null;
        switch (pet.getPetType()) {
            case Horse:
                switch (((MyHorse) pet).getHorseType()) {
                    case 1:
                        entityClass = EntityMyDonkey.class;
                        break;
                    case 2:
                        entityClass = EntityMyMule.class;
                        break;
                    case 3:
                        entityClass = EntityMyZombieHorse.class;
                        break;
                    case 4:
                        entityClass = EntityMySkeletonHorse.class;
                        break;
                }
                break;
            case Guardian:
                if (((MyGuardian) pet).isElder()) {
                    entityClass = EntityMyElderGuardian.class;
                }
                break;
            case Skeleton:
                if (((MySkeleton) pet).isWither()) {
                    entityClass = EntityMyWitherSkeleton.class;
                } else if (((MySkeleton) pet).isStray()) {
                    entityClass = EntityMyStray.class;
                }
                break;
            case Zombie:
                if (((MyZombie) pet).isVillager()) {
                    entityClass = EntityMyZombieVillager.class;
                } else if (((MyZombie) pet).isHusk()) {
                    entityClass = EntityMyHusk.class;
                }
                break;
        }
        if (entityClass == null) {
            entityClass = entityClasses.get(pet.getPetType());
        }

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

        registry.addToRegistry(4, new MinecraftKey("elder_guardian"), EntityMyElderGuardian.class);
        registry.addToRegistry(5, new MinecraftKey("wither_skeleton"), EntityMyWitherSkeleton.class);
        registry.addToRegistry(6, new MinecraftKey("stray"), EntityMyStray.class);
        registry.addToRegistry(23, new MinecraftKey("husk"), EntityMyHusk.class);
        registry.addToRegistry(27, new MinecraftKey("zombie_villager"), EntityMyZombieVillager.class);
        registry.addToRegistry(28, new MinecraftKey("skeleton_horse"), EntityMySkeletonHorse.class);
        registry.addToRegistry(29, new MinecraftKey("zombie_horse"), EntityMyZombieHorse.class);
        registry.addToRegistry(31, new MinecraftKey("donkey"), EntityMyDonkey.class);
        registry.addToRegistry(32, new MinecraftKey("mule"), EntityMyMule.class);
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

        private RegistryMaterials original;
        private boolean useCustomEntities = false;
        private final BiMap<MinecraftKey, Class<? extends EntityMyPet>> key2Class = HashBiMap.create();
        private final BiMap<Class<? extends EntityMyPet>, MinecraftKey> class2Key = key2Class.inverse();
        private final BiMap<Class<? extends EntityMyPet>, Integer> ID2Class = HashBiMap.create();
        private final BiMap<Integer, Class<? extends EntityMyPet>> class2ID = ID2Class.inverse();

        public MyPetRegistryMaterials(RegistryMaterials original) {
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
            class2ID.put(id, clazz);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Entity> get(Object key) {
            if (useCustomEntities && key2Class.containsKey(key)) {
                return key2Class.get(key);
            }
            return (Class<? extends Entity>) original.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public MinecraftKey b(Object value) {
            if (useCustomEntities && class2Key.containsKey(value)) {
                return class2Key.get(value);
            }
            return (MinecraftKey) original.b(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public int a(Object key) {
            if (useCustomEntities && ID2Class.containsKey(key)) {
                return ID2Class.get(key);
            }
            return original.a(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends Entity> getId(int id) {
            if (useCustomEntities && class2ID.containsKey(id)) {
                return class2ID.get(id);
            }
            return (Class<? extends Entity>) original.getId(id);
        }
    }
}