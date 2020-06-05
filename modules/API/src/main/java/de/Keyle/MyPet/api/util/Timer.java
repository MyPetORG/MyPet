/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.RepositoryCallback;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class Timer {
    private static List<Integer> timerIDs = new ArrayList<>();
    private static final List<Scheduler> tasksToSchedule = new ArrayList<>();

    private Timer() {
    }

    public static void stopTimer() {
        if (timerIDs.size() > 0) {
            for (int timerID : timerIDs) {
                Bukkit.getScheduler().cancelTask(timerID);
            }
            timerIDs.clear();
        }
    }

    public static void startTimer() {
        stopTimer();

        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetApi.getPlugin(), () -> {
            for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
                myPet.schedule();
            }
        }, 0L, 20L));
        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetApi.getPlugin(), () -> {
            for (Scheduler task : tasksToSchedule) {
                task.schedule();
            }
        }, 5L, 20L));
        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetApi.getPlugin(), () -> {
            for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
                player.schedule();
            }
        }, 10L, 20L));

        timerIDs.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(MyPetApi.getPlugin(), () -> {
            // If  DECREMENT_TIME_IF_PET_STORED is false then this task is useless. However if its later turned on and
            // the configuration is reloaded then the task will allow the change to take effect without a server restart.
            if(Configuration.Respawn.DECREMENT_TIME_IF_PET_STORED) {
                for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
                    final MyPetPlayer owner = player;
                    // FIXME: Is there a data structure that gets you all pets for an online player?
                    //  reading it from disk every time may not be the best option
                    MyPetApi.getRepository().getMyPets(owner, new RepositoryCallback<List<StoredMyPet>>() {
                        @Override
                        public void callback(List<StoredMyPet> pets) {
                            for (StoredMyPet smp : pets) {
                                // If this is an active pet then it is not this task's job to decrement respawn time.
                                if (smp.getOwner().hasMyPet() && smp.getOwner().getMyPet().getUUID().equals(smp.getUUID())) {
                                    continue;
                                }
                                if (smp.getRespawnTime() > 0) {
                                    smp.setRespawnTime(Math.max(smp.getRespawnTime() - 10, 0));
                                    final StoredMyPet updatedPet = smp;
                                    // FIXME: Do i need to call this or is there some structure that I can change that
                                    //  will get saved periodically? or perhaps a way to update multiple pets with one database access.
                                    MyPetApi.getRepository().updateMyPet(updatedPet, null);
                                }
                            }
                        }
                    });
                }
            }
        }, 15L, 100L));
    }

    public static void reset() {
        tasksToSchedule.clear();
        stopTimer();
    }

    public static void addTask(Scheduler task) {
        tasksToSchedule.add(task);
    }

    public static void removeTask(Scheduler task) {
        tasksToSchedule.remove(task);
    }
}