/*
 * Copyright (C) 2011-2013 Keyle
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;

public class JSON_Configuration
{
    public File jsonFile;
    private JSONObject config;

    public JSON_Configuration(String path)
    {
        this(new File(path));
    }

    public JSON_Configuration(File file)
    {
        jsonFile = file;
    }

    public JSONObject getJSONObject()
    {
        if (config == null)
        {
            config = new JSONObject();
        }
        return config;
    }

    public void load()
    {
        config = new JSONObject();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            config = (JSONObject) obj;
        }
        catch (Exception ignored)
        {
            ignored.printStackTrace();
        }
        jsonFile.setWritable(true);
        jsonFile.setReadable(true);
    }

    public boolean save()
    {
        try
        {
            // http://jsonformatter.curiousconcept.com/
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
            writer.write(config.toJSONString());
            writer.close();
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public void clearConfig()
    {
        config = new JSONObject();
    }
}