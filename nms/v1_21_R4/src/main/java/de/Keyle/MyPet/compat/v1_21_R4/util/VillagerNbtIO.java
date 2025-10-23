package de.Keyle.MyPet.compat.v1_21_R4.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftEntity;
import org.bukkit.entity.Entity;

public final class VillagerNbtIO {
    private static HolderLookup.Provider lookupFor(org.bukkit.World world) {
        // registry access needed by Value I/O
        RegistryAccess ra = ((CraftWorld) world).getHandle().registryAccess();
        return ra; // RegistryAccess implements Provider
    }

    /** Read/apply NBT into an existing villager */
    public static void readInto(Entity bukkitEntity, CompoundTag tag) {
        try {
            net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
            net.minecraft.world.entity.npc.Villager villager = (net.minecraft.world.entity.npc.Villager) entity;

            // Try to use the registry-aware TagValueInput API if present
            try {
                Class<?> tagValueInputClass = Class.forName("net.minecraft.world.level.storage.TagValueInput");
                Class<?> valueInputClass = Class.forName("net.minecraft.world.level.storage.ValueInput");
                Class<?> problemReporterClass = Class.forName("net.minecraft.util.ProblemReporter");

                Object discarding = problemReporterClass.getField("DISCARDING").get(null);
                HolderLookup.Provider lookup = lookupFor(bukkitEntity.getWorld());
                java.lang.reflect.Method createMethod = tagValueInputClass.getMethod("create", problemReporterClass, HolderLookup.Provider.class, CompoundTag.class);
                Object input = createMethod.invoke(null, discarding, lookup, tag);

                java.lang.reflect.Method loadMethod = villager.getClass().getMethod("load", valueInputClass);
                loadMethod.invoke(villager, input);
                return;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // TagValue API not available or method signatures differ; fall back
            }

            // Fallback: use legacy method readAdditionalSaveData(CompoundTag)
            try {
                java.lang.reflect.Method readMethod = villager.getClass().getMethod("readAdditionalSaveData", CompoundTag.class);
                readMethod.invoke(villager, tag);
                return;
            } catch (NoSuchMethodException nsme) {
                // try alternative name
            }

            // Last-resort fallback: try load(CompoundTag)
            try {
                java.lang.reflect.Method loadCompound = villager.getClass().getMethod("load", CompoundTag.class);
                loadCompound.invoke(villager, tag);
                return;
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Could not read villager NBT: no suitable read method found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read villager NBT", e);
        }
    }

    /** Write NBT out of a villager */
    public static CompoundTag writeFrom(Entity bukkitEntity) {
        try {
            net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
            net.minecraft.world.entity.npc.Villager villager = (net.minecraft.world.entity.npc.Villager) entity;

            HolderLookup.Provider lookup = lookupFor(bukkitEntity.getWorld());

            // Try to use the registry-aware TagValueOutput API if present
            try {
                Class<?> tagValueOutputClass = Class.forName("net.minecraft.world.level.storage.TagValueOutput");
                Class<?> valueOutputClass = Class.forName("net.minecraft.world.level.storage.ValueOutput");
                Class<?> problemReporterClass = Class.forName("net.minecraft.util.ProblemReporter");

                Object discarding = problemReporterClass.getField("DISCARDING").get(null);
                java.lang.reflect.Method createWithContext = tagValueOutputClass.getMethod("createWithContext", problemReporterClass, HolderLookup.Provider.class);
                Object output = createWithContext.invoke(null, discarding, lookup);

                // prefer saveWithoutId(ValueOutput) if available
                try {
                    java.lang.reflect.Method saveWithoutId = villager.getClass().getMethod("saveWithoutId", valueOutputClass);
                    saveWithoutId.invoke(villager, output);
                } catch (NoSuchMethodException ns) {
                    // try alternative method name
                    java.lang.reflect.Method saveMethod = villager.getClass().getMethod("saveWithoutId", Object.class);
                    saveMethod.invoke(villager, output);
                }

                java.lang.reflect.Method buildResult = output.getClass().getMethod("buildResult");
                Object result = buildResult.invoke(output);
                return (CompoundTag) result;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // TagValue API not available; fall back to legacy CompoundTag-based writing
            }

            // Fallback: use legacy method addAdditionalSaveData(CompoundTag) or saveWithoutId(CompoundTag)
            CompoundTag tag = new CompoundTag();
            try {
                java.lang.reflect.Method addMethod = villager.getClass().getMethod("addAdditionalSaveData", CompoundTag.class);
                addMethod.invoke(villager, tag);
                return tag;
            } catch (NoSuchMethodException nsme) {
                // try saveWithoutId(CompoundTag)
            }

            try {
                java.lang.reflect.Method saveMethod = villager.getClass().getMethod("saveWithoutId", CompoundTag.class);
                saveMethod.invoke(villager, tag);
                return tag;
            } catch (NoSuchMethodException ex2) {
                try {
                    java.lang.reflect.Method saveMethod2 = villager.getClass().getMethod("save", CompoundTag.class);
                    saveMethod2.invoke(villager, tag);
                    return tag;
                } catch (NoSuchMethodException ex3) {
                    throw new RuntimeException("No suitable villager write method found");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not write villager NBT", e);
        }
    }
}

