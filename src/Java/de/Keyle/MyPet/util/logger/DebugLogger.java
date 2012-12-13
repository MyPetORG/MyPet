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

package de.Keyle.MyPet.util.logger;

import de.Keyle.MyPet.MyPetPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class DebugLogger
{
    private final Logger debugLogger = Logger.getLogger("MyPet");
    private boolean enabled = false;

    public DebugLogger(boolean enabled)
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
                Handler fileHandler = new FileHandler(MyPetPlugin.getPlugin().getDataFolder().getPath() + File.separator + "MyPet.log");
                debugLogger.setUseParentHandlers(false);
                fileHandler.setFormatter(new LogFormat());
                debugLogger.addHandler(fileHandler);
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
            debugLogger.info(text);
        }
    }

    public void warning(String text)
    {
        if (enabled)
        {
            debugLogger.warning(text);
        }
    }

    public void severe(String text)
    {
        if (enabled)
        {
            debugLogger.severe(text);
        }
    }
}