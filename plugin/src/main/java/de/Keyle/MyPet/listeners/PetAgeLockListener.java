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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.entity.spawn.PetGrowthLock;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Golden-dandelion age-lock support, wired reflectively so MyPet keeps compiling
 * and running on Paper 1.21.x (Java 21) while still handling the toggle on Paper
 * 26.1+ servers, where {@code PlayerToggleEntityAgeLockEvent} and golden
 * dandelions exist.
 *
 * <p>The event class is absent from the 1.21.x API, so it is never referenced at
 * compile time. {@link #register(Plugin)} resolves it via reflection; on a
 * pre-26.1 server the class is missing and registration is skipped — a clean
 * no-op, since golden dandelions don't exist there. On a 26.1+ server the handler
 * is installed via {@link org.bukkit.plugin.PluginManager#registerEvent} with a
 * reflective {@link EventExecutor}.
 *
 * <p>Behaviour mirrors the other pet interaction gates: owner-only; the owner's
 * choice is persisted in {@link PetGrowthLock}'s PDC key (overriding the
 * per-type {@code PreventNaturalGrowup} config default for that pet) so
 * {@code VanillaMobSpawner} re-derives the same lock state on the next spawn.
 * Only {@code getEntity()} / {@code isAgeLocked()} are read reflectively;
 * {@code getPlayer()} and {@code setCancelled(...)} come from {@link PlayerEvent}
 * / {@link Cancellable}, which exist on every supported API version.
 */
public final class PetAgeLockListener implements Listener {

    private static final String EVENT_CLASS_NAME =
            "io.papermc.paper.event.player.PlayerToggleEntityAgeLockEvent";

    private PetAgeLockListener() {
    }

    /**
     * Wires the golden-dandelion handler when the running server exposes
     * {@code PlayerToggleEntityAgeLockEvent} (Paper 26.1+); otherwise does nothing.
     */
    public static void register(Plugin plugin) {
        Class<? extends Event> eventClass;
        Method getEntity;
        Method isAgeLocked;
        try {
            eventClass = Class.forName(EVENT_CLASS_NAME).asSubclass(Event.class);
            getEntity = eventClass.getMethod("getEntity");
            isAgeLocked = eventClass.getMethod("isAgeLocked");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Pre-26.1 server: golden dandelions don't exist, nothing to gate.
            return;
        }

        EventExecutor executor = (listener, event) -> {
            if (!eventClass.isInstance(event)) {
                return;
            }
            try {
                handle(event, getEntity, isAgeLocked);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Event shape changed unexpectedly (reflective accessors, or a
                // cast on a future same-named event) — fail safe so one bad call
                // can't break other handlers or vanilla follow-up.
            }
        };

        Bukkit.getPluginManager().registerEvent(
                eventClass, new PetAgeLockListener(), EventPriority.LOW, executor, plugin, true);
    }

    private static void handle(Event event, Method getEntity, Method isAgeLocked)
            throws ReflectiveOperationException {
        if (!(getEntity.invoke(event) instanceof LivingEntity entity)) {
            return;
        }
        if (!PetEntityMarker.isMarked(entity)) {
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(entity);
        if (pet == null) {
            return;
        }
        Player player = ((PlayerEvent) event).getPlayer();
        if (!isOwner(player, pet)) {
            ((Cancellable) event).setCancelled(true);
            return;
        }
        Mob mob = pet.getBukkitEntity();
        if (mob != null) {
            // Record the owner's per-pet choice (overrides the PreventNaturalGrowup
            // config default); VanillaMobSpawner re-derives the lock from it on
            // the next spawn. Vanilla applies the live toggle to the mob.
            PetGrowthLock.setOverride(mob, (Boolean) isAgeLocked.invoke(event));
        }
    }

    private static boolean isOwner(Player player, Pet pet) {
        return pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }
}
