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

package de.Keyle.MyWolf.util.logger;

import de.Keyle.MyWolf.MyWolfPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class MyWolfLogger
{
    private final Logger DebugLogger = Logger.getLogger("MyWolf");
    private boolean enabled = false;

    public MyWolfLogger(boolean enabled)
    {
        this.enabled = enabled;
        setup();
    }

    public boolean setup()
    {
        if (enabled)
        {
            try
            {
                Handler file_handler = new FileHandler(MyWolfPlugin.getPlugin().getDataFolder().getPath() + File.separator + "MyWolf.log");
                DebugLogger.setUseParentHandlers(false);
                file_handler.setFormatter(new LogFormat());
                DebugLogger.addHandler(file_handler);
                return true;
            }
            catch (IOException e)
            {
                this.enabled = false;
                e.printStackTrace();
            }
        }
        return false;
    }

    public void info(String text)
    {
        if (enabled)
        {
            DebugLogger.info(text);
        }
    }

    public void warning(String text)
    {
        if (enabled)
        {
            DebugLogger.warning(text);
        }
    }

    public void severe(String text)
    {
        if (enabled)
        {
            DebugLogger.severe(text);
        }
    }
}