/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import net.minecraft.server.v1_6_R2.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R2.util.UnsafeList;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public class BukkitUtil {

    static Field Packet63WorldParticles_a = Util.getField(Packet63WorldParticles.class, "a");
    static Field Packet63WorldParticles_b = Util.getField(Packet63WorldParticles.class, "b");
    static Field Packet63WorldParticles_c = Util.getField(Packet63WorldParticles.class, "c");
    static Field Packet63WorldParticles_d = Util.getField(Packet63WorldParticles.class, "d");
    static Field Packet63WorldParticles_e = Util.getField(Packet63WorldParticles.class, "e");
    static Field Packet63WorldParticles_f = Util.getField(Packet63WorldParticles.class, "f");
    static Field Packet63WorldParticles_g = Util.getField(Packet63WorldParticles.class, "g");
    static Field Packet63WorldParticles_h = Util.getField(Packet63WorldParticles.class, "h");
    static Field Packet63WorldParticles_i = Util.getField(Packet63WorldParticles.class, "i");

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
    public static void playParticleEffect(Location location, String effectName, float offsetX, float offsetY, float offsetZ, float speed, int count, int radius) {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(effectName, "Effect cannot be null");
        Validate.notNull(location.getWorld(), "World cannot be null");

        Packet63WorldParticles packet = new Packet63WorldParticles();

        Util.setFieldValue(Packet63WorldParticles_a, packet, effectName);
        Util.setFieldValue(Packet63WorldParticles_b, packet, (float) location.getX());
        Util.setFieldValue(Packet63WorldParticles_c, packet, (float) location.getY());
        Util.setFieldValue(Packet63WorldParticles_d, packet, (float) location.getZ());
        Util.setFieldValue(Packet63WorldParticles_e, packet, offsetX);
        Util.setFieldValue(Packet63WorldParticles_f, packet, offsetY);
        Util.setFieldValue(Packet63WorldParticles_g, packet, offsetZ);
        Util.setFieldValue(Packet63WorldParticles_h, packet, speed);
        Util.setFieldValue(Packet63WorldParticles_i, packet, count);

        for (Player player : location.getWorld().getPlayers()) {
            if ((int) player.getLocation().distance(location) <= radius) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
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
        return canSpawn(loc, entity.width, entity.height, entity.length);
    }

    public static Boolean canSpawn(Location loc, float width, float height, float length) {
        net.minecraft.server.v1_6_R2.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        float halfEntityWidth = width / 2;
        AxisAlignedBB bb = AxisAlignedBB.a(loc.getX() - halfEntityWidth, loc.getY() - height, loc.getZ() - halfEntityWidth, loc.getX() + halfEntityWidth, loc.getY() - height + length, loc.getZ() + halfEntityWidth);

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
                if (world.getChunkAt(x, z).isLoaded()) {
                    for (int y = minY - 1; y < maxY; y++) {
                        Block block = Block.byId[world.getBlockAt(x, y, z).getTypeId()];

                        if (block != null) {
                            block.a(((CraftWorld) world).getHandle(), x, y, z, axisalignedbb, unsafeList, null);
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
}