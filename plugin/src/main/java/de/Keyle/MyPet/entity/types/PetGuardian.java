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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetAquaticEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.listeners.PetListenerGuards;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.SUGAR}, flySpeed = 1.1013D)
public class PetGuardian extends PetImpl implements PetAquaticEntity {

    public static final ConfigKey<Boolean> CAN_SWIM = ConfigKey.bool("Guardian", "CanSwim", true);
    public static final ConfigKey<Boolean> PREVENT_SUFFOCATION = ConfigKey.bool("Guardian", "PreventSuffocation", true);

    public static final Supplier<Listener> SPIKE_OWNER_PROTECTOR =
            PetListenerRegistry.register(SpikeOwnerProtector::new);

    public PetGuardian(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /** Stops a Guardian pet's vanilla spikes (thorns) from hurting its own owner. */
    public static final class SpikeOwnerProtector implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onGuardianSpikeDamage(EntityDamageByEntityEvent event) {
            if (event.getCause() != DamageCause.THORNS) return;
            Pet pet = PetListenerGuards.markedPet(event.getDamager()).orElse(null);
            if (!(pet instanceof PetGuardian)) return;
            if (pet.getOwner().equals(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }
}
