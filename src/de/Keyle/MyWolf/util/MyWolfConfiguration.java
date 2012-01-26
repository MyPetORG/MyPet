package de.Keyle.MyWolf.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Keyle
 * Date: 26.01.12
 * Time: 12:42
 * To change this template use File | Settings | File Templates.
 */
public class MyWolfConfiguration
{
    public File ConfigFile;
    public FileConfiguration Config;

    public MyWolfConfiguration(String Path)
    {
        ConfigFile = new File(Path);
        Config = new YamlConfiguration();
        try
        {
            Config.load(ConfigFile);
        }
        catch (Exception ignored) {}
    }
    
    public MyWolfConfiguration(File f)
    {
        ConfigFile = f;
        Config = new YamlConfiguration();
        try
        {
            Config.load(ConfigFile);
        }
        catch (Exception ignored){}
    }

    public boolean saveConfig()
    {
        try
        {
            Config.save(ConfigFile);
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
