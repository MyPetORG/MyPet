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

package de.Keyle.MyPet.compat.v1_17_R1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_17_R1.util.UnsafeList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.compat.v1_17_R1.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FoodOnAStickItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

@Compat("v1_17_R1")
public class PlatformHelper extends de.Keyle.MyPet.api.PlatformHelper {

    private static final Method ENTITY_LIVING_cD = ReflectionUtil.getMethod(net.minecraft.world.entity.LivingEntity.class, "cT");
    private static final Method CHAT_MESSAGE_k = ReflectionUtil.getMethod(TranslatableComponent.class, "k");

    /**
     * @param location   the {@link Location} around which players must be to see the effect
     * @param effectName list of effects: https://gist.github.com/riking/5759002
     * @param offsetX    the amount to be randomly offset by in the X axis
     * @param offsetY    the amount to be randomly offset by in the Y axis
     * @param offsetZ    the amount to be randomly offset by in the Z axis
     * @param speed      the speed of the particles
     * @param count      the number of particles
     * @param radius     the radius around the location
     */
    @Override
    public void playParticleEffect(Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, de.Keyle.MyPet.api.compat.Compat<Object> data) {
        ParticleType effect = Registry.PARTICLE_TYPE.get(new ResourceLocation(effectName));

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        ParticleOptions particle = null;

        if (effect.getDeserializer() != null && data != null) {
            try {
                particle = effect.getDeserializer().fromCommand(effect, new StringReader(" " + data.get().toString()));
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        } else if (effect instanceof SimpleParticleType) {
            particle = (SimpleParticleType) effect;
        }
        if (particle == null) {
            return;
        }

        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(particle, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);
        radius = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {
            if ((int) MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), location) <= radius) {
                ((CraftPlayer) player).getHandle().connection.send(packet);
            }
        }

    }

    /**
     * @param location   the {@link Location} around which players must be to see the effect
     * @param effectName list of effects: https://gist.github.com/riking/5759002
     * @param offsetX    the amount to be randomly offset by in the X axis
     * @param offsetY    the amount to be randomly offset by in the Y axis
     * @param offsetZ    the amount to be randomly offset by in the Z axis
     * @param speed      the speed of the particles
     * @param count      the number of particles
     * @param radius     the radius around the location
     */
    @Override
    public void playParticleEffect(Player player, Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, de.Keyle.MyPet.api.compat.Compat<Object> data) {
    	ParticleType effect = Registry.PARTICLE_TYPE.get(new ResourceLocation(effectName));

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        ParticleOptions particle = null;

        if (effect.getDeserializer() != null && data != null) {
            try {
                particle = effect.getDeserializer().fromCommand(effect, new StringReader(" " + data.get().toString()));
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        } else if (effect instanceof SimpleParticleType) {
            particle = (SimpleParticleType) effect;
        }
        if (particle == null) {
            return;
        }

        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(particle, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);

        if (MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), location) <= radius) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }

    @Override
    public boolean canSpawn(Location loc, MyPetMinecraftEntity entity) {
        return canSpawn(loc, ((net.minecraft.world.entity.LivingEntity) entity).getBoundingBox());
    }

    public Boolean canSpawn(Location loc, AABB bb) {
        net.minecraft.world.level.Level mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        return getBlockBBsInBB(mcWorld, bb).isEmpty() && !mcWorld.containsAnyLiquid(bb);
    }

    public List getBlockBBsInBB(net.minecraft.world.level.Level world, AABB axisalignedbb) {
        UnsafeList unsafeList = new UnsafeList();

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
                if (((ServerChunkCache) world.getChunkSource()).hasChunk(x >> 4, z >> 4)) {
                    for (int y = minY - 1; y <= maxY; y++) {
                        BlockPos bp = new BlockPos(x, y, z);
                        BlockState blockData = world.getBlockState(bp);
                        if (blockData != null && blockData.getMaterial().isSolid()) {
                            vec3d = blockData.getCollisionShape(world, bp);
                            isEmpty = vec3d.isEmpty();
                            if (!isEmpty) {
                                for (AABB bb : vec3d.toAabbs()) {
                                    if (bb.move(bp).intersects(axisalignedbb)) {
                                        unsafeList.add(bb);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return unsafeList;
    }

    @Override
    public String getPlayerLanguage(Player player) {
        String locale = player.getLocale();
        if (locale == null || locale.equals("")) {
            return "en_us";
        }
        return locale;
    }

    @Override
    public TagCompound entityToTag(Entity bukkitEntity) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        CompoundTag vanillaNBT = new CompoundTag();

        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            ((net.minecraft.world.entity.LivingEntity) entity).addAdditionalSaveData(vanillaNBT);
        } else {
            Method b = ReflectionUtil.getMethod(entity.getClass(), "b", CompoundTag.class);
            try {
                b.invoke(entity, vanillaNBT);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return (TagCompound) ItemStackNBTConverter.vanillaCompoundToCompound(vanillaNBT);
    }

    @Override
    public void applyTagToEntity(TagCompound tag, Entity bukkitEntity) {
        net.minecraft.world.entity.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        CompoundTag vanillaNBT = (CompoundTag) ItemStackNBTConverter.compoundToVanillaCompound(tag);
        if (vanillaNBT != null) {
            if (bukkitEntity instanceof Villager) {
            	net.minecraft.world.entity.npc.Villager villager = (net.minecraft.world.entity.npc.Villager) entity;
                villager.readAdditionalSaveData(vanillaNBT);
            } else if (bukkitEntity instanceof net.minecraft.world.entity.npc.WanderingTrader) {
            	net.minecraft.world.entity.npc.WanderingTrader villager = (net.minecraft.world.entity.npc.WanderingTrader) entity;
                villager.addAdditionalSaveData(vanillaNBT);
            }
        }
    }

    @Override
    public TagCompound itemStackToCompund(org.bukkit.inventory.ItemStack itemStack) {
        return ItemStackNBTConverter.itemStackToCompound(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack compundToItemStack(TagCompound compound) {
        return CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compoundToItemStack(compound));
    }

    @Override
    public void sendMessageRaw(Player player, String message) {
        if (player instanceof CraftPlayer) {
            ((CraftPlayer) player).getHandle().connection.send(new ClientboundChatPacket(Component.Serializer.fromJson(message), ChatType.CHAT, player.getUniqueId()));
        }
    }

    @Override
    public void sendMessageActionBar(Player player, String message) {
        if (player instanceof CraftPlayer) {
        	Component cbc = Component.Serializer.fromJson("{\"text\": \"" + Util.escapeJsonString(message) + "\"}");
            ((CraftPlayer) player).getHandle().connection.send(new ClientboundChatPacket(cbc, ChatType.GAME_INFO, player.getUniqueId()));
        }
    }

    @Override
    public void addZombieTargetGoal(Zombie zombie) {
    }

    @Override
    public boolean comparePlayerWithEntity(MyPetPlayer player, Object obj) {
        if (obj instanceof net.minecraft.world.entity.player.Player && player != null && player.getPlayer() != null) {
            net.minecraft.world.entity.player.Player entityHuman = (net.minecraft.world.entity.player.Player) obj;
            return player.getPlayer().getUniqueId().equals(entityHuman.getUUID());
        }
        return false;
    }

    @Override
    public boolean isEquipment(org.bukkit.inventory.ItemStack itemStack) {
        {
            ItemStack itemstack = CraftItemStack.asNMSCopy(itemStack);
            int slot = Mob.getEquipmentSlotForItem(itemstack).getFilterFlag();
            if (slot == 0) {
                if (itemstack.getItem() instanceof SwordItem) {
                    return true;
                } else if (itemstack.getItem() instanceof AxeItem) {
                    return true;
                } else if (itemstack.getItem() instanceof ShovelItem) {
                    return true;
                } else if (itemstack.getItem() instanceof HoeItem) {
                    return true;
                } else if (itemstack.getItem() instanceof PickaxeItem) {
                    return true;
                } else if (itemstack.getItem() instanceof BowItem) {
                    return true;
                } else if (itemstack.getItem() instanceof ShieldItem) {
                    return true;
                } else if (itemstack.getItem() instanceof TridentItem) {
                    return true;
                } else if (itemstack.getItem() instanceof FishingRodItem) {
                    return true;
                } else if (itemstack.getItem() instanceof CompassItem) {
                    return true;
                } else if (itemstack.getItem() instanceof FoodOnAStickItem) {
                    return true;
                } else if (itemstack.getItem() instanceof SignItem) {
                    return true;
                } else return itemstack.getItem() instanceof CrossbowItem;
            }
            return true;
        }
    }

    @Override
    public String getVanillaName(org.bukkit.inventory.ItemStack bukkitItemStack) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItemStack);
        return itemStack.getItem().getDescriptionId();
    }

    @Override
    public void doPickupAnimation(Entity entity, Entity target) {
        int count = 1;
        if (target instanceof ItemEntity) {
            count = ((ItemEntity) target).getItem().getCount();
        }
        for (Entity p : target.getNearbyEntities(10, 10, 10)) {
            if (p instanceof Player) {
                ((CraftPlayer) p).getHandle().connection.send(new ClientboundTakeItemEntityPacket(target.getEntityId(), entity.getEntityId(), count));
            }
        }
    }

    @Override
    public Entity getEntity(int id, World world) {
    	net.minecraft.world.entity.Entity e = ((CraftWorld) world).getHandle().getEntity(id);
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
    public void strikeLightning(Location loc, float distance) {
    	ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
    	LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
        lightning.setVisualOnly(true);
        lightning.moveTo(loc.getX(), loc.getY(), loc.getZ(), 0.0F, 0.0F);
        world.getCraftServer()
                .getServer()
                .getPlayerList()
                .broadcast(null, loc.getX(), loc.getY(), loc.getZ(), distance, world.dimension(),
                        new ClientboundAddEntityPacket(lightning));
        world.getCraftServer()
                .getServer()
                .getPlayerList()
                .broadcast(null, loc.getX(), loc.getY(), loc.getZ(), distance, world.dimension(),
                        new ClientboundSoundPacket(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, loc.getX(), loc.getY(), loc.getZ(), distance, 1F));
    }

    @Override
    public String getLastDamageSource(LivingEntity e) {
        net.minecraft.world.entity.LivingEntity el = ((CraftLivingEntity) e).getHandle();
        if (el.getLastDamageSource() == null) {
            return null;
        }
        return ((TranslatableComponent) el.getLastDamageSource().getLocalizedDeathMessage(el)).getKey();
    }

    @Override
    public String itemstackToString(org.bukkit.inventory.ItemStack itemStack) {
        ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
        String itemstack = itemDatabase.getByID(itemStack.getType().getKey().getKey()).getId();
        if (itemStack.hasItemMeta()) {
            itemstack += " " + CraftItemStack.asNMSCopy(itemStack).getTag().toString();
        }
        return itemstack;
    }
}
