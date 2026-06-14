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
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.gui.context.PetMenuContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Dispatches player right-click on a Pet to the pet's {@link Pet#onInteract}
 * method. Cancels the underlying {@link PlayerInteractEntityEvent} if the pet
 * consumed the interaction (feed, sit toggle, per-type action).
 *
 * <p>Visual changes from vanilla interaction (saddling a pig, dyeing wool,
 * shearing a sheep, etc.) are persisted automatically via the live entity:
 * {@code getInfo()} snapshots the Bukkit mob on save, so no post-event
 * field-cache resync is needed.
 */
public class PetInteractionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // PlayerInteractEntityEvent fires once per hand (MAIN_HAND then OFF_HAND).
        // We dispatch only on the main-hand pass so feed/sit/equip handlers
        // run exactly once per right-click. Matches the legacy
        // EntityMyPet#handlePlayerInteraction guard that returned SUCCESS for
        // OFF_HAND without taking any action.
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

        EquipmentSlot hand = event.getHand();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(hand);

        if (pet.onInteract(player, item, hand)) {
            event.setCancelled(true);
            return;
        }

        // Fallback: a bare-hand, non-sneaking right-click by the owner opens the
        // pet management menu. Sneak+empty toggles sit and a configured
        // RIGHT_CLICK_COMMAND both run inside onInteract above, so they take
        // precedence; this only fires when neither consumed the interaction.
        // Naturally rideable pets are excluded so a bare-hand click still mounts
        // them (handled later by RideInteractListener / vanilla).
        boolean bareHand = item == null || item.getType().isAir();
        if (bareHand && !player.isSneaking() && isOwner(player, pet)
                && !(pet instanceof PetNaturallyRideable)
                && Configuration.Misc.RIGHT_CLICK_COMMAND.isEmpty()) {
            event.setCancelled(true);
            openPetMenu(player, pet);
        }
    }

    private static boolean isOwner(Player player, Pet pet) {
        return pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }

    @SuppressWarnings("unchecked")
    private static void openPetMenu(Player player, Pet pet) {
        MyPetApi.getGuiService().openMenu(
                player,
                (MenuId<PetMenuContext>) (MenuId<?>) MenuIds.PET_MENU,
                new PetMenuContext(player, pet));
    }
}
