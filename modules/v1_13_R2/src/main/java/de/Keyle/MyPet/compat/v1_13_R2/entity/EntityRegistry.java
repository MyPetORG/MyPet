/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_13_R2.entity.types.*;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static de.Keyle.MyPet.api.entity.MyPetType.*;

@Compat("v1_13_R2")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

    protected static Map<MyPetType, Class<? extends EntityMyPet>> entityClasses = new HashMap<>();

    Field RegistryMaterials_a = ReflectionUtil.getField(RegistryMaterials.class, "b");
    Field RegistryID_b = ReflectionUtil.getField(RegistryID.class, "b");
    Field RegistryID_c = ReflectionUtil.getField(RegistryID.class, "c");
    Field RegistryID_d = ReflectionUtil.getField(RegistryID.class, "d");
    Field EntityTypes_a_a = ReflectionUtil.getField(EntityTypes.a.class, "a");
    Method EntityTypes_a = ReflectionUtil.getMethod(EntityTypes.class, "a", String.class, EntityTypes.a.class);

    public enum RegistryNames {
        Bat("bat"),
        Blaze("blaze"),
        CaveSpider("cave_spider"),
        Chicken("chicken"),
        Cod("cod"),
        Cow("cow"),
        Creeper("creeper"),
        Donkey("donkey"),
        Dolphin("dolphin"),
        Drowned("drowned"),
        ElderGuardian("elder_guardian"),
        EnderDragon("ender_dragon"),
        Enderman("enderman"),
        Endermite("endermite"),
        Evoker("evoker"),
        Ghast("ghast"),
        Giant("giant"),
        Guardian("guardian"),
        Horse("horse"),
        Husk("husk"),
        Illusioner("illusioner"),
        IronGolem("iron_golem"),
        Llama("llama"),
        MagmaCube("magma_cube"),
        Mooshroom("mooshroom"),
        Mule("mule"),
        Ocelot("ocelot"),
        Parrot("parrot"),
        Phantom("phantom"),
        Pig("pig"),
        PigZombie("zombie_pigman"),
        PolarBear("polar_bear"),
        Pufferfish("pufferfish"),
        Rabbit("rabbit"),
        Salmon("salmon"),
        Sheep("sheep"),
        Silverfish("silverfish"),
        Skeleton("skeleton"),
        SkeletonHorse("skeleton_horse"),
        Slime("slime"),
        Snowman("snow_golem"),
        Spider("spider"),
        Squid("squid"),
        Stray("stray"),
        Turtle("turtle"),
        TropicalFish("tropicalfish"),
        Witch("witch"),
        Wither("wither"),
        WitherSkeleton("wither_skeleton"),
        Wolf("wolf"),
        Vex("vex"),
        Villager("villager"),
        Vindicator("vindicator"),
        Zombie("zombie"),
        ZombieHorse("zombie_horse"),
        ZombieVillager("zombie_villager");

        public String minecraftName;

        RegistryNames(String typeName) {
            this.minecraftName = typeName;
        }
    }

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
            MyPetApi.getLogger().info(ChatColor.RED + entityClass.getName() + " is no valid MyPet(Entity)!");
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
        IRegistry registry = getRegistry();
        if (isRegistryCustom()) {
            MyPetApi.getLogger().info("Custom entity registry found: " + IRegistry.ENTITY_TYPE.getClass().getName());
        }
        Object[] backup = backupRegistryID(registry);

        for (MyPetType type : entityClasses.keySet()) {
            try {
                EntityTypes.a entitytypes = EntityTypes.a.a(EntityMyPet.class).b().a();
                EntityTypes_a.invoke(null, RegistryNames.valueOf(type.name()).minecraftName, entitytypes);
            } catch (Exception e) {
                e.printStackTrace();
                // NPE means that the entity was registered successfully but the key was not
            }
        }
        restoreRegistryID(registry, backup);
    }

    protected Object[] backupRegistryID(IRegistry registry) {
        RegistryID a = (RegistryID) ReflectionUtil.getFieldValue(RegistryMaterials_a, registry);
        Object[] d = (Object[]) ReflectionUtil.getFieldValue(RegistryID_d, a);

        return Arrays.copyOf(d, d.length);
    }

    protected void restoreRegistryID(IRegistry registry, Object[] backup) {
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

    protected IRegistry getRegistry() {
        if (isRegistryCustom()) {
            return getCustomRegistry(IRegistry.ENTITY_TYPE);
        }
        return IRegistry.ENTITY_TYPE;
    }

    protected boolean isRegistryCustom() {
        return IRegistry.ENTITY_TYPE.getClass() != RegistryMaterials.class;
    }

    public IRegistry getCustomRegistry(IRegistry registryMaterials) {
        for (Field field : registryMaterials.getClass().getDeclaredFields()) {
            if (field.getType() == RegistryMaterials.class) {
                field.setAccessible(true);
                try {
                    IRegistry reg = (RegistryMaterials) field.get(registryMaterials);

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
        IRegistry registry = getRegistry();

        RegistryID registryID = (RegistryID) ReflectionUtil.getFieldValue(RegistryMaterials_a, registry);
        Object[] entityClasses = (Object[]) ReflectionUtil.getFieldValue(RegistryID_b, registryID);
        int[] entityIDs = (int[]) ReflectionUtil.getFieldValue(RegistryID_c, registryID);

        if (entityClasses != null && entityIDs != null) {
            for (int i = 0; i < entityClasses.length; i++) {
                if (entityClasses[i] != null) {
                    if (entityClasses[i] instanceof EntityTypes.a) {
                        Class<?> entityClass = (Class<?>) ReflectionUtil.getFieldValue(EntityTypes_a_a, entityClasses[i]);
                        if (entityClass != null && EntityMyPet.class.isAssignableFrom(entityClass)) {
                            entityClasses[i] = null;
                            entityIDs[i] = 0;
                        }
                    }
                }
            }
        }
    }
}