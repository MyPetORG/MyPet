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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetLavaEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.entity.leashing.WildAngerCheck;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.EntityTickAccess;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.ai.BrainAccess;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.BONE}, flySpeed = 0.6608D)
public class PetWarden extends PetImpl implements PetLavaEntity {

    private static final int DARKNESS_RADIUS = 20;
    private static final int DARKNESS_INTERVAL = 120;

    public static final WildAngerCheck<Warden> ANGER_CHECK =
            new WildAngerCheck<>(Warden.class, warden -> warden.getAngerLevel() != Warden.AngerLevel.CALM);

    /**
     * Strips the brain behaviors that drive autonomous targeting / attacks
     * ({@code SetRoarTarget} is the load-bearing one) and the emerge/dig
     * spawn-and-despawn animations.
     */
    public static final PetBrainBehaviorRemoval BRAIN_BEHAVIOR_REMOVAL = new PetBrainBehaviorRemoval(
            "Warden",
            "SetRoarTarget",
            "Roar",
            "SonicBoom",
            "MeleeAttack",
            "Emerging",
            "Digging"
    );

    public static final Supplier<Listener> DARKNESS_EFFECT_SUPPRESSOR =
            PetListenerRegistry.register(DarknessEffectSuppressor::new);

    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "Warden",
            OwnerRetaliationSuppressor::startForPet,
            OwnerRetaliationSuppressor::stopForPet
    );

    public PetWarden(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Per-tick {@code ATTACK_TARGET} clear so the brain can't enter FIGHT
     * after the Warden takes damage (vanilla {@code Warden#hurt} writes
     * the attacker to {@code AngerManagement}, which {@code WardenSensor}
     * — a Sensor, not strippable — copies into {@code ATTACK_TARGET}).
     * Same shape as {@code PetBreeze.AutonomousAttackSuppressor}.
     */
    public static final class OwnerRetaliationSuppressor {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        private OwnerRetaliationSuppressor() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Warden warden)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (warden.isDead()) return;
                    BrainAccess.clearAttackTarget(warden);
                } catch (Throwable ignored) {
                }
            }, null, 1L, 1L);
            if (task != null) {
                tasks.put(key, task);
            }
        }

        public static void stopForPet(Pet pet) {
            ScheduledTask task = tasks.remove(pet.getUUID());
            if (task != null) {
                try {
                    task.cancel();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Cancels the entity-tick darkness pulse when the source is a pet.
     * Attributes the {@code EntityPotionEffectEvent} via vanilla's apply
     * formula {@code (tickCount + id) % 120 == 0}, with a fallback to
     * "all nearby Wardens are pets" when {@link EntityTickAccess} can't
     * read {@code tickCount}.
     */
    public static final class DarknessEffectSuppressor implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onWardenAppliesDarkness(EntityPotionEffectEvent event) {
            if (event.getCause() != EntityPotionEffectEvent.Cause.WARDEN) return;
            Entity victim = event.getEntity();
            var nearbyWardens = victim.getWorld().getNearbyEntities(
                    victim.getLocation(), DARKNESS_RADIUS, DARKNESS_RADIUS, DARKNESS_RADIUS,
                    e -> e instanceof Warden);
            if (nearbyWardens.isEmpty()) return;
            for (var nearby : nearbyWardens) {
                int tickCount = EntityTickAccess.getTickCount(nearby);
                if (tickCount < 0) {
                    // Reflection unavailable — fall back to all-pets rule.
                    if (nearbyWardens.stream().allMatch(PetEntityMarker::isMarked)) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if ((tickCount + nearby.getEntityId()) % DARKNESS_INTERVAL == 0) {
                    if (PetEntityMarker.isMarked(nearby)) {
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }
}
