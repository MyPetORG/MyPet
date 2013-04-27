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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class MyPetTimer
{
    private static int timerID = -1;
    private static final List<IScheduler> tasksToSchedule = new ArrayList<IScheduler>();

    private MyPetTimer()
    {
    }

    public static void stopTimer()
    {
        if (timerID != -1)
        {
            DebugLogger.info("Timer stop");
            Bukkit.getScheduler().cancelTask(timerID);
            timerID = -1;
        }
    }

    public static void startTimer()
    {
        stopTimer();
        DebugLogger.info("Timer start");

        timerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetPlugin.getPlugin(), new Runnable()
        {
            public void run()
            {
                for (MyPet myPet : MyPetList.getAllActiveMyPets())
                {
                    myPet.scheduleTask();
                }
                for (IScheduler task : tasksToSchedule)
                {
                    task.schedule();
                }
                for (MyPetPlayer player : MyPetPlayer.getMyPetPlayers())
                {
                    player.schedule();
                }
            }
        }, 0L, 20L);
    }

    public static void reset()
    {
        tasksToSchedule.clear();
        stopTimer();
    }

    public static void addTask(IScheduler task)
    {
        tasksToSchedule.add(task);
    }

    public static void removeTask(IScheduler task)
    {
        tasksToSchedule.remove(task);
    }
}