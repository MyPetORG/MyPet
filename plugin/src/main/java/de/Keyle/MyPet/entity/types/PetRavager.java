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

import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorHelpers;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.BEEF, Material.MUTTON}, flySpeed = 0.6608D)
public class PetRavager extends PetImpl {

    public static final ConfigKey<Boolean> ALLOW_LEAF_DESTRUCTION = ConfigKey.bool("Ravager", "AllowLeafDestruction", false);

    public static final Supplier<Listener> LEAF_DESTRUCTION_SUPPRESSOR =
            PetListenerRegistry.register(LeafDestructionSuppressor::new);

    /**
     * Plays the Ravager's forward-lunge attack animation on every successful
     * pet hit. {@code PetMeleeAttackGoal} calls {@code mob.swingMainHand()},
     * which is a no-op on the armless Ravager model. Vanilla drives the lunge
     * via an internal {@code attackTick} field, exposed on Bukkit's
     * {@code Ravager} interface as {@code setAttackTicks(int)}.
     */
    public static final PetBehavior<EntityDamageByEntityEvent> ATTACK_ANIMATION =
            PetBehaviorHelpers.onPetDamages("Ravager", (event, pet, mob) -> {
                if (mob instanceof Ravager ravager) {
                    ravager.setAttackTicks(10);
                }
            });

    public PetRavager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Suppresses vanilla Ravager leaf-breaking. {@code Ravager#aiStep} runs
     * at the entity-tick level (not a goal), so removing AI goals in
     * {@code PetGoalInstaller} doesn't touch it — cancelling the resulting
     * {@code EntityChangeBlockEvent} is the only Bukkit-level handle.
     */
    public static final class LeafDestructionSuppressor implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onPetRavagerDestroyLeaves(EntityChangeBlockEvent event) {
            if (ALLOW_LEAF_DESTRUCTION.get()) return;
            if (!(event.getEntity() instanceof Ravager)) return;
            if (!Tag.LEAVES.isTagged(event.getBlock().getType())) return;
            if (!PetEntityMarker.isMarked(event.getEntity())) return;
            event.setCancelled(true);
        }
    }
}
