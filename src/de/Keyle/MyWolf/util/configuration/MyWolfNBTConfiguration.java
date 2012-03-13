/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util.configuration;

import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagCompound;

import java.io.*;

public class MyWolfNBTConfiguration
{
    public File NBTFile;
    private NBTTagCompound nbtTagCompound = new NBTTagCompound();

    public MyWolfNBTConfiguration(String Path)
    {
        NBTFile = new File(Path);
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
    }

    public MyWolfNBTConfiguration(File f)
    {
        NBTFile = f;
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
    }

    public NBTTagCompound getNBTTagCompound()
    {
        return nbtTagCompound;
    }

    public boolean save()
    {
        try
        {
            DataOutputStream F_Out = new DataOutputStream(new FileOutputStream(NBTFile));
            NBTBase.a(nbtTagCompound, F_Out);
            F_Out.close();
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
            FileInputStream fi = new FileInputStream(NBTFile);
            fi.read();

            if (fi.read() != -1)
            {
                fi.close();
                fi = new FileInputStream(NBTFile);
                DataInputStream F_In = new DataInputStream(fi);
                nbtTagCompound = (NBTTagCompound) NBTBase.b(F_In);
                F_In.close();
            }
            fi.close();
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
