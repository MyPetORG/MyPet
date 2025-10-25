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

package de.Keyle.MyPet.compat.v1_8_R3;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;
import de.Keyle.MyPet.api.util.inventory.material.MaterialHolder;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_8_R3.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import net.minecraft.server.v1_8_R3.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftZombie;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

@Compat("v1_8_R3")
public class PlatformHelper extends de.Keyle.MyPet.api.PlatformHelper {

    private static Field goalSelectorField = ReflectionUtil.getField(EntityInsentient.class, "goalSelector");

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
        EnumParticle effect = EnumParticle.valueOf(effectName);

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        int[] intData = data != null ? (int[]) data.get() : new int[0];

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effect, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count, intData);
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
        EnumParticle effect = EnumParticle.valueOf(effectName);

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        int[] intData = data != null ? (int[]) data.get() : new int[0];

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effect, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count, intData);
        radius = radius * radius;

        if ((int) MyPetApi.getPlatformHelper().distanceSquared(player.getLocation(), location) <= radius) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    public boolean canSpawn(Location loc, MyPetMinecraftEntity entity) {
        return canSpawn(loc, ((EntityLiving) entity).getBoundingBox());
    }

    public Boolean canSpawn(Location loc, AxisAlignedBB bb) {
        net.minecraft.server.v1_8_R3.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        return getBlockBBsInBB(loc.getWorld(), bb).isEmpty() && !mcWorld.containsLiquid(bb);
    }

    @SuppressWarnings("unchecked")
    public List getBlockBBsInBB(World world, AxisAlignedBB axisalignedbb) {
        UnsafeList unsafeList = new UnsafeList();
        int minX = MathHelper.floor(axisalignedbb.a);
        int maxX = MathHelper.floor(axisalignedbb.d + 1.0D);
        int minY = MathHelper.floor(axisalignedbb.b);
        int maxY = MathHelper.floor(axisalignedbb.e + 1.0D);
        int minZ = MathHelper.floor(axisalignedbb.c);
        int maxZ = MathHelper.floor(axisalignedbb.f + 1.0D);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (world.isChunkLoaded(x >> 4, z >> 4)) {
                    for (int y = minY - 1; y <= maxY; y++) {
                        Block block = CraftMagicNumbers.getBlock(world.getBlockAt(x, y, z));
                        if (block != null && block.getMaterial().isSolid()) {
                            block.a(((CraftWorld) world).getHandle(), new BlockPosition(x, y, z), block.getBlockData(), axisalignedbb, unsafeList, null);
                        }
                    }
                }
            }
        }
        return unsafeList;
    }

    public String getPlayerLanguage(Player player) {
        if (!(player instanceof CraftPlayer)) {
            return "en_US";
        }
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        try {
            Field field = entityPlayer.getClass().getDeclaredField("locale");
            String lang = field.get(entityPlayer).toString();
            if (lang == null) {
                return "en_US";
            }
            return lang;
        } catch (Exception e) {
            return "en_US";
        }
    }

    @Override
    public TagCompound entityToTag(Entity bukkitEntity) {
        net.minecraft.server.v1_8_R3.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound vanillaNBT = new NBTTagCompound();

        entity.e(vanillaNBT);

        return (TagCompound) ItemStackNBTConverter.vanillaCompoundToCompound(vanillaNBT);
    }

    @Override
    public void applyTagToEntity(TagCompound tag, Entity bukkitEntity) {
        net.minecraft.server.v1_8_R3.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound vanillaNBT = (NBTTagCompound) ItemStackNBTConverter.compoundToVanillaCompound(tag);
        entity.f(vanillaNBT);
    }

    @Override
    public TagCompound itemStackToCompund(org.bukkit.inventory.ItemStack itemStack) {
        return ItemStackNBTConverter.itemStackToCompound(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack compundToItemStack(TagCompound compound) {
        return CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compoundToItemStack(compound));
    }

    public void sendMessageActionBar(Player player, String message) {
        if (player instanceof CraftPlayer) {
            IChatBaseComponent cbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + Util.escapeJsonString(message) + "\"}");
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(cbc, (byte) 2));
        }
    }

    public void addZombieTargetGoal(Zombie zombie) {
        EntityZombie ez = ((CraftZombie) zombie).getHandle();
        if (goalSelectorField != null) {
            try {
                PathfinderGoalSelector pgs = (PathfinderGoalSelector) goalSelectorField.get(ez);
                pgs.a(3, new PathfinderGoalMeleeAttack(ez, EntityMyPet.class, 1.0D, true));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
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
            int slot = EntityInsentient.c(itemstack);
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
                } else if (itemstack.getItem() instanceof ItemFishingRod) {
                    return true;
                } else if (itemstack.getItem() == Items.COMPASS) {
                    return true;
                } else if (itemstack.getItem() == Items.CLOCK) {
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
        return itemStack.getItem().e_(itemStack) + ".name";
    }

    @Override
    public void doPickupAnimation(Entity entity, Entity target) {
        for (Entity p : target.getNearbyEntities(10, 10, 10)) {
            if (p instanceof Player) {
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutCollect(target.getEntityId(), entity.getEntityId()));
            }
        }
    }

    @Override
    public Entity getEntity(int id, World world) {
        net.minecraft.server.v1_8_R3.Entity e = ((CraftWorld) world).getHandle().a(id);
        return e != null ? e.getBukkitEntity() : null;
    }

    public org.bukkit.inventory.ItemStack asBukkitItemStack(ItemStack itemStack) {
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    public ItemStack asNmsItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public net.minecraft.server.v1_8_R3.World getWorldNMS(World world) {
        return ((CraftWorld) world).getHandle();
    }

    public Material getMaterial(MaterialHolder materialHolder) {
        Material mat = Material.getMaterial(materialHolder.getLegacyId().getId());
        if (mat == null) {
            mat = Material.matchMaterial(materialHolder.getLegacyName().getName());
        }
        return mat;
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
                        new PacketPlayOutNamedSoundEffect("ambient.weather.thunder", loc.getX(), loc.getY(), loc.getZ(), distance, 1F));
    }

    @Override
    public Entity getEntityByUUID(UUID uuid) {
        net.minecraft.server.v1_8_R3.Entity e = ((CraftServer) Bukkit.getServer()).getServer().a(uuid);
        return e != null ? e.getBukkitEntity() : null;
    }

    public String getLastDamageSource(LivingEntity e) {
        EntityLiving el = ((CraftLivingEntity) e).getHandle();
        if (!(el.combatTracker.b() instanceof ChatMessage)) {
            return null;
        }
        return ((ChatMessage) el.combatTracker.b()).i();
    }

    @Override
    public String itemstackToString(org.bukkit.inventory.ItemStack itemStack) {
        ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
        String itemstack = itemDatabase.getByLegacyId(itemStack.getTypeId(), itemStack.getData().getData()).getId();
        if (itemStack.hasItemMeta()) {
            itemstack += " " + CraftItemStack.asNMSCopy(itemStack).getTag().toString();
        }
        return itemstack;
    }

    public boolean gameruleDoDeathMessages(LivingEntity entity) {
        return Boolean.valueOf(entity.getWorld().getGameRuleValue("showDeathMessages"));
    }
}