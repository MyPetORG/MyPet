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

package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders custom-coloured potion particles around MyPet pets via per-pet
 * {@code Particle.DUST} scheduler tasks.
 *
 * <p>Pets register themselves via {@link #show(Pet, Color)} — which lazily
 * starts the tick task on the mob's entity scheduler — and deregister via
 * {@link #hide(Pet)}, which cancels it. {@link #stopForPet} is the despawn
 * cleanup.
 */
public class PetPotionParticleController {

    private static final Map<UUID, Particle.DustOptions> activeByPet = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    /** Despawn cleanup — cancels the tick task and forgets the colour. */
    public static void stopForPet(Pet pet) {
        UUID key = pet.getUUID();
        cancelTask(key);
        activeByPet.remove(key);
    }

    public static void show(Pet pet, Color color) {
        if (pet == null || color == null) return;
        activeByPet.put(pet.getUUID(), new Particle.DustOptions(color, 1.0f));
        startTask(pet);
    }

    public static void hide(Pet pet) {
        if (pet == null) return;
        UUID key = pet.getUUID();
        activeByPet.remove(key);
        cancelTask(key);
    }

    private static void startTask(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        Plugin plugin = MyPetApi.getPlugin();
        tasks.computeIfAbsent(pet.getUUID(),
                key -> mob.getScheduler().runAtFixedRate(plugin, t -> tickPet(pet), null, 1L, 2L));
    }

    private static void cancelTask(UUID key) {
        ScheduledTask task = tasks.remove(key);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    private static void tickPet(Pet pet) {
        Particle.DustOptions options = activeByPet.get(pet.getUUID());
        if (options == null) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.isDead()) return;

        double width = mob.getWidth();
        double height = mob.getHeight();
        mob.getWorld().spawnParticle(
                Particle.DUST,
                mob.getLocation().add(0, height * 0.5, 0),
                3,
                width * 0.5, height * 0.5, width * 0.5,
                0.0,
                options);
    }
}
