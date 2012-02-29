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

package de.Keyle.MyWolf.util;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolfPlugin;

public class MyWolfTimer
{
    private int Timer = -1;

    public void stopTimer()
    {
        if (Timer != -1)
        {
            MyWolfPlugin.getPlugin().getServer().getScheduler().cancelTask(Timer);
            Timer = -1;
        }
    }

    public void startTimer()
    {
        if (Timer != -1)
        {
            stopTimer();
        }

        Timer = MyWolfPlugin.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(MyWolfPlugin.getPlugin(), new Runnable()
        {
            int AutoSaveTimer = MyWolfConfig.AutoSaveTime;

            public void run()
            {
                for (MyWolf MWolf : MyWolfList.getMyWolfList())
                {
                    MWolf.scheduleTask();
                }
                if (MyWolfConfig.AutoSaveTime > 0 && AutoSaveTimer-- <= 0)
                {
                    MyWolfPlugin.getPlugin().saveWolves(MyWolfPlugin.NBTWolvesFile);
                    AutoSaveTimer = MyWolfConfig.AutoSaveTime;
                }
            }
        }, 0L, 20L);
    }
}
