/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.configuration;

import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_5.NBTBase;
import net.minecraft.server.v1_4_5.NBTTagCompound;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

public class NBTConfiguration
{
    private File NBTFile;
    private NBTTagCompound nbtTagCompound = new NBTTagCompound();

    public NBTConfiguration(String path)
    {
        this(new File(path));
    }

    public NBTConfiguration(File file)
    {
        NBTFile = file;
        if (!NBTFile.exists())
        {
            try
            {
                NBTFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        file.setWritable(true);
        file.setReadable(true);
    }

    public NBTTagCompound getNBTTagCompound()
    {
        return nbtTagCompound;
    }

    public boolean save()
    {
        try
        {
            DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(NBTFile)));

            NBTBase.a(nbtTagCompound, outputStream);
            outputStream.close();
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public void load()
    {
        try
        {
            FileInputStream inputStream = new FileInputStream(NBTFile);
            inputStream.read();
            if (inputStream.read() != -1)
            {
                inputStream.close();
                inputStream = new FileInputStream(NBTFile);
                DataInputStream F_In = new DataInputStream(new BufferedInputStream(new GZIPInputStream(inputStream)));
                nbtTagCompound = (NBTTagCompound) NBTBase.b(F_In);
                F_In.close();
                MyPetUtil.getDebugLogger().info("loaded GZIP NBTfile");
            }
            inputStream.close();
            return;
        }
        catch (ZipException ignored)
        {
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            FileInputStream inputStream = new FileInputStream(NBTFile);
            inputStream.read();
            if (inputStream.read() != -1)
            {
                inputStream.close();
                inputStream = new FileInputStream(NBTFile);
                DataInputStream F_In;
                F_In = new DataInputStream(inputStream);
                nbtTagCompound = (NBTTagCompound) NBTBase.b(F_In);
                F_In.close();
                MyPetUtil.getDebugLogger().info("loaded unziped NBTfile");
            }
            inputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void clearConfig()
    {
        nbtTagCompound = new NBTTagCompound();
    }
}