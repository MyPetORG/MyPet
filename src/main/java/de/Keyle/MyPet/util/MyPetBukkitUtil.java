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

import net.minecraft.server.v1_5_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_5_R3.util.UnsafeList;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public class MyPetBukkitUtil
{
    public static String setColors(String text)
    {
        for (ChatColor color : ChatColor.values())
        {
            text = text.replace("%" + color.name().replace("_", "").toLowerCase() + "%", color.toString());
        }
        return text;
    }

    public static String stripColors(String text)
    {
        return text.replaceAll("§[abcdefklmnor0-9]", "");
    }

    public static Material checkMaterial(int itemid, Material defaultMaterial)
    {
        if (Material.getMaterial(itemid) == null)
        {
            return defaultMaterial;
        }
        else
        {
            return Material.getMaterial(itemid);
        }
    }

    public static boolean isValidMaterial(int itemid)
    {
        return Material.getMaterial(itemid) != null;
    }

    public static void sendMessage(Player player, String Message)
    {
        if (player != null && player.isOnline())
        {
            player.sendMessage(Message);
        }
    }

    public static Boolean canSpawn(Location loc, Entity entity)
    {
        return canSpawn(loc, entity.width, entity.height, entity.length);
    }

    public static Boolean canSpawn(Location loc, float width, float height, float length)
    {
        net.minecraft.server.v1_5_R3.World mcWorld = ((CraftWorld) loc.getWorld()).getHandle();
        float halfEntityWidth = width / 2;
        AxisAlignedBB bb = AxisAlignedBB.a(loc.getX() - halfEntityWidth, loc.getY() - height, loc.getZ() - halfEntityWidth, loc.getX() + halfEntityWidth, loc.getY() - height + length, loc.getZ() + halfEntityWidth);

        return getBlockBBsInBB(loc.getWorld(), bb).isEmpty() && !mcWorld.containsLiquid(bb);
    }

    public static List getBlockBBsInBB(World world, AxisAlignedBB axisalignedbb)
    {
        UnsafeList unsafeList = new UnsafeList();
        int minX = MathHelper.floor(axisalignedbb.a);
        int maxX = MathHelper.floor(axisalignedbb.d + 1.0D);
        int minY = MathHelper.floor(axisalignedbb.b);
        int maxY = MathHelper.floor(axisalignedbb.e + 1.0D);
        int minZ = MathHelper.floor(axisalignedbb.c);
        int maxZ = MathHelper.floor(axisalignedbb.f + 1.0D);

        for (int x = minX ; x < maxX ; x++)
        {
            for (int z = minZ ; z < maxZ ; z++)
            {
                if (world.getChunkAt(x, z).isLoaded())
                {
                    for (int y = minY - 1 ; y < maxY ; y++)
                    {
                        Block block = Block.byId[world.getBlockAt(x, y, z).getTypeId()];

                        if (block != null)
                        {
                            block.a(((CraftWorld) world).getHandle(), x, y, z, axisalignedbb, unsafeList, null);
                        }
                    }
                }
            }
        }
        return unsafeList;
    }

    public static String getPlayerLanguage(Player player)
    {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        try
        {
            Field field = entityPlayer.getLocale().getClass().getDeclaredField("e");
            field.setAccessible(true);

            return (String) field.get(entityPlayer.getLocale());
        }
        catch (Exception e)
        {
            return "en_US";
        }
    }
}