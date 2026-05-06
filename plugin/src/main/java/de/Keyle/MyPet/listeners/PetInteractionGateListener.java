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
import de.Keyle.MyPet.api.entity.PetInteractionGate;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Suppresses vanilla item-driven right-click interactions on MyPet pets when
 * the pet's own per-type config flag is disabled (cow milking, sheep shearing,
 * mushroom stew, etc.).
 *
 * <p>In v4 pets are real vanilla mobs, so behaviors that previously had to be
 * driven from NMS overrides — bucket-on-cow, shears-on-sheep, etc. — now run
 * for free from vanilla {@code Mob#mobInteract}. The flags that used to
 * *enable* those behaviors therefore flip role: they now *suppress* the
 * vanilla path when set to {@code false}.
 *
 * <p>The listener is pet-agnostic: it dispatches via the
 * {@link PetInteractionGate} marker interface. Adding a new gated
 * interaction is one {@code extends PetInteractionGate} clause plus the two
 * abstract methods on the relevant {@code Pet<Type>} class — no listener
 * changes required.
 *
 * <p>Cancelling {@link PlayerInteractEntityEvent} short-circuits vanilla's
 * {@code mobInteract} before it runs, which also prevents downstream events
 * like {@code PlayerShearEntityEvent} from firing — a single hook suffices
 * for both the bucket/bowl interactions and the shears interaction.
 *
 * <p>Both hand passes are honored: if the player has the gated item in
 * offhand only, vanilla still milks/shears via the offhand pass, so we
 * must gate both. The event reports which hand triggered the call via
 * {@link PlayerInteractEntityEvent#getHand()}.
 */
public class PetInteractionGateListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!PetEntityMarker.isMarked(event.getRightClicked())) {
            return;
        }
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getRightClicked());
        if (!(pet instanceof PetInteractionGate gate)) {
            return;
        }
        ItemStack handItem = event.getPlayer().getInventory().getItem(event.getHand());
        if (handItem == null) {
            return;
        }
        if (!gate.gatedInteractionItems().contains(handItem.getType())) {
            return;
        }
        if (gate.isInteractionSuppressed()) {
            event.setCancelled(true);
        }
    }
}
