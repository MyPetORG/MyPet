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

import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Shields the owner and the throwing pet from a pet's own Potion-skill splash. The pet is always
 * excluded (an undead pet would take damage from its own Instant Health lob); the owner is excluded
 * only from <em>harmful</em> throws aimed at an enemy, so beneficial potions still reach the owner.
 * The thrown potion carries the owner UUID and a harmful flag in its {@link PersistentDataContainer}
 * (set by {@code PotionImpl}).
 */
public class PetPotionListener implements Listener {

    private static final NamespacedKey POTION_OWNER_KEY = new NamespacedKey("mypet", "potion_owner");
    private static final NamespacedKey POTION_HARMFUL_KEY = new NamespacedKey("mypet", "potion_harmful");

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        ProjectileSource shooter = potion.getShooter();
        if (!(shooter instanceof LivingEntity thrower) || !PetEntityMarker.isMarked(thrower)) {
            return;
        }
        // The pet is never affected by its own throw.
        event.setIntensity(thrower, 0);

        PersistentDataContainer pdc = potion.getPersistentDataContainer();
        Byte harmful = pdc.get(POTION_HARMFUL_KEY, PersistentDataType.BYTE);
        if (harmful == null || harmful == 0) {
            return; // beneficial throw — let the owner receive the effect
        }
        String ownerId = pdc.get(POTION_OWNER_KEY, PersistentDataType.STRING);
        if (ownerId == null) {
            return;
        }
        // Harmful throw at an enemy — keep the owner out of the splash even if they're in the blast.
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (affected.getUniqueId().toString().equals(ownerId)) {
                event.setIntensity(affected, 0);
            }
        }
    }
}
