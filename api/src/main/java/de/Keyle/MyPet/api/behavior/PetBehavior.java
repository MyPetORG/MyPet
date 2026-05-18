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

package de.Keyle.MyPet.api.behavior;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.function.Function;

/**
 * One per-pet event handler, dispatched by the central
 * {@code PetBehaviorDispatcher}. Built via {@link PetBehaviorHelpers}
 * factories ({@link PetBehaviorHelpers#onPetInteract}, etc.) — direct
 * construction is package-private to enforce that helpers own the
 * entity-extraction logic for each event type.
 *
 * <p>Each instance self-registers with {@link PetBehaviorRegistry} on
 * construction so the dispatcher can iterate every behavior at startup
 * without further wiring.
 */
public final class PetBehavior<E extends Event> {

    private final Class<E> eventClass;
    private final String petType;
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private final Function<E, Entity> entityExtractor;
    private final PetEventHandler<E> handler;

    PetBehavior(Class<E> eventClass,
                String petType,
                EventPriority priority,
                boolean ignoreCancelled,
                Function<E, Entity> entityExtractor,
                PetEventHandler<E> handler) {
        this.eventClass = eventClass;
        this.petType = petType;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
        this.entityExtractor = entityExtractor;
        this.handler = handler;
        PetBehaviorRegistry.register(this);
    }

    /** Bukkit event class this behavior listens for. */
    public Class<E> eventClass() {
        return eventClass;
    }

    /** Pet type name (e.g. {@code "Creeper"}) this behavior applies to. */
    public String petType() {
        return petType;
    }

    /** Bukkit event priority — most behaviors are {@link EventPriority#NORMAL}. */
    public EventPriority priority() {
        return priority;
    }

    /** Whether the handler skips already-cancelled events. */
    public boolean ignoreCancelled() {
        return ignoreCancelled;
    }

    /**
     * Pulls the entity of interest out of {@code event} so the dispatcher can
     * route based on its pet membership. {@code PlayerInteractEntityEvent}
     * returns {@code getRightClicked()}; {@code EntityDamageEvent} returns
     * {@code getEntity()}; etc. Each {@link PetBehaviorHelpers} factory bakes
     * in the right extractor for its event type.
     */
    public Function<E, Entity> entityExtractor() {
        return entityExtractor;
    }

    /** The actual handler — invoked by the dispatcher after pet-type match. */
    public PetEventHandler<E> handler() {
        return handler;
    }
}
