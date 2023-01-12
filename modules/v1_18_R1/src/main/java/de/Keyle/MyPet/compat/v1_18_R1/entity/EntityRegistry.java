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

package de.Keyle.MyPet.compat.v1_18_R1.entity;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import lombok.SneakyThrows;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Compat("v1_18_R1")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

	BiMap<MyPetType, Class<? extends EntityMyPet>> entityClasses = HashBiMap.create();
	Map<MyPetType, EntityType> entityTypes = new HashMap<>();
	private DefaultedRegistry<EntityType> custReg = null;

	protected void registerEntityType(MyPetType petType, String key, DefaultedRegistry<EntityType<?>> entityRegistry) {
		EntityDimensions size = entityRegistry.get(new ResourceLocation(key.toLowerCase())).getDimensions();
		EntityType leType;
		if(!entityRegistry.containsKey(ResourceLocation.tryParse("mypet_" + key.toLowerCase()))) {
			leType = Registry.register(entityRegistry, "mypet_" + key.toLowerCase(), EntityType.Builder.createNothing(MobCategory.CREATURE).noSave().noSummon().sized(size.width, size.height).build(key));
		} else {
			leType = entityRegistry.get(ResourceLocation.tryParse("mypet_" + key.toLowerCase()));
		}
		entityTypes.put(petType, leType);
		EntityType<? extends LivingEntity> types = (EntityType<? extends LivingEntity>) entityRegistry.get(new ResourceLocation(key));
		registerDefaultAttributes(entityTypes.get(petType), types);
		overwriteEntityID(entityTypes.get(petType), getEntityTypeId(petType, entityRegistry), entityRegistry);
	}

	@SneakyThrows
	public static void registerDefaultAttributes(EntityType<? extends LivingEntity> customType, EntityType<? extends LivingEntity> rootType) {
		MyAttributeDefaults.registerCustomEntityType(customType, rootType);
	}

	protected void registerEntity(MyPetType type, DefaultedRegistry<EntityType<?>> entityRegistry) {
		Class<? extends EntityMyPet> entityClass = ReflectionUtil.getClass("de.Keyle.MyPet.compat.v1_18_R1.entity.types.EntityMy" + type.name());
		entityClasses.forcePut(type, entityClass);

		String key = type.getTypeID().toString();
		registerEntityType(type, key, entityRegistry);
	}

	public MyPetType getMyPetType(Class<? extends EntityMyPet> clazz) {
		return entityClasses.inverse().get(clazz);
	}

	@Override
	public MyPetMinecraftEntity createMinecraftEntity(MyPet pet, org.bukkit.World bukkitWorld) {
		EntityMyPet petEntity = null;
		Class<? extends MyPetMinecraftEntity> entityClass = entityClasses.get(pet.getPetType());
		Level world = ((CraftWorld) bukkitWorld).getHandle();

		try {
			Constructor<?> ctor = entityClass.getConstructor(Level.class, MyPet.class);
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
			Level world = ((CraftWorld) bukkitWorld).getHandle();
			return world.addFreshEntity(((EntityMyPet) entity), CreatureSpawnEvent.SpawnReason.CUSTOM);
		}
		return false;
	}

	@Override
	public void registerEntityTypes() {
		DefaultedRegistry<EntityType<?>> entityRegistry = getRegistry(Registry.ENTITY_TYPE);
		MethodHandle ENTITY_REGISTRY_SETTER = ReflectionUtil.createStaticFinalSetter(Registry.class, "Z"); //ENTITY_TYPE

		if(custReg != null) {
			//Gotta put the original Registry in. Just for a moment
			try {
				ENTITY_REGISTRY_SETTER.invoke(entityRegistry);
			} catch (Throwable e) {
			}
		}

		for (MyPetType type : MyPetType.all()) {
			registerEntity(type, entityRegistry);
		}

		if(custReg != null) {
			//Gotta put the custom Registry back into place
			try {
				ENTITY_REGISTRY_SETTER.invoke(custReg);
			} catch (Throwable e) {
			}
			custReg = null;
		}
	}

	public <T> T getEntityType(MyPetType petType) {
		return (T) this.entityTypes.get(petType);
	}

	@Override
	public void unregisterEntityTypes() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DefaultedRegistry<EntityType<?>> getRegistry(DefaultedRegistry registryMaterials) {
		if (!registryMaterials.getClass().getName().equals(DefaultedRegistry.class.getName())) {
			MyPetApi.getLogger().info("Custom entity registry found: " + registryMaterials.getClass().getName());
			if(custReg == null) {
				custReg = registryMaterials;
			}
			for (Field field : registryMaterials.getClass().getDeclaredFields()) {
				if (field.getType() == DefaultedRegistry.class) {
					field.setAccessible(true);
					try {
						DefaultedRegistry<EntityType<?>> reg = (DefaultedRegistry<EntityType<?>>) field.get(registryMaterials);

						if (!reg.getClass().getName().equals(DefaultedRegistry.class.getName())) {
							reg = getRegistry(reg);
						}

						return reg;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return registryMaterials;
	}

	protected void overwriteEntityID(EntityType types, int id, DefaultedRegistry<EntityType<?>> entityRegistry) {
		try {
			Field bgF = MappedRegistry.class.getDeclaredField("bA"); //This is toId
			bgF.setAccessible(true);
			Object map = bgF.get(entityRegistry);
			Class<?> clazz = map.getClass();
			Method mapPut = clazz.getDeclaredMethod("put", Object.class, int.class);
			mapPut.setAccessible(true);
			mapPut.invoke(map, types, id);
		} catch (ReflectiveOperationException ex) {

			ex.printStackTrace();
		}

	}

	protected int getEntityTypeId(MyPetType type, DefaultedRegistry<EntityType<?>> entityRegistry) {
		EntityType<?> types = entityRegistry.get(new ResourceLocation(type.getTypeID().toString()));
		return entityRegistry.getId(types);
	}
}
