/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.util.logger.DebugLogger;
import net.minecraft.server.v1_8_R1.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_8_R1.util.UnsafeList;
import org.bukkit.entity.Player;
import org.spigotmc.SpigotConfig;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BukkitUtil {
    /**
     * @param location   the {@link Location} around which players must be to see the effect
     * @param effect list of effects: https://gist.github.com/riking/5759002
     * @param offsetX    the amount to be randomly offset by in the X axis
     * @param offsetY    the amount to be randomly offset by in the Y axis
     * @param offsetZ    the amount to be randomly offset by in the Z axis
     * @param speed      the speed of the particles
     * @param count      the number of particles
     * @param radius     the radius around the location
     */
    public static void playParticleEffect(Location location, EnumParticle effect, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius) {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effect, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effect, false, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, count);

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().getWorld() == location.getWorld()) {
                if ((int) player.getLocation().distance(location) <= radius) {
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }
        }
    }

    public static Material checkMaterial(int itemid, Material defaultMaterial) {
        if (Material.getMaterial(itemid) == null) {
            return defaultMaterial;
        } else {
            return Material.getMaterial(itemid);
        }
    }

    public static boolean isValidMaterial(int itemid) {
        return Material.getMaterial(itemid) != null;
    }

    public static String getMaterialName(int itemId) {
        if (isValidMaterial(itemId)) {
            return Material.getMaterial(itemId).name();
        }
        return String.valueOf(itemId);
    }

    public static void sendMessage(Player player, String Message) {
        if (player != null && player.isOnline()) {
            player.sendMessage(Message);
        }
    }

    public static Boolean canSpawn(Location loc, Entity entity) {
        return canSpawn(loc, entity.getBoundingBox());
    }

    public static Boolean canSpawn(Location loc, AxisAlignedBB bb) {
        net.minecraft.server.v1_8_R1.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        return getBlockBBsInBB(loc.getWorld(), bb).isEmpty() && !mcWorld.containsLiquid(bb);
    }

    public static List getBlockBBsInBB(World world, AxisAlignedBB axisalignedbb) {
        UnsafeList unsafeList = new UnsafeList();
        int minX = MathHelper.floor(axisalignedbb.a);
        int maxX = MathHelper.floor(axisalignedbb.d + 1.0D);
        int minY = MathHelper.floor(axisalignedbb.b);
        int maxY = MathHelper.floor(axisalignedbb.e + 1.0D);
        int minZ = MathHelper.floor(axisalignedbb.c);
        int maxZ = MathHelper.floor(axisalignedbb.f + 1.0D);

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (world.isChunkLoaded(x >> 4, z >> 4)) {
                    for (int y = minY - 1; y < maxY; y++) {
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

    public static String getPlayerLanguage(Player player) {
        if (!(player instanceof CraftPlayer)) {
            return "en_US";
        }
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        try {
            Field field = entityPlayer.getClass().getDeclaredField("locale");
            field.setAccessible(true);

            return (String) field.get(entityPlayer);
        } catch (Exception e) {
            return "en_US";
        }
    }

    public static String getCommandSenderLanguage(CommandSender sender) {
        String lang = "en";
        if (sender instanceof Player) {
            lang = getPlayerLanguage((Player) sender);
        }
        return lang;
    }

    public static boolean isEquipment(ItemStack itemstack) {
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
            }
            return false;
        }
        return true;
    }

    private static Boolean bungee = null;

    public static boolean isInOnlineMode() {
        if (bungee == null) {
            try {
                bungee = SpigotConfig.bungee;
            } catch (NoClassDefFoundError ignored) {
                bungee = false;
            }
        }
        return bungee || Bukkit.getOnlineMode();
    }

    @SuppressWarnings("unchecked")
    public static boolean registerMyPetEntity(Class<? extends EntityMyPet> myPetEntityClass, String entityTypeName, int entityTypeId) {
        try {
            Field EntityTypes_d = EntityTypes.class.getDeclaredField("d");
            Field EntityTypes_f = EntityTypes.class.getDeclaredField("f");
            EntityTypes_d.setAccessible(true);
            EntityTypes_f.setAccessible(true);

            Map<Class, String> d = (Map) EntityTypes_d.get(EntityTypes_d);
            Map<Class, Integer> f = (Map) EntityTypes_f.get(EntityTypes_f);

            Iterator cIterator = d.keySet().iterator();
            while (cIterator.hasNext()) {
                Class clazz = (Class) cIterator.next();
                if (clazz.getCanonicalName().equals(myPetEntityClass.getCanonicalName())) {
                    cIterator.remove();
                }
            }

            Iterator eIterator = f.keySet().iterator();
            while (eIterator.hasNext()) {
                Class clazz = (Class) eIterator.next();
                if (clazz.getCanonicalName().equals(myPetEntityClass.getCanonicalName())) {
                    eIterator.remove();
                }
            }

            d.put(myPetEntityClass, entityTypeName);
            f.put(myPetEntityClass, entityTypeId);

            return true;
        } catch (Exception e) {
            DebugLogger.severe("error while registering " + myPetEntityClass.getCanonicalName());
            DebugLogger.severe(e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean unregisterMyPetEntities() {
        DebugLogger.info("Unregister MyPet entities");
        try {
            Field EntityTypes_d = EntityTypes.class.getDeclaredField("d");
            Field EntityTypes_f = EntityTypes.class.getDeclaredField("f");
            EntityTypes_d.setAccessible(true);
            EntityTypes_f.setAccessible(true);

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

            return true;
        } catch (Exception e) {
            DebugLogger.severe("error while unregistering MyPet entities");
            DebugLogger.severe(e.getMessage());
            return false;
        }
    }

    public static void sendMessageRaw(Player player, String message) {
        if (player instanceof CraftPlayer) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(ChatSerializer.a(message)));
        }
    }
}