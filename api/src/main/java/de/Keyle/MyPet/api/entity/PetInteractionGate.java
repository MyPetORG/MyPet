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

package de.Keyle.MyPet.api.entity;

import org.bukkit.Material;

import java.util.Set;

/**
 * Marker for pet types whose underlying vanilla mob has a per-tick item-driven
 * right-click interaction (cow + bucket → milk, sheep and shears → wool, etc.).
 * In v4 pets are real vanilla mobs, so these interactions happen for free on
 * every tame pet. Implementers expose two pieces of metadata that
 * {@code PetInteractionGateListener} uses to decide whether to cancel the
 * upstream {@code PlayerInteractEntityEvent} (which short-circuits vanilla's
 * interaction handler before it can run):
 *
 * <ol>
 *   <li>{@link #gatedInteractionItems()} — the set of {@link Material}s in
 *       the player's hand that trigger this pet's gated interaction. Most
 *       pets gate one material; some (e.g., mooshroom: bowl and shears) may
 *       eventually gate two.</li>
 *   <li>{@link #isInteractionSuppressed()} — reads the implementer's own
 *       per-pet config flag (e.g. {@code PetCow.CAN_GIVE_MILK}).
 *       Returns {@code true} when the admin has disabled the interaction.</li>
 * </ol>
 *
 * <p>Per-pet flag names stay semantically rich ({@code CanGiveMilk},
 * {@code CanBeSheared}, {@code CanGiveStew}) because each implementer reads
 * its own {@code ConfigKey} static field. The listener never names them.
 * Mirrors {@link PetNaturalDrop} for the drop-event family.
 */
public interface PetInteractionGate extends Pet {

    /**
     * The set of held-item materials that trigger this pet's gated
     * vanilla interaction (e.g. {@code BUCKET} for cow milking,
     * {@code SHEARS} for sheep shearing).
     */
    Set<Material> gatedInteractionItems();

    /**
     * Returns {@code true} if the admin has disabled this pet type's
     * vanilla interaction via the per-type config flag.
     */
    boolean isInteractionSuppressed();
}
