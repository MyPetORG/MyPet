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

package de.Keyle.MyPet.compat.v1_21_R7;

import com.mojang.brigadier.StringReader;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_21_R7.entity.EntityMyAquaticPet;
import de.Keyle.MyPet.compat.v1_21_R7.util.VillagerNbtIO;
import de.Keyle.MyPet.compat.v1_21_R7.util.inventory.ItemStackNBTConverter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlatformHelper extends de.Keyle.MyPet.api.PlatformHelper {

    public static final Field dragonPartsField = ReflectionUtil.getField(ServerLevel.class, "ad"); //Mojang Field: dragonParts
    private static final TagParser<?> TAG_PARSER_INSTANCE = TagParser.create(NbtOps.INSTANCE);
    private static final StackWalker leWalker = StackWalker.getInstance(Collections.singleton(StackWalker.Option.RETAIN_CLASS_REFERENCE), 4);
    private static final RegistryAccess REGISTRY_ACCESS = CraftRegistry.getMinecraftRegistry();
    private static Method readParticleMethod = ReflectionUtil.getMethod(ParticleArgument.class, "a", TagParser.class, StringReader.class, ParticleType.class, HolderLookup.Provider.class);

    @Override
    public boolean canSpawn(Location loc, MyPetMinecraftEntity entity) {
        return canSpawn(loc, ((net.minecraft.world.entity.LivingEntity) entity).getBoundingBox(), entity instanceof EntityMyAquaticPet);
    }

    public Boolean canSpawn(Location loc, AABB bb, boolean canSpawnUnderwater) {
        Level mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        if (canSpawnUnderwater) {
            return getBlockBBsInBB(mcWorld, bb).isEmpty();
        }
        return getBlockBBsInBB(mcWorld, bb).isEmpty() && !mcWorld.containsAnyLiquid(bb);
    }

    public List<AABB> getBlockBBsInBB(Level world, AABB axisalignedbb) {
        List<AABB> list = new ArrayList<>();

        int minX = Mth.floor(axisalignedbb.minX);
        int maxX = (int) Math.ceil(axisalignedbb.maxX);
        int minY = Mth.floor(axisalignedbb.minY);
        int maxY = (int) Math.ceil(axisalignedbb.maxY);
        int minZ = Mth.floor(axisalignedbb.minZ);
        int maxZ = (int) Math.ceil(axisalignedbb.maxZ);

        VoxelShape vec3d;
        boolean isEmpty;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (world.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                    for (int y = minY - 1; y <= maxY; y++) {
                        BlockPos bp = new BlockPos(x, y, z);
                        BlockState blockData = world.getBlockState(bp);
                        if (blockData != null && blockData.isSolid()) {
                            vec3d = blockData.getCollisionShape(world, bp);
                            isEmpty = vec3d.isEmpty();
                            if (!isEmpty) {
                                for (AABB bb : vec3d.toAabbs()) {
                                    if (bb.move(bp).intersects(axisalignedbb)) {
                                        list.add(bb);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    public CompoundBinaryTag entityToTag(Entity bukkitEntity) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        CompoundTag vanillaNBT = new CompoundTag();

        // For villagers and wandering traders we need to use the special NBT IO helpers
        try {
            if (bukkitEntity instanceof Villager || bukkitEntity instanceof WanderingTrader) {
                // VillagerNbtIO.writeFrom returns a CompoundTag -> use it
                CompoundTag written = VillagerNbtIO.writeFrom(bukkitEntity);
                if (written != null) {
                    vanillaNBT = written;
                }
            } else {
                HolderLookup.Provider lookup = ((CraftWorld) bukkitEntity.getWorld()).getHandle().registryAccess();
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
                entity.saveWithoutId(output);
                vanillaNBT = output.buildResult();
            }
        } catch (Exception e) {
            ErrorUtil.report(e);
        }

        return (CompoundBinaryTag) ItemStackNBTConverter.vanillaCompoundToCompound(vanillaNBT);
    }

    @Override
    public void applyTagToEntity(CompoundBinaryTag tag, Entity bukkitEntity) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        CompoundTag nbtData = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(tag);

        if (nbtData != null) {
            if (bukkitEntity instanceof Villager) {
                VillagerNbtIO.readInto(bukkitEntity, nbtData);
            } else if (bukkitEntity instanceof WanderingTrader) {
                // Wandering traders are handled the same way as villagers for NBT read
                VillagerNbtIO.readInto(bukkitEntity, nbtData);
            }
        }
    }

    @Override
    public CompoundBinaryTag itemStackToCompound(org.bukkit.inventory.ItemStack itemStack) {
        return ItemStackNBTConverter.itemStackToCompound(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack compoundToItemStack(CompoundBinaryTag compound) {
        return CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compoundToItemStack(compound));
    }


    @Override
    public void addZombieTargetGoal(Zombie zombie) {
    }

    @Override
    public boolean comparePlayerWithEntity(MyPetPlayer player, Object obj) {
        if (obj instanceof net.minecraft.world.entity.player.Player entityHuman && player != null && player.getPlayer() != null) {
            return player.getPlayer().getUniqueId().equals(entityHuman.getUUID());
        }
        return false;
    }

    @Override
    public boolean isEquipment(org.bukkit.inventory.ItemStack itemStack) {
        {
            ItemStack itemstack = CraftItemStack.asNMSCopy(itemStack);
            Equippable equipable = itemstack.get(DataComponents.EQUIPPABLE);
            EquipmentSlot slotRaw = equipable != null ? equipable.slot() : EquipmentSlot.MAINHAND;
            int slot = slotRaw.getId();
            if (slot == 0) {
                if (itemstack.getItem().components().has(DataComponents.WEAPON)) {
                    return true;
                }

                if (itemstack.getItem().components().has(DataComponents.TOOL)) {
                    return true;
                }

                return switch (itemstack.getItem()) {
                    case AxeItem axeItem -> true;
                    case ShovelItem shovelItem -> true;
                    case HoeItem hoeItem -> true;
                    case BowItem bowItem -> true;
                    case ShieldItem shieldItem -> true;
                    case TridentItem tridentItem -> true;
                    case FishingRodItem fishingRodItem -> true;
                    case CompassItem compassItem -> true;
                    case FoodOnAStickItem foodOnAStickItem -> true;
                    case SignItem signItem -> true;
                    default -> itemstack.getItem() instanceof CrossbowItem;
                };
            }
            return true;
        }
    }

    @Override
    public void doPickupAnimation(Entity entity, Entity target) {
        if (entity instanceof LivingEntity collector && target instanceof org.bukkit.entity.Item item) {
            collector.playPickupItemAnimation(item, item.getItemStack().getAmount());
        } else {
            for (Entity p : target.getNearbyEntities(10, 10, 10)) {
                if (p instanceof Player) {
                    ((CraftPlayer) p).getHandle().connection.send(
                            new ClientboundTakeItemEntityPacket(target.getEntityId(), entity.getEntityId(), 1));
                }
            }
        }
    }

    @Override
    public Entity getEntity(int id, World world) {
        net.minecraft.world.entity.Entity e = ((CraftWorld) world).getHandle().getEntities().get(id);
        if (e == null) {
            /*Int2ObjectMap dragonParts = (Int2ObjectMap) ReflectionUtil.getFieldValue(dragonPartsField, ((CraftWorld)world).getHandle());
            e = (net.minecraft.world.entity.Entity) dragonParts.get(id);*/
        }
        return e != null ? e.getBukkitEntity() : null;
    }

    public org.bukkit.inventory.ItemStack asBukkitItemStack(ItemStack itemStack) {
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    public ItemStack asNmsItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public Level getWorldNMS(World world) {
        return ((CraftWorld) world).getHandle();
    }

    @Override
    public String getLastDamageSource(LivingEntity e) {
        net.minecraft.world.entity.LivingEntity el = ((CraftLivingEntity) e).getHandle();
        if (el.getLastDamageSource() == null) {
            return null;
        }
        return ((TranslatableContents) el.getLastDamageSource().getLocalizedDeathMessage(el).getContents()).getKey();
    }

    @Override
    public String itemstackToString(org.bukkit.inventory.ItemStack itemStack) {
        ItemStack stack = CraftItemStack.asNMSCopy(itemStack);
        return ". " + ItemStackNBTConverter.itemStackToVanillaCompound(stack).toString();
    }

    @Override
    public boolean gameruleDoDeathMessages(LivingEntity entity) {
        return entity.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
    }

    @Override
    public boolean doStackWalking(Class<?> leClass, int oldDepth) {
        return leWalker.walk(s -> s.limit(oldDepth + 1).map(StackWalker.StackFrame::getDeclaringClass).anyMatch(leClass::equals));
    }

    private String parseNBTForEffect(String effectName) {
        return switch (effectName) {
            case "item" -> "item";
            case "block", "block_marker", "falling_dust", "dust_pillar" -> "block_state";
            case "entity_effect" -> "color";
            default -> "";
        };
    }
}
