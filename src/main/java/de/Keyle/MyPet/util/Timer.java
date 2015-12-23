/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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
import de.Keyle.MyPet.api.util.IScheduler;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.repository.PlayerList;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class Timer {
    private static List<Integer> timerIDs = new ArrayList<>();
    private static final List<IScheduler> tasksToSchedule = new ArrayList<>();

    private Timer() {
    }

    public static void stopTimer() {
        if (timerIDs.size() > 0) {
            DebugLogger.info("Timer stop");
            for (int timerID : timerIDs) {
                Bukkit.getScheduler().cancelTask(timerID);
            }
            timerIDs.clear();
        }
    }

    public static void startTimer() {
        stopTimer();
        DebugLogger.info("Timer start");

        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetPlugin.getPlugin(), new Runnable() {
            public void run() {
                for (MyPet myPet : MyPetList.getAllActiveMyPets()) {
                    myPet.scheduleTask();
                }
            }
        }, 0L, 20L));
        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetPlugin.getPlugin(), new Runnable() {
            public void run() {
                for (IScheduler task : tasksToSchedule) {
                    task.schedule();
                }
            }
        }, 5L, 20L));
        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetPlugin.getPlugin(), new Runnable() {
            public void run() {
                for (MyPetPlayer player : PlayerList.getMyPetPlayers()) {
                    player.schedule();
                }
            }
        }, 10L, 20L));
    }

    public static void reset() {
        tasksToSchedule.clear();
        stopTimer();
    }

    public static void addTask(IScheduler task) {
        tasksToSchedule.add(task);
    }

    public static void removeTask(IScheduler task) {
        tasksToSchedule.remove(task);
    }
}