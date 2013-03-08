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

import java.io.BufferedReader;
import java.io.FileReader;

public class MyPetUtil
{
    public static boolean isInt(String number)
    {
        try
        {
            Integer.parseInt(number);
            return true;
        }
        catch (NumberFormatException nFE)
        {
            return false;
        }
    }

    public static boolean isByte(String number)
    {
        try
        {
            Byte.parseByte(number);
            return true;
        }
        catch (NumberFormatException nFE)
        {
            return false;
        }
    }

    public static boolean isDouble(String number)
    {
        try
        {
            Double.parseDouble(number);
            return true;
        }
        catch (NumberFormatException nFE)
        {
            return false;
        }
    }

    public static boolean isLong(String number)
    {
        try
        {
            Long.parseLong(number);
            return true;
        }
        catch (NumberFormatException nFE)
        {
            return false;
        }
    }

    public static boolean isFloat(String number)
    {
        try
        {
            Float.parseFloat(number);
            return true;
        }
        catch (NumberFormatException nFE)
        {
            return false;
        }
    }

    public static boolean isShort(String number)
    {
        try
        {
            Short.parseShort(number);
            return true;
        }
        catch (NumberFormatException nFE)
        {
            return false;
        }
    }

    public static String readFileAsString(String filePath) throws java.io.IOException
    {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1)
        {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static String convertStreamToString(java.io.InputStream is)
    {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
