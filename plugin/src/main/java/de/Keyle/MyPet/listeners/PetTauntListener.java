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

import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.skill.skills.Taunt;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.skill.skills.TauntImpl;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * Event half of the Taunt skill: catches mobs (re)acquiring the owner as a
 * target between the skill's periodic pulses and redirects them onto the
 * taunting pet immediately.
 *
 * <p>Safety invariants (anti-grief): the new target is always the owner's own
 * pet (never a player), only mob targets aimed at the owner are touched, and
 * other MyPet pets' targeting is never rewired.
 */
public class PetTauntListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        // Only hostile mobs — not provoked neutrals or another player's tamed animal.
        if (!(event.getEntity() instanceof Monster mob)) return;
        if (!(event.getTarget() instanceof Player targetPlayer)) return;
        if (WorldGroup.getGroupByWorld(mob.getWorld()).isDisabled()) return;
        // Never rewire another MyPet's targeting.
        if (PetEntityMarker.isMarked(mob)) return;

        Pet pet = getPetManager().getPet(targetPlayer);
        if (pet == null || pet.getStatus() != PetState.Here) return;
        Mob petMob = pet.getBukkitEntity();
        if (petMob == null || petMob.isDead() || petMob.equals(mob)) return;
        if (!petMob.getWorld().equals(mob.getWorld())) return;

        Taunt taunt = pet.getSkills().get(Taunt.class);
        if (taunt == null || !taunt.isActive()) return;
        if (TauntImpl.isFriendly(pet)) return;

        double range = taunt.getRange().getValue().doubleValue();
        if (petMob.getLocation().distanceSquared(mob.getLocation()) > range * range) return;

        event.setTarget(petMob);
        TauntImpl.playGrowlCue(petMob);
    }
}
