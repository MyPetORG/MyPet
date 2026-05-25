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
import de.Keyle.MyPet.api.config.ConfigKeyRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetSaddleable;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.util.PetSaddleHelper;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Gates saddle / harness application by non-owners on marked
 * {@link PetSaddleable} pets. Hooks {@link PlayerInteractEntityEvent}
 * at {@link EventPriority#HIGHEST} with {@code ignoreCancelled = true}.
 *
 * <p>Owner saddle/harness application is left untouched — the existing
 * {@code PetInteractionListener} dispatches owner interactions through
 * {@code Pet#onInteract}. Non-owner application is gated by the
 * {@code MyPet.Pets.<Type>.AllowNonOwnerSaddle} flag (default {@code false}):
 * when {@code false}, the event is cancelled and vanilla's saddle handler
 * does not run; when {@code true}, vanilla handles the application normally
 * and {@code PetEntitySnapshot} captures the new state on next save.
 *
 * <p>Saddle-like-item detection ({@link PetSaddleHelper#isSaddleLikeItem})
 * recognizes {@link org.bukkit.Material#SADDLE} for all saddle-shaped
 * mobs and the 16 {@code *_HARNESS} dye variants for HappyGhast.
 */
public class PetSaddleGateListener implements Listener {

    private static boolean isOwner(Player player, Pet pet) {
        return pet != null && pet.getOwner() != null && pet.getOwner().getPlayer() != null
                && pet.getOwner().getPlayer().getUniqueId().equals(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSaddleApply(PlayerInteractEntityEvent event) {
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
        if (!(pet instanceof PetSaddleable)) {
            return;
        }

        Mob mob = pet.getBukkitEntity();
        if (mob == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!PetSaddleHelper.isSaddleLikeItem(held, mob)) {
            return;
        }

        if (isOwner(player, pet)) {
            // Owner saddle/harness application flows through PetInteractionListener
            // and Pet#onInteract — don't interfere.
            return;
        }

        String petType = pet.getPetType().name();
        boolean allowed = ConfigKeyRegistry.readBool(petType, "AllowNonOwnerSaddle", false);
        if (!allowed) {
            event.setCancelled(true);
        }
    }
}
