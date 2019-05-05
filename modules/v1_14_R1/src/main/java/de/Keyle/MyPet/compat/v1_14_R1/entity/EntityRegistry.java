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

package de.Keyle.MyPet.compat.v1_14_R1.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Compat("v1_14_R1")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

    Map<MyPetType, Class<? extends EntityMyPet>> entityClasses = new HashMap<>();
    Map<MyPetType, EntityTypes> entityTypes = new HashMap<>();

    public EntityRegistry() {
    }

    protected void registerEntityType(MyPetType petType, String key) {
        EntitySize size = IRegistry.ENTITY_TYPE.get(new MinecraftKey(key.toLowerCase())).j();
        entityTypes.put(petType, IRegistry.a(IRegistry.ENTITY_TYPE, "mypet_" + key.toLowerCase(), EntityTypes.a.a(EnumCreatureType.CREATURE).b().a().a(size.width, size.height).a(key)));
        overwriteEntityID(entityTypes.get(petType), getEntityTypeId(petType));
    }

    @SuppressWarnings("unchecked")
    protected void registerEntity(MyPetType type) {
        Class<? extends EntityMyPet> entityClass = ReflectionUtil.getClass("de.Keyle.MyPet.compat.v1_14_R1.entity.types.EntityMy" + type.name());
        entityClasses.put(type, entityClass);
        String key = type.getTypeID().toString();
        registerEntityType(type, key);
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
    public void registerEntityTypes() {
        for (MyPetType type : MyPetType.values()) {
            registerEntity(type);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getEntityType(MyPetType petType) {
        return (T) this.entityTypes.get(petType);
    }

    @Override
    public void unregisterEntityTypes() {
    }

    @SuppressWarnings("ConstantConditions")
    protected void overwriteEntityID(EntityTypes types, int id) {
        RegistryID registryID = (RegistryID) ReflectionUtil.getFieldValue(RegistryMaterials.class, IRegistry.ENTITY_TYPE, "b");
        int[] c = (int[]) ReflectionUtil.getFieldValue(RegistryID.class, registryID, "c");
        Method b = ReflectionUtil.getMethod(RegistryID.class, "b", Object.class, int.class);
        Method d = ReflectionUtil.getMethod(RegistryID.class, "d", Object.class);
        try {
            int dVal = (int) d.invoke(registryID, types);
            int bVal = (int) b.invoke(registryID, types, dVal);
            c[bVal] = id;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    protected int getEntityTypeId(MyPetType type) {
        EntityTypes types = IRegistry.ENTITY_TYPE.get(new MinecraftKey(type.getTypeID().toString()));
        return IRegistry.ENTITY_TYPE.a(types);
    }
}