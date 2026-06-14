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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;

/**
 * {@link EntityMountEvent} backstop that catches mount paths not flowing
 * through {@link RideInteractListener}'s saddle-on-rideable-mob {@code mobInteract}
 * handling. Primary use cases:
 *
 * <ul>
 *   <li><b>HappyGhast</b> (1.21.6+) — has dedicated vanilla mount logic
 *       that doesn't route through {@code AbstractHorse#mobInteract}.</li>
 *   <li><b>Nautilus / ZombieNautilus</b> (1.21.x+) — same shape.</li>
 *   <li><b>Vanilla saddle-mobInteract paths</b> for owner mounting when the
 *       owner is not holding the configured {@code RIDE_ITEM} (and therefore
 *       {@link RideInteractListener} did not invoke our gate logic).</li>
 *   <li><b>Future mount mechanics</b> Mojang adds without a code change here.</li>
 * </ul>
 *
 * <p>{@link RideGate#isInsideApproval} early-returns the gate when the
 * mount is the synchronous side effect of {@link RideGate#approve} from
 * {@link RideInteractListener} — same evaluator runs once in that case,
 * not twice. Net per-mount cost: a single {@link ThreadLocal#get} returning
 * {@code 0} for the vanilla path, or {@code 1} for the side-effect path.
 */
public class PetMountGateListener implements Listener {

    private static boolean isOwner(Player player, Pet pet) {
        return pet != null && pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event) {
        if (RideGate.isInsideApproval()) {
            return;
        }
        if (!PetEntityMarker.isMarked(event.getMount())) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Pet pet = MyPetApi.getPetManager().getPetFromEntity(event.getMount());
        if (pet == null) {
            return;
        }
        if (!RideGate.isMountable(pet)) {
            return;
        }

        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }

        boolean owner = isOwner(player, pet);

        if (!pet.canMove()) {
            // Sitting / disabled. Mirror RideInteractListener's gate:
            // non-owners always rejected; owners allowed (mount semantics
            // on a sitting pet are vanilla's concern from here).
            if (!owner) {
                event.setCancelled(true);
            }
            return;
        }

        // The mount being added by this event is NOT yet in getPassengers()
        // at this priority — vanilla adds it after the event resolves. So
        // an empty passenger list at this moment means the new mounter is
        // the driver.
        boolean isDriverSeat = mob.getPassengers().isEmpty();

        RideGate.Rejection rejection = RideGate.evaluate(pet, mob, player, owner, isDriverSeat);
        if (rejection != null) {
            RideGate.sendRejectionMessage(pet, player, rejection, owner);
            event.setCancelled(true);
        }
    }
}
