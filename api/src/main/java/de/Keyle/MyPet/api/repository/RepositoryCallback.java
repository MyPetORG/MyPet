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

package de.Keyle.MyPet.api.repository;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class RepositoryCallback<T> extends BukkitRunnable {
    T value;

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public void run() {
        callback(value);
    }

    public abstract void callback(T value);

    public void run(T value) {
        setValue(value);
        callback(value);
    }

    public synchronized BukkitTask runTask(Plugin plugin, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTask(plugin);
    }

    public synchronized BukkitTask runTaskAsynchronously(Plugin plugin, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskAsynchronously(plugin);
    }

    public synchronized BukkitTask runTaskLater(Plugin plugin, long delay, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskLater(plugin, delay);
    }

    public synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskLaterAsynchronously(plugin, delay);
    }

    public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskTimer(plugin, delay, period);
    }

    public synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskTimerAsynchronously(plugin, delay, period);
    }

    public synchronized BukkitTask runTask(T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTask(MyPetApi.getPlugin());
    }

    public synchronized BukkitTask runTaskAsynchronously(T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskAsynchronously(MyPetApi.getPlugin());
    }

    public synchronized BukkitTask runTaskLater(long delay, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskLater(MyPetApi.getPlugin(), delay);
    }

    public synchronized BukkitTask runTaskLaterAsynchronously(long delay, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskLaterAsynchronously(MyPetApi.getPlugin(), delay);
    }

    public synchronized BukkitTask runTaskTimer(long delay, long period, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskTimer(MyPetApi.getPlugin(), delay, period);
    }

    public synchronized BukkitTask runTaskTimerAsynchronously(long delay, long period, T value) throws IllegalArgumentException, IllegalStateException {
        this.setValue(value);
        return super.runTaskTimerAsynchronously(MyPetApi.getPlugin(), delay, period);
    }
}