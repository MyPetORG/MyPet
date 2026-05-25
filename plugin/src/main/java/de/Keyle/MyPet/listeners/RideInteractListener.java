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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetNaturallyRideable;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handles the saddle-on-rideable-mob {@code mobInteract} flow for mounting
 * MyPet rideable pets. Catches {@link PlayerInteractEntityEvent} at
 * {@link EventPriority#HIGHEST} and routes every branch — owner vs non-owner,
 * primary vs secondary seat — through {@link RideGate#evaluate} so the gate
 * logic stays in a single place and is reused by {@link PetMountGateListener}.
 *
 * <p><b>Cancellation discipline:</b> every rejection branch cancels the event
 * before returning, so vanilla's {@code mobInteract} handler does not run.
 * This closes the v4 NMS-elimination regression where vanilla's saddle-mount
 * logic was silently running alongside MyPet's gating.
 *
 * <p>Approved mounts call {@link RideGate#approve} (not {@code mob.addPassenger}
 * directly) so the synchronously-fired {@code EntityMountEvent} side effect
 * is detectable by {@link PetMountGateListener} via {@code RideGate.isInsideApproval()}.
 *
 * <p>The owner mount path additionally requires the configured
 * {@code Skilltree.Skill.Ride.RIDE_ITEM} to be held — preserving the legacy
 * explicit-trigger contract. Non-owners are not subject to the RIDE_ITEM
 * check; their mount attempts are evaluated whenever they right-click a
 * marked rideable pet.
 */
public class RideInteractListener implements Listener {

    private static boolean isOwner(Player player, Pet pet) {
        return pet != null && pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRideWithConfiguredItem(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!PetEntityMarker.isMarked(event.getRightClicked())) {
            return;
        }

        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getRightClicked());
        if (pet == null) {
            return;
        }
        if (!(pet instanceof PetNaturallyRideable)) {
            return;
        }

        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }

        Player player = event.getPlayer();
        if (mob.getPassengers().contains(player)) {
            // Already on board — nothing to do.
            return;
        }

        boolean owner = isOwner(player, pet);
        boolean isDriverSeat = mob.getPassengers().isEmpty();

        if (!pet.canMove()) {
            // Sitting / disabled — RideInteractListener doesn't engage. Return
            // without cancelling for everyone so other listeners (saddle gate,
            // interaction gate) and vanilla can still handle non-mount actions
            // on the sitting pet (saddle placement, food, inventory access).
            // Vanilla won't mount a sitting/disabled mount anyway, so there's
            // no bypass to close; if any future mob does allow it,
            // PetMountGateListener's EntityMountEvent backstop catches it.
            return;
        }

        // Only engage when the player is holding the configured RIDE_ITEM —
        // for both owners and non-owners. The held-item gate scopes this
        // listener to the "explicit MyPet ride trigger" path. Other vanilla
        // interactions on the pet (saddle placement, inventory, food) get
        // to proceed to PetSaddleGateListener / vanilla mobInteract / etc.
        //
        // The mount bypass is still closed by PetMountGateListener's
        // EntityMountEvent backstop: if vanilla mobInteract proceeds to
        // actually addPassenger (e.g., empty-hand right-click on a saddled
        // mount), the backstop catches it and gates via the same
        // RideGate.evaluate chain we'd otherwise run here.
        if (Configuration.Skilltree.Skill.Ride.RIDE_ITEM != null
                && !Configuration.Skilltree.Skill.Ride.RIDE_ITEM.compare(player.getInventory().getItemInMainHand())) {
            return;
        }

        RideGate.Rejection rejection = RideGate.evaluate(pet, mob, player, owner, isDriverSeat);
        if (rejection != null) {
            RideGate.sendRejectionMessage(pet, player, rejection, owner);
            event.setCancelled(true);
            return;
        }

        RideGate.approve(mob, player);
        event.setCancelled(true);
    }
}
