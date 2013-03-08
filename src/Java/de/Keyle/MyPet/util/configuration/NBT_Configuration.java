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

package de.Keyle.MyPet.util.configuration;

import de.Keyle.MyPet.util.MyPetUtil;
import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.Tag;
import org.spout.nbt.stream.NBTInputStream;
import org.spout.nbt.stream.NBTOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NBT_Configuration
{
    private File NBTFile;
    private CompoundTag nbtTagCompound;

    public NBT_Configuration(File file)
    {
        NBTFile = file;
    }

    public CompoundTag getNBTCompound()
    {
        if (nbtTagCompound == null)
        {
            clearConfig();
        }
        return nbtTagCompound;
    }

    public boolean save()
    {
        try
        {
            OutputStream os = new FileOutputStream(NBTFile);
            NBTOutputStream nbtOutputStream = new NBTOutputStream(os);
            nbtOutputStream.writeTag(nbtTagCompound);
            nbtOutputStream.close();
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private List<Tag<?>> readRawNBT(File f, boolean compressed)
    {
        List<Tag<?>> tags = new ArrayList<Tag<?>>();
        try
        {
            InputStream is = new FileInputStream(f);
            NBTInputStream ns = new NBTInputStream(is, compressed);
            try
            {
                boolean eof = false;
                while (!eof)
                {
                    try
                    {
                        tags.add(ns.readTag());
                    }
                    catch (EOFException e)
                    {
                        eof = true;
                    }
                }
            }
            finally
            {
                try
                {
                    ns.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
        catch (IOException e)
        {
            return null;
        }
        return tags;
    }

    public void load()
    {
        if (!NBTFile.exists())
        {
            return;
        }
        List<Tag<?>> tags = readRawNBT(NBTFile, true);
        if (tags != null)
        {
            if (MyPetUtil.getDebugLogger() != null)
            {
                MyPetUtil.getDebugLogger().info("loaded compressed NBT file (" + NBTFile.getName() + ")");
            }
        }
        else
        {
            tags = readRawNBT(NBTFile, false);
            if (tags != null)
            {
                if (MyPetUtil.getDebugLogger() != null)
                {
                    MyPetUtil.getDebugLogger().info("loaded uncompressed NBT file (" + NBTFile.getName() + ")");
                }
            }
        }
        if (tags != null && tags.size() > 0)
        {
            nbtTagCompound = (CompoundTag) tags.get(0);
        }
    }

    public void clearConfig()
    {
        nbtTagCompound = new CompoundTag("root", new CompoundMap());
    }
}