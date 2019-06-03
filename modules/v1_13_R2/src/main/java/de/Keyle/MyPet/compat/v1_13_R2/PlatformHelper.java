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

package de.Keyle.MyPet.compat.v1_13_R2;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.compat.v1_13_R2.util.FieldCompat;
import de.Keyle.MyPet.compat.v1_13_R2.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.UnsafeList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.*;

import java.util.List;

@Compat("v1_13_R2")
public class PlatformHelper extends de.Keyle.MyPet.api.PlatformHelper {

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
    public void playParticleEffect(Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, de.Keyle.MyPet.api.compat.Compat<Object> data) {
        Particle effect = IRegistry.PARTICLE_TYPE.get(new MinecraftKey(effectName));

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        ParticleParam particle = null;

        if (effect.f() != null && data != null) {
            try {
                //noinspection unchecked
                particle = effect.f().b(effect, new StringReader(" " + data.get().toString()));
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        } else if (effect instanceof ParticleType) {
            particle = (ParticleType) effect;
        }
        if (particle == null) {
            return;
        }

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(particle, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);
        radius = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {
            if ((int) MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), location) <= radius) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
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
    public void playParticleEffect(Player player, Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, de.Keyle.MyPet.api.compat.Compat<Object> data) {
        Particle effect = IRegistry.PARTICLE_TYPE.get(new MinecraftKey(effectName));

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        ParticleParam particle = null;

        if (effect.f() != null && data != null) {
            try {
                //noinspection unchecked
                particle = effect.f().b(effect, new StringReader(" " + data.get().toString()));
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        } else if (effect instanceof ParticleType) {
            particle = (ParticleType) effect;
        }
        if (particle == null) {
            return;
        }

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(particle, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);

        if (MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), location) <= radius) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    public boolean canSpawn(Location loc, MyPetMinecraftEntity entity) {
        return canSpawn(loc, ((EntityLiving) entity).getBoundingBox());
    }

    public Boolean canSpawn(Location loc, AxisAlignedBB bb) {
        net.minecraft.server.v1_13_R2.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        return getBlockBBsInBB(mcWorld, bb).isEmpty() && !mcWorld.containsLiquid(bb);
    }

    @SuppressWarnings("unchecked")
    public List getBlockBBsInBB(net.minecraft.server.v1_13_R2.World world, AxisAlignedBB axisalignedbb) {
        UnsafeList unsafeList = new UnsafeList();
        int minX;
        int maxX;
        int minY;
        int maxY;
        int minZ;
        int maxZ;

        if (FieldCompat.AxisAlignedBB_Fields.get()) {
            minX = MathHelper.floor((Double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_minX.get(), axisalignedbb));
            maxX = (int) Math.ceil((Double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_maxX.get(), axisalignedbb));
            minY = MathHelper.floor((Double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_minY.get(), axisalignedbb));
            maxY = (int) Math.ceil((Double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_maxY.get(), axisalignedbb));
            minZ = MathHelper.floor((Double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_minZ.get(), axisalignedbb));
            maxZ = (int) Math.ceil((Double) ReflectionUtil.getFieldValue(FieldCompat.AxisAlignedBB_maxZ.get(), axisalignedbb));
        } else {
            minX = MathHelper.floor(axisalignedbb.minX);
            maxX = (int) Math.ceil(axisalignedbb.maxX);
            minY = MathHelper.floor(axisalignedbb.minY);
            maxY = (int) Math.ceil(axisalignedbb.maxY);
            minZ = MathHelper.floor(axisalignedbb.minZ);
            maxZ = (int) Math.ceil(axisalignedbb.maxZ);
        }

        VoxelShape vec3d;
        boolean isEmpty;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (((ChunkProviderServer) world.getChunkProvider()).isLoaded(x >> 4, z >> 4)) {
                    for (int y = minY - 1; y <= maxY; y++) {
                        BlockPosition bp = new BlockPosition(x, y, z);
                        IBlockData blockData = world.getType(bp);
                        if (blockData != null && blockData.getMaterial().isSolid()) {
                            if (FieldCompat.AxisAlignedBB_Fields.get()) {
                                try {
                                    vec3d = (VoxelShape) FieldCompat.IBlockData_getCollisionShape.get().invoke(blockData, world, bp);
                                    isEmpty = (boolean) FieldCompat.VoxelShape_isEmpty.get().invoke(vec3d);
                                } catch (Exception e) {
                                    vec3d = null;
                                    isEmpty = true;
                                }
                            } else {
                                vec3d = blockData.getCollisionShape(world, bp);
                                isEmpty = vec3d.isEmpty();
                            }
                            if (!isEmpty) {
                                for (AxisAlignedBB bb : vec3d.d()) {
                                    if (bb.a(bp).c(axisalignedbb)) {
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

    public String getPlayerLanguage(Player player) {
        String locale = player.getLocale();
        if (locale == null || locale.equals("")) {
            return "en_us";
        }
        return locale;
    }

    @Override
    public TagCompound entityToTag(Entity bukkitEntity) {
        net.minecraft.server.v1_13_R2.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound vanillaNBT = new NBTTagCompound();

        entity.save(vanillaNBT);

        return (TagCompound) ItemStackNBTConverter.vanillaCompoundToCompound(vanillaNBT);
    }

    @Override
    public void applyTagToEntity(TagCompound tag, Entity bukkitEntity) {
        net.minecraft.server.v1_13_R2.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound vanillaNBT = (NBTTagCompound) ItemStackNBTConverter.compoundToVanillaCompound(tag);

        // Just a temporary fix until I come up with a better solution
        if (bukkitEntity instanceof Villager) {
            EntityVillager villager = (EntityVillager) entity;

            villager.setProfession(vanillaNBT.getInt("Profession"));
            villager.riches = vanillaNBT.getInt("Riches");
            villager.careerId = vanillaNBT.getInt("Career");
            ReflectionUtil.setFieldValue("bQ", villager, vanillaNBT.getInt("CareerLevel"));
            ReflectionUtil.setFieldValue("bM", villager, vanillaNBT.getBoolean("Willing"));
            if (vanillaNBT.hasKeyOfType("Offers", 10)) {
                NBTTagCompound nbttaglist = vanillaNBT.getCompound("Offers");
                ReflectionUtil.setFieldValue("trades", villager, new MerchantRecipeList(nbttaglist));
            }

            NBTTagList invTag = vanillaNBT.getList("Inventory", 10);

            for (int i = 0; i < invTag.size(); ++i) {
                ItemStack itemstack = ItemStack.a(invTag.getCompound(i));
                villager.inventory.a(itemstack);
            }

            villager.p(true);

            if (villager.isBaby()) {
                villager.goalSelector.a(8, new PathfinderGoalPlay(villager, 0.32D));
            } else if (villager.getProfession() == 0) {
                villager.goalSelector.a(6, new PathfinderGoalVillagerFarm(villager, 0.6D));
            }
        }

        // can not be used in 1.10
        //entity.f(vanillaNBT);
    }

    @Override
    public TagCompound itemStackToCompund(org.bukkit.inventory.ItemStack itemStack) {
        return ItemStackNBTConverter.itemStackToCompound(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack compundToItemStack(TagCompound compound) {
        return CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compoundToItemStack(compound));
    }

    public void sendMessageRaw(Player player, String message) {
        if (player instanceof CraftPlayer) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(message)));
        }
    }

    public void sendMessageActionBar(Player player, String message) {
        if (player instanceof CraftPlayer) {
            IChatBaseComponent cbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + Util.escapeJsonString(message) + "\"}");
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(cbc, ChatMessageType.GAME_INFO));
        }
    }

    public void addZombieTargetGoal(Zombie zombie) {
    }

    @Override
    public boolean comparePlayerWithEntity(MyPetPlayer player, Object obj) {
        if (obj instanceof EntityHuman && player != null && player.getPlayer() != null) {
            EntityHuman entityHuman = (EntityHuman) obj;
            return player.getPlayer().getUniqueId().equals(entityHuman.getUniqueID());
        }
        return false;
    }

    @Override
    public boolean isEquipment(org.bukkit.inventory.ItemStack itemStack) {
        {
            ItemStack itemstack = CraftItemStack.asNMSCopy(itemStack);
            int slot = EntityInsentient.e(itemstack).c();
            if (slot == 0) {
                if (itemstack.getItem() instanceof ItemSword) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemAxe) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemSpade) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemHoe) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemPickaxe) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemBow) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemShield) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemTrident) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemFishingRod) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemCompass) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemClock) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemCarrotStick) {
                    return true;
                } else if (itemstack.getItem() instanceof ItemSign) {
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    @Override
    public String getVanillaName(org.bukkit.inventory.ItemStack bukkitItemStack) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItemStack);
        return itemStack.getItem().getName();
    }

    @Override
    public void doPickupAnimation(Entity entity, Entity target) {
        int count = 1;
        if (target instanceof EntityItem) {
            count = ((EntityItem) target).getItemStack().getCount();
        }
        for (Entity p : target.getNearbyEntities(10, 10, 10)) {
            if (p instanceof Player) {
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutCollect(target.getEntityId(), entity.getEntityId(), count));
            }
        }
    }

    @Override
    public Entity getEntity(int id, World world) {
        net.minecraft.server.v1_13_R2.Entity e = ((CraftWorld) world).getHandle().getEntity(id);
        return e != null ? e.getBukkitEntity() : null;
    }

    public org.bukkit.inventory.ItemStack asBukkitItemStack(ItemStack itemStack) {
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    public ItemStack asNmsItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public net.minecraft.server.v1_13_R2.World getWorldNMS(World world) {
        return ((CraftWorld) world).getHandle();
    }

    @Override
    public void strikeLightning(Location loc, float distance) {
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
        EntityLightning lightning = new EntityLightning(world, loc.getX(), loc.getY(), loc.getZ(), true);
        world.getServer()
                .getServer()
                .getPlayerList()
                .sendPacketNearby(null, loc.getX(), loc.getY(), loc.getZ(), distance, world.dimension,
                        new PacketPlayOutSpawnEntityWeather(lightning));
        world.getServer()
                .getServer()
                .getPlayerList()
                .sendPacketNearby(null, loc.getX(), loc.getY(), loc.getZ(), distance, world.dimension,
                        new PacketPlayOutNamedSoundEffect(SoundEffects.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, loc.getX(), loc.getY(), loc.getZ(), distance, 1F));
    }

    public String getLastDamageSource(LivingEntity e) {
        EntityLiving el = ((CraftLivingEntity) e).getHandle();
        if (el.cr() == null) {
            return null;
        }
        return ((ChatMessage) el.cr().getLocalizedDeathMessage(el)).k();
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