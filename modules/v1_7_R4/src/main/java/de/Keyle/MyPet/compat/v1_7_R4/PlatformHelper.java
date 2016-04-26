/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_7_R4;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_7_R4.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_7_R4.util.inventory.ItemStackNBTConverter;
import de.keyle.knbt.TagCompound;
import net.minecraft.server.v1_7_R4.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftZombie;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_7_R4.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_7_R4.util.UnsafeList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.lang.reflect.Field;
import java.util.List;

@Compat("v1_7_R4")
public class PlatformHelper extends de.Keyle.MyPet.api.PlatformHelper {
    private static Field goalSelectorField = null;

    static {
        try {
            goalSelectorField = EntityInsentient.class.getDeclaredField("goalSelector");
            goalSelectorField.setAccessible(true);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
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
    public void playParticleEffect(Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, int... data) {

        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effectName, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effectName, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().getWorld() == location.getWorld()) {
                if ((int) player.getLocation().distance(location) <= radius) {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
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
    public void playParticleEffect(Player player, Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius, int... data) {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effectName, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effectName, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);

        if (player.getLocation().getWorld() == location.getWorld()) {
            if ((int) player.getLocation().distance(location) <= radius) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }

    public boolean canSpawn(Location loc, MyPetMinecraftEntity entity) {
        return canSpawn(loc, ((EntityLiving) entity).boundingBox);
    }

    public Boolean canSpawn(Location loc, AxisAlignedBB bb) {
        net.minecraft.server.v1_7_R4.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
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
                            block.a(((CraftWorld) world).getHandle(), x, y, z, axisalignedbb, unsafeList, null);
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
            if(lang == null) {
                return "en_US";
            }
            return lang;
        } catch (Exception e) {
            return "en_US";
        }
    }

    @Override
    public TagCompound entityToTag(Entity bukkitEntity) {
        net.minecraft.server.v1_7_R4.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound vanillaNBT = new NBTTagCompound();

        entity.e(vanillaNBT);

        return (TagCompound) ItemStackNBTConverter.vanillaCompoundToCompound(vanillaNBT);
    }

    @Override
    public void applyTagToEntity(TagCompound tag, Entity bukkitEntity) {
        net.minecraft.server.v1_7_R4.Entity entity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound vanillaNBT = (NBTTagCompound) ItemStackNBTConverter.compoundToVanillaCompound(tag);
        entity.f(vanillaNBT);
    }

    @Override
    public TagCompound itemStackToCompund(org.bukkit.inventory.ItemStack itemStack) {
        return ItemStackNBTConverter.itemStackToCompund(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack compundToItemStack(TagCompound compound) {
        return CraftItemStack.asBukkitCopy(ItemStackNBTConverter.compundToItemStack(compound));
    }

    public void sendMessageRaw(Player player, String message) {
        if (player instanceof CraftPlayer) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(ChatSerializer.a(message)));
        }
    }

    public void sendMessageActionBar(Player player, String message) {
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
        EntityHuman entityHuman = (EntityHuman) obj;
        if (MyPetApi.getPlugin().isInOnlineMode()) {
            return player.getPlayerUUID().equals(entityHuman.getUniqueID());
        } else {
            return entityHuman.getName().equals(player.getName());
        }
    }

    @Override
    public boolean isEquipment(org.bukkit.inventory.ItemStack itemStack) {
        {
            ItemStack itemstack = CraftItemStack.asNMSCopy(itemStack);
            int slot = EntityInsentient.b(itemstack);
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
                }
                return false;
            }
            return true;
        }
    }

    @Override
    public void doPickupAnimation(Entity entity, Entity target) {
        for (Entity p : target.getNearbyEntities(10, 10, 10)) {
            if (p instanceof Player) {
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutCollect(target.getEntityId(), entity.getEntityId()));
            }
        }
    }

    public org.bukkit.inventory.ItemStack asBukkitItemStack(ItemStack itemStack) {
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    public ItemStack asNmsItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public net.minecraft.server.v1_7_R4.World getWorldNMS(World world) {
        return ((CraftWorld) world).getHandle();
    }
}