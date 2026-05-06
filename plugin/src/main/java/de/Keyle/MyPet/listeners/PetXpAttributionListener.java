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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.skill.MyPetExperience;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Damage-weighted experience distribution: tracks how much damage each
 * living entity deals to a target so that kill-credit XP can be split
 * proportionally. Runs at {@link EventPriority#MONITOR} because it only
 * observes final damage values after all other listeners have had their say.
 */
public class PetXpAttributionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageMonitor(final EntityDamageByEntityEvent event) {
        @SuppressWarnings("ConstantConditions")
        boolean nullEntity = event.getEntity() == null;
        if (nullEntity) return;

        Entity target = event.getEntity();
        if (WorldGroup.getGroupByWorld(target.getWorld()).isDisabled()) return;

        if (!(target instanceof LivingEntity)) return;
        if (target instanceof Player) return;
        if (PetEntityMarker.isMarked(target)) return;

        if (!Configuration.LevelSystem.Experience.DAMAGE_WEIGHTED_EXPERIENCE_DISTRIBUTION) return;

        Entity source = event.getDamager();
        LivingEntity livingSource = null;
        if (source instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity) {
                livingSource = (LivingEntity) projectile.getShooter();
            }
        } else if (source instanceof LivingEntity) {
            livingSource = (LivingEntity) source;
        }
        if (livingSource != null) {
            MyPetExperience.addDamageToEntity(livingSource, (LivingEntity) target, event.getDamage());
        }
    }
}
