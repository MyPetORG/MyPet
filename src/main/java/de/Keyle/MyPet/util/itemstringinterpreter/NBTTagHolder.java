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

package de.Keyle.MyPet.util.itemstringinterpreter;

import net.minecraft.server.v1_6_R2.*;

/**
 * This class will be removed with Minecraft 1.7
 */
public class NBTTagHolder extends NBTHolder
{
    protected String data;

    public NBTTagHolder(String name, String data)
    {
        this.name = name;
        this.data = data;
    }

    @Override
    public NBTBase getNBT()
    {
        try
        {
            if (this.data.matches("[-+]?[0-9]*\\.?[0-9]+[d|D]"))
            {
                return new NBTTagDouble(this.name, Double.parseDouble(this.data.substring(0, this.data.length() - 1)));
            }
            if (this.data.matches("[-+]?[0-9]*\\.?[0-9]+[f|F]"))
            {
                return new NBTTagFloat(this.name, Float.parseFloat(this.data.substring(0, this.data.length() - 1)));
            }
            if (this.data.matches("[-+]?[0-9]+[b|B]"))
            {
                return new NBTTagByte(this.name, Byte.parseByte(this.data.substring(0, this.data.length() - 1)));
            }
            if (this.data.matches("[-+]?[0-9]+[l|L]"))
            {
                return new NBTTagLong(this.name, Long.parseLong(this.data.substring(0, this.data.length() - 1)));
            }
            if (this.data.matches("[-+]?[0-9]+[s|S]"))
            {
                return new NBTTagShort(this.name, Short.parseShort(this.data.substring(0, this.data.length() - 1)));
            }
            if (this.data.matches("[-+]?[0-9]+"))
            {
                return new NBTTagInt(this.name, Integer.parseInt(this.data.substring(0, this.data.length())));
            }
            if (this.data.matches("[-+]?[0-9]*\\.?[0-9]+"))
            {
                return new NBTTagDouble(this.name, Double.parseDouble(this.data.substring(0, this.data.length())));
            }
            if ((this.data.equalsIgnoreCase("true")) || (this.data.equalsIgnoreCase("false")))
            {
                return new NBTTagByte(this.name, (byte) (Boolean.parseBoolean(this.data) ? 1 : 0));
            }
            if ((this.data.startsWith("[")) && (this.data.endsWith("]")))
            {
                if (this.data.length() > 2)
                {
                    String str = this.data.substring(1, this.data.length() - 1);
                    String[] arrayOfString = str.split(",");
                    try
                    {
                        if (arrayOfString.length <= 1)
                        {
                            return new NBTTagIntArray(this.name, new int[]{Integer.parseInt(str.trim())});
                        }

                        int[] arrayOfInt = new int[arrayOfString.length];
                        for (int i = 0 ; i < arrayOfString.length ; i++)
                        {
                            arrayOfInt[i] = Integer.parseInt(arrayOfString[i].trim());
                        }
                        return new NBTTagIntArray(this.name, arrayOfInt);
                    }
                    catch (NumberFormatException localNumberFormatException2)
                    {
                        return new NBTTagString(this.name, this.data);
                    }
                }
                return new NBTTagIntArray(this.name);
            }

            if ((this.data.startsWith("\"")) && (this.data.endsWith("\"")) && (this.data.length() > 2))
            {
                this.data = this.data.substring(1, this.data.length() - 1);
            }

            this.data = this.data.replaceAll("\\\\\"", "\"");
            return new NBTTagString(this.name, this.data);
        }
        catch (NumberFormatException localNumberFormatException1)
        {
            this.data = this.data.replaceAll("\\\\\"", "\"");
        }
        return new NBTTagString(this.name, this.data);
    }
}