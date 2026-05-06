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
import de.Keyle.MyPet.api.entity.MyPet;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a private "stay" hint above sitting pets — a single
 * {@link Particle#BLOCK_MARKER BLOCK_MARKER} of {@link Material#BARRIER BARRIER}
 * shown only to the owner, every 60 ticks.
 *
 * <p>Per-pet scheduling: one task per pet, registered on spawn and cancelled on
 * despawn. This is Folia-safe — each task runs on the region thread that owns
 * the pet entity.
 */
public class PetSitParticleController {

    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public static void startForPet(MyPet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        Plugin plugin = MyPetApi.getPlugin();
        UUID key = pet.getUUID();
        stopForPet(pet);
        ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> tickPet(pet), null, 60L, 60L);
        if (task != null) {
            tasks.put(key, task);
        }
    }

    public static void stopForPet(MyPet pet) {
        UUID key = pet.getUUID();
        ScheduledTask task = tasks.remove(key);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    private static void tickPet(MyPet pet) {
        if (!pet.isSitting()) return;
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.isDead()) return;
        Player owner = pet.getOwner().getPlayer();
        if (owner == null || !owner.isOnline()) return;
        owner.spawnParticle(
                Particle.BLOCK_MARKER,
                mob.getLocation().add(0, mob.getEyeHeight() + 1, 0),
                1, 0F, 0F, 0F, 0F,
                Material.BARRIER.createBlockData());
    }
}
