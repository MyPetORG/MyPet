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

package de.Keyle.MyPet.compat.v1_9_R1.entity;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_9_R1.entity.types.*;
import net.minecraft.server.v1_9_R1.EntityTypes;
import net.minecraft.server.v1_9_R1.World;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static de.Keyle.MyPet.api.entity.MyPetType.*;

@Compat("v1_9_R1")
public class EntityRegistry extends de.Keyle.MyPet.api.entity.EntityRegistry {

    protected Map<MyPetType, Class<? extends MyPetMinecraftEntity>> entityClasses = new HashMap<>();

    public EntityRegistry() {
        entityClasses.put(Bat, EntityMyBat.class);
        entityClasses.put(Blaze, EntityMyBlaze.class);
        entityClasses.put(CaveSpider, EntityMyCaveSpider.class);
        entityClasses.put(Chicken, EntityMyChicken.class);
        entityClasses.put(Cow, EntityMyCow.class);
        entityClasses.put(Creeper, EntityMyCreeper.class);
        entityClasses.put(EnderDragon, EntityMyEnderDragon.class);
        entityClasses.put(Enderman, EntityMyEnderman.class);
        entityClasses.put(Endermite, EntityMyEndermite.class);
        entityClasses.put(Ghast, EntityMyGhast.class);
        entityClasses.put(Giant, EntityMyGiant.class);
        entityClasses.put(Guardian, EntityMyGuardian.class);
        entityClasses.put(Horse, EntityMyHorse.class);
        entityClasses.put(IronGolem, EntityMyIronGolem.class);
        entityClasses.put(MagmaCube, EntityMyMagmaCube.class);
        entityClasses.put(Mooshroom, EntityMyMooshroom.class);
        entityClasses.put(Ocelot, EntityMyOcelot.class);
        entityClasses.put(Pig, EntityMyPig.class);
        entityClasses.put(PigZombie, EntityMyPigZombie.class);
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
        entityClasses.put(Villager, EntityMyVillager.class);
        entityClasses.put(Zombie, EntityMyZombie.class);
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
        for (MyPetType type : entityClasses.keySet()) {
            registerEntityType(type, entityClasses.get(type));
        }
    }

    @SuppressWarnings("unchecked")
    public void registerEntityType(MyPetType type, Class<? extends MyPetMinecraftEntity> entityClass) {
        try {
            Field EntityTypes_d = ReflectionUtil.getField(EntityTypes.class, "d");
            Field EntityTypes_f = ReflectionUtil.getField(EntityTypes.class, "f");

            Map<Class, String> d = (Map) EntityTypes_d.get(EntityTypes_d);
            Map<Class, Integer> f = (Map) EntityTypes_f.get(EntityTypes_f);

            Iterator cIterator = d.keySet().iterator();
            while (cIterator.hasNext()) {
                Class clazz = (Class) cIterator.next();
                if (clazz.getCanonicalName().equals(entityClass.getCanonicalName())) {
                    cIterator.remove();
                }
            }

            Iterator eIterator = f.keySet().iterator();
            while (eIterator.hasNext()) {
                Class clazz = (Class) eIterator.next();
                if (clazz.getCanonicalName().equals(entityClass.getCanonicalName())) {
                    eIterator.remove();
                }
            }

            f.put(entityClass, (Integer) type.getTypeID());

        } catch (Exception e) {
            MyPetApi.getLogger().warning("Error while registering " + entityClass.getCanonicalName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterEntityTypes() {
        try {
            Field EntityTypes_d = ReflectionUtil.getField(EntityTypes.class, "d");
            Field EntityTypes_f = ReflectionUtil.getField(EntityTypes.class, "f");

            Map<Class, String> d = (Map) EntityTypes_d.get(EntityTypes_d);
            Map<Class, Integer> f = (Map) EntityTypes_f.get(EntityTypes_f);

            Iterator dIterator = d.keySet().iterator();
            while (dIterator.hasNext()) {
                Class clazz = (Class) dIterator.next();
                if (clazz.getCanonicalName().startsWith("de.Keyle.MyPet")) {
                    dIterator.remove();
                }
            }

            Iterator fIterator = f.keySet().iterator();
            while (fIterator.hasNext()) {
                Class clazz = (Class) fIterator.next();
                if (clazz.getCanonicalName().startsWith("de.Keyle.MyPet")) {
                    fIterator.remove();
                }
            }
        } catch (Exception e) {
            MyPetApi.getLogger().warning("Error while unregistering MyPet entities");
        }
    }
}