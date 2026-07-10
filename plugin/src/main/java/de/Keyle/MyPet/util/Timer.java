/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.Scheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Timer {
    private static final List<Scheduler> tasksToSchedule = new ArrayList<>();
    private static final List<ScheduledTask> miscTasks = new ArrayList<>();
    private static final Map<UUID, ScheduledTask> petTasks = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledTask> playerTasks = new ConcurrentHashMap<>();

    private Timer() {
    }

    public static void stopTimer() {
        for (ScheduledTask task : miscTasks) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        miscTasks.clear();
        for (ScheduledTask task : petTasks.values()) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        petTasks.clear();
        for (ScheduledTask task : playerTasks.values()) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        playerTasks.clear();
    }

    public static void startTimer() {
        stopTimer();

        Plugin plugin = MyPetApi.getPlugin();
        ScheduledTask miscTask = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (Scheduler s : tasksToSchedule) {
                s.schedule();
            }
        }, 5L, 20L);
        miscTasks.add(miscTask);
    }

    public static void startPetTicking(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }
        Plugin plugin = MyPetApi.getPlugin();
        UUID key = pet.getUUID();
        stopPetTicking(pet);
        ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> pet.schedule(), null, 1L, 20L);
        if (task != null) {
            petTasks.put(key, task);
        }
    }

    public static void stopPetTicking(Pet pet) {
        UUID key = pet.getUUID();
        ScheduledTask task = petTasks.remove(key);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    public static void startPlayerTicking(MyPetPlayer myPetPlayer) {
        Player player = myPetPlayer.getPlayer();
        if (player == null) {
            return;
        }
        UUID key = myPetPlayer.getUniqueId();
        ScheduledTask running = playerTasks.get(key);
        if (running != null && !running.isCancelled()) {
            return;
        }
        // A cancelled task can linger in the map (e.g. the player's scheduler died on quit);
        // drop it so a rejoin gets a fresh ticker instead of being blocked by the stale entry.
        stopPlayerTicking(myPetPlayer);
        Plugin plugin = MyPetApi.getPlugin();
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, t -> myPetPlayer.schedule(), null, 10L, 20L);
        if (task != null) {
            playerTasks.put(key, task);
        }
    }

    public static void stopPlayerTicking(MyPetPlayer myPetPlayer) {
        UUID key = myPetPlayer.getUniqueId();
        ScheduledTask task = playerTasks.remove(key);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
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
