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

package de.Keyle.MyPet.compat.v1_21_R5.entity;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import lombok.SneakyThrows;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@Compat("v1_21_R5")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

	BiMap<MyPetType, Class<? extends EntityMyPet>> entityClasses = HashBiMap.create();
	Map<MyPetType, EntityType> entityTypes = new HashMap<>();
	private DefaultedRegistry<EntityType> custReg = null;

	protected void registerEntityType(MyPetType petType, String key, DefaultedRegistry<EntityType<?>> entityRegistry) {
		EntityDimensions size = entityRegistry.get(ResourceLocation.tryParse(key.toLowerCase())).get().value().getDimensions();
		EntityType leType;
		if(!entityRegistry.containsKey(ResourceLocation.tryParse("mypet_" + key.toLowerCase()))) {
			leType = Registry.register(entityRegistry, "mypet_" + key.toLowerCase(), EntityType.Builder.createNothing(MobCategory.CREATURE).noSave().noSummon().sized(size.width(), size.height()).build(
					ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(key))));
		} else {
			leType = entityRegistry.get(ResourceLocation.tryParse("mypet_" + key.toLowerCase())).get().value();
		}
		entityTypes.put(petType, leType);
		EntityType<? extends LivingEntity> types = (EntityType<? extends LivingEntity>) entityRegistry.get(ResourceLocation.tryParse(key)).get().value();
		registerDefaultAttributes(entityTypes.get(petType), types);
		overwriteEntityID(entityTypes.get(petType), getEntityTypeId(petType, entityRegistry), entityRegistry);
	}

	@SneakyThrows
	public static void registerDefaultAttributes(EntityType<? extends LivingEntity> customType, EntityType<? extends LivingEntity> rootType) {
		MyAttributeDefaults.registerCustomEntityType(customType, rootType);
	}

	protected void registerEntity(MyPetType type, DefaultedRegistry<EntityType<?>> entityRegistry) {
		Class<? extends EntityMyPet> entityClass = ReflectionUtil.getClass("de.Keyle.MyPet.compat.v1_21_R5.entity.types.EntityMy" + type.name());
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
		//Let's prepare the Vanilla-Registry
		DefaultedRegistry<EntityType<?>> entityRegistry = getRegistry(BuiltInRegistries.ENTITY_TYPE);
		Field frozenDoBe = ReflectionUtil.getField(MappedRegistry.class,"l"); //frozen
		Field intrusiveHolderCacheField = ReflectionUtil.getField(MappedRegistry.class,"m"); //intrusiveHolderCache or unregisteredIntrusiveHolders or intrusiveValueToEntry
		Field allTagsField = ReflectionUtil.getField(MappedRegistry.class,"k"); //allTags
		MethodHandle ENTITY_REGISTRY_SETTER = ReflectionUtil.createStaticFinalSetter(BuiltInRegistries.class, "f"); //ENTITY_TYPE

		Object allTagsSaved = ReflectionUtil.getFieldValue(allTagsField, entityRegistry);

		if(custReg != null) {
			//Gotta put the original Registry in. Just for a moment
			try {
				ENTITY_REGISTRY_SETTER.invoke(entityRegistry);
			} catch (Throwable e) {
			}
		}

		//We are now working with the Vanilla-Registry
		Class TagSetClass = MappedRegistry.class.getDeclaredClasses()[0];
		Method unboundMethod = ReflectionUtil.getMethod(TagSetClass, "a"); //unbound
		ReflectionUtil.setFinalFieldValue(frozenDoBe, entityRegistry, false);
		ReflectionUtil.setFinalFieldValue(intrusiveHolderCacheField, entityRegistry, new IdentityHashMap());
        try {
            ReflectionUtil.setFinalFieldValue(allTagsField, entityRegistry, unboundMethod.invoke(entityRegistry));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Now lets handle the Bukkit-Registry
		//First copy the old registrie's map into a new one:
		org.bukkit.Registry<org.bukkit.entity.EntityType> bukkitRegistry = org.bukkit.Registry.ENTITY_TYPE;
		Field mapField =  ReflectionUtil.getField(org.bukkit.Registry.SimpleRegistry.class, "map");
		Map<NamespacedKey, org.bukkit.entity.EntityType> bukkitMap = (Map) ReflectionUtil.getFieldValue(mapField, bukkitRegistry);
		ImmutableMap.Builder<NamespacedKey, org.bukkit.entity.EntityType> ownMap = ImmutableMap.builder();
		ownMap.putAll(bukkitMap);

		for (MyPetType type : MyPetType.all()) {
			//The fun part
			registerEntity(type, entityRegistry);

			/*
			A Tutorial on how to trick Spigot:
				Instead of falling back to the "Unknown"-Type, Spigot now does not accept "null" anymore
				(see https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/commits/02d49078870f39892e7e2ce2916e17a879e9a3f0#src/main/java/org/bukkit/craftbukkit/entity/CraftEntityType.java)
				This means that Mypet-Entites (aka mypet_pig etc) can't be converted.
				This means that the "hack" MyPet used at the end of 1.20.1 doesn't work anymore.
				And just replacing the type of the pet when it spawns doesn't work either.
				This is bad.

				Now onto the *trickery*.
				Basically:
				We basically tell Bukkit that we are a BlockDisplay. Yep.
				This means everything will be created properly in the beginning, will be handled (kinda) properly with other plugins
				and also when the pet dies.
				It's stupid that we have to do this but it seems to work -> I'm happy.
			 */
			ownMap.put(NamespacedKey.fromString("mypet_" + type.getTypeID().toString()), org.bukkit.entity.EntityType.BLOCK_DISPLAY);
		}

		//Post-Handle Bukkit-Registry
		//We now have our entities and all the others in there
		ReflectionUtil.setFieldValue(mapField, bukkitRegistry, ownMap.build());

		//Post-Handle Vanilla Registry
		entityRegistry.freeze();

		/* Let me explain what's happening here:
		Earlier we saved allTags. They contain stuff like special vulnerabilities to enchantments,
		FallDamage-Resistance and in some instances (namely the Turtle) the ability for a Mob to
		breath underwater.
		Through the above code we reset those tags so we now have to do 2 things:
		First re-add the Tags (that's why we saved them)
		And then tell Minecraft to reload those values for them to actually take effect.
		Gotta love MC */
		ReflectionUtil.setFieldValue(allTagsField, entityRegistry, allTagsSaved);
		Method refreshMethod = ReflectionUtil.getMethod(MappedRegistry.class, "u"); //refreshTagsInHolders
        try {
            refreshMethod.invoke(entityRegistry);
        } catch (Exception e) {
			e.printStackTrace();
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
	public DefaultedRegistry<EntityType<?>> getRegistry(DefaultedRegistry dMappedRegistry) {
		if (!dMappedRegistry.getClass().getName().equals(DefaultedMappedRegistry.class.getName())) {
			MyPetApi.getLogger().info("Custom entity registry found: " + dMappedRegistry.getClass().getName());
			if(custReg == null) {
				custReg = dMappedRegistry;
			}
			for (Field field : dMappedRegistry.getClass().getDeclaredFields()) {
				if (field.getType() == DefaultedMappedRegistry.class || field.getType() == MappedRegistry.class) {
					field.setAccessible(true);
					try {
						DefaultedRegistry<EntityType<?>> reg = (DefaultedRegistry<EntityType<?>>) field.get(dMappedRegistry);

						if (!reg.getClass().getName().equals(DefaultedMappedRegistry.class.getName())) {
							reg = getRegistry(reg);
						}
						return reg;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return dMappedRegistry;
	}

	protected void overwriteEntityID(EntityType types, int id, DefaultedRegistry<EntityType<?>> entityRegistry) {
		try {
			Field bgF = MappedRegistry.class.getDeclaredField("d"); //This is toId
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
		EntityType<?> types = entityRegistry.get(ResourceLocation.tryParse(type.getTypeID().toString())).get().value();
		return entityRegistry.getId(types);
	}
}
