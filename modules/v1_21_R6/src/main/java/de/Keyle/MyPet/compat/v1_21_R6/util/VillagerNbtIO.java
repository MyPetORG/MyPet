package de.Keyle.MyPet.compat.v1_21_R6.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.bukkit.craftbukkit.v1_21_R6.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R6.entity.CraftEntity;
import org.bukkit.entity.Entity;

public final class VillagerNbtIO {
    private static HolderLookup.Provider lookupFor(org.bukkit.World world) {
        // registry access needed by Value I/O
        RegistryAccess ra = ((CraftWorld) world).getHandle().registryAccess();
        return (HolderLookup.Provider) ra; // RegistryAccess implements Provider
    }

    /** Read/apply NBT into an existing villager */
    public static void readInto(Entity bukkitEntity, CompoundTag tag) {
        try {
            net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
            net.minecraft.world.entity.npc.Villager villager = (net.minecraft.world.entity.npc.Villager) entity;

            HolderLookup.Provider lookup = lookupFor(bukkitEntity.getWorld());
            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
            villager.load(input);
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
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
            villager.saveWithoutId(output);
            return output.buildResult();
        } catch (Exception e) {
            throw new RuntimeException("Could not write villager NBT", e);
        }
    }
}
