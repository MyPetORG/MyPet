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

import org.bukkit.event.EventPriority;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Factory methods for {@link PetBehavior}, one per common Bukkit event
 * shape. Each helper bakes in the right entity-extractor for its event
 * type so the dispatcher doesn't need a per-event switch.
 *
 * <p>New helpers are added <i>as real migrations need them</i>, not
 * speculatively. Each comes with a defaults overload (most common priority
 * + ignoreCancelled values) plus an explicit overload for handlers that
 * need to interpose at a specific point in the chain.
 */
public final class PetBehaviorHelpers {

    private PetBehaviorHelpers() {}

    /**
     * Right-click on a pet entity. Fires for the common "player interacts
     * with this specific pet" pattern: flint-and-steel on a Creeper,
     * bucket on a Cow, shears on a Sheep, etc.
     *
     * <p>Defaults to {@link EventPriority#LOW} with {@code ignoreCancelled}
     * — matches the existing {@code PetInteractionGateListener} priority
     * so cancellations land before vanilla's {@code mobInteract}.
     */
    public static PetBehavior<PlayerInteractEntityEvent> onPetInteract(
            String petType, PetEventHandler<PlayerInteractEntityEvent> handler) {
        return onPetInteract(petType, EventPriority.LOW, true, handler);
    }

    /** {@link #onPetInteract(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<PlayerInteractEntityEvent> onPetInteract(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<PlayerInteractEntityEvent> handler) {
        return new PetBehavior<>(
                PlayerInteractEntityEvent.class,
                petType,
                priority,
                ignoreCancelled,
                PlayerInteractEntityEvent::getRightClicked,
                handler);
    }

    /**
     * Pet entity is the source of an explosion ({@code EntityExplodeEvent}).
     * Defaults to {@link EventPriority#NORMAL} with {@code ignoreCancelled = false}.
     */
    public static PetBehavior<EntityExplodeEvent> onPetExplodes(
            String petType, PetEventHandler<EntityExplodeEvent> handler) {
        return onPetExplodes(petType, EventPriority.NORMAL, false, handler);
    }

    /** {@link #onPetExplodes(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<EntityExplodeEvent> onPetExplodes(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<EntityExplodeEvent> handler) {
        return new PetBehavior<>(
                EntityExplodeEvent.class,
                petType,
                priority,
                ignoreCancelled,
                EntityExplodeEvent::getEntity,
                handler);
    }

    /**
     * Pet entity is the <b>victim</b> of damage ({@code EntityDamageEvent}).
     * Defaults to {@link EventPriority#NORMAL} with {@code ignoreCancelled = false}.
     */
    public static PetBehavior<EntityDamageEvent> onPetDamaged(
            String petType, PetEventHandler<EntityDamageEvent> handler) {
        return onPetDamaged(petType, EventPriority.NORMAL, false, handler);
    }

    /** {@link #onPetDamaged(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<EntityDamageEvent> onPetDamaged(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<EntityDamageEvent> handler) {
        return new PetBehavior<>(
                EntityDamageEvent.class,
                petType,
                priority,
                ignoreCancelled,
                EntityDamageEvent::getEntity,
                handler);
    }

    /**
     * Pet entity is the <b>damager</b> of an entity-on-entity attack
     * ({@code EntityDamageByEntityEvent}). The handler's {@code mob} argument
     * is the damaging pet; {@code event.getEntity()} is the victim.
     * Defaults to {@link EventPriority#NORMAL} with {@code ignoreCancelled = false}.
     */
    public static PetBehavior<EntityDamageByEntityEvent> onPetDamages(
            String petType, PetEventHandler<EntityDamageByEntityEvent> handler) {
        return onPetDamages(petType, EventPriority.NORMAL, false, handler);
    }

    /** {@link #onPetDamages(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<EntityDamageByEntityEvent> onPetDamages(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<EntityDamageByEntityEvent> handler) {
        return new PetBehavior<>(
                EntityDamageByEntityEvent.class,
                petType,
                priority,
                ignoreCancelled,
                EntityDamageByEntityEvent::getDamager,
                handler);
    }

    /**
     * Creeper pet entity is becoming powered/unpowered
     * ({@code CreeperPowerEvent}). Defaults to {@link EventPriority#NORMAL}
     * with {@code ignoreCancelled = false}.
     */
    public static PetBehavior<CreeperPowerEvent> onPetCreeperPower(
            String petType, PetEventHandler<CreeperPowerEvent> handler) {
        return onPetCreeperPower(petType, EventPriority.NORMAL, false, handler);
    }

    /** {@link #onPetCreeperPower(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<CreeperPowerEvent> onPetCreeperPower(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<CreeperPowerEvent> handler) {
        return new PetBehavior<>(
                CreeperPowerEvent.class,
                petType,
                priority,
                ignoreCancelled,
                CreeperPowerEvent::getEntity,
                handler);
    }

    /**
     * Pet entity is being transformed into a different entity type
     * ({@code EntityTransformEvent}). Common for lightning conversions and
     * mooshroom variant flips. Defaults to {@link EventPriority#NORMAL}
     * with {@code ignoreCancelled = false}.
     */
    public static PetBehavior<EntityTransformEvent> onPetLightningTransform(
            String petType, PetEventHandler<EntityTransformEvent> handler) {
        return onPetLightningTransform(petType, EventPriority.NORMAL, false, handler);
    }

    /** {@link #onPetLightningTransform(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<EntityTransformEvent> onPetLightningTransform(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<EntityTransformEvent> handler) {
        return new PetBehavior<>(
                EntityTransformEvent.class,
                petType,
                priority,
                ignoreCancelled,
                EntityTransformEvent::getEntity,
                handler);
    }

    /**
     * Pet entity is forming a block ({@code EntityBlockFormEvent}). Currently
     * only SnowGolem fires this vanilla-wise (snow-track placement). Defaults
     * to {@link EventPriority#NORMAL} with {@code ignoreCancelled = true}.
     */
    public static PetBehavior<EntityBlockFormEvent> onPetBlockForm(
            String petType, PetEventHandler<EntityBlockFormEvent> handler) {
        return onPetBlockForm(petType, EventPriority.NORMAL, true, handler);
    }

    /** {@link #onPetBlockForm(String, PetEventHandler)} with explicit priority + ignoreCancelled. */
    public static PetBehavior<EntityBlockFormEvent> onPetBlockForm(
            String petType, EventPriority priority, boolean ignoreCancelled,
            PetEventHandler<EntityBlockFormEvent> handler) {
        return new PetBehavior<>(
                EntityBlockFormEvent.class,
                petType,
                priority,
                ignoreCancelled,
                EntityBlockFormEvent::getEntity,
                handler);
    }
}
