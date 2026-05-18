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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.entity.ai.attack.PetRangedAttackGoal;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * PvP policy engine for pet entities: determines who can damage whom.
 * <ul>
 *   <li>Combust-by-entity: cancels owner-on-pet burn and hook-plugin violations</li>
 *   <li>Owner friendly-fire gate ({@code OWNER_CAN_ATTACK_PET} config)</li>
 *   <li>Hook-plugin {@code canHurt} integration (WorldGuard, MobArena, etc.)</li>
 *   <li>Pet-on-pet projectile self-damage prevention and duel-mode bypass</li>
 * </ul>
 *
 * <p>Cube-mob (Slime, MagmaCube) passive contact damage gating lives in
 * {@code PetSlime.CUBE_CONTACT_DAMAGE_GATE + PetMagmaCube.CUBE_CONTACT_DAMAGE_GATE}.
 */
public class PetPvPListener implements Listener {

    @EventHandler
    public void onCombustByEntity(EntityCombustByEntityEvent event) {
        @SuppressWarnings("ConstantConditions")
        boolean nullEntity = event.getEntity() == null;
        if (nullEntity) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        if (!PetEntityMarker.isMarked(event.getEntity())) return;

        if (!(event.getCombuster() instanceof Player || (event.getCombuster() instanceof Projectile && ((Projectile) event.getCombuster()).getShooter() instanceof Player))) {
            return;
        }
        Player damager;
        if (event.getCombuster() instanceof Projectile) {
            damager = (Player) ((Projectile) event.getCombuster()).getShooter();
        } else {
            damager = (Player) event.getCombuster();
        }

        Pet pet = getPetManager().getPetFromEntity(event.getEntity());
        if (pet == null) return;

        if (pet.getOwner().equals(damager) && !Configuration.Misc.OWNER_CAN_ATTACK_PET) {
            event.setCancelled(true);
        } else if (!pet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, pet.getOwner().getPlayer(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(final EntityDamageByEntityEvent event) {
        Pet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        // Player-on-pet PvP gate
        if (event.getDamager() instanceof Player || (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player)) {
            Player damager;
            if (event.getDamager() instanceof Projectile) {
                damager = (Player) ((Projectile) event.getDamager()).getShooter();
            } else {
                damager = (Player) event.getDamager();
            }
            if (pet.getOwner().equals(damager) && (!Configuration.Misc.OWNER_CAN_ATTACK_PET)) {
                event.setCancelled(true);
            } else if (!pet.getOwner().equals(damager) && !MyPetApi.getHookHelper().canHurt(damager, pet.getOwner().getPlayer(), true)) {
                event.setCancelled(true);
            }
        }

        // Pet-on-pet projectile: self-damage prevention + duel bypass
        if (event.getDamager() instanceof Projectile projectile) {
            Pet shooterPet = PetRangedAttackGoal.getSourcePet(projectile);
            if (shooterPet != null && shooterPet.getBukkitEntity() != null) {
                if (pet == shooterPet) {
                    event.setCancelled(true);
                }
                boolean inDuel = shooterPet.getTargetPriority() == TargetPriority.Duel
                        && pet.getTargetPriority() == TargetPriority.Duel
                        && shooterPet.getPetTarget() == pet.getBukkitEntity();
                if (!inDuel && !MyPetApi.getHookHelper().canHurt(shooterPet.getOwner().getPlayer(), pet.getOwner().getPlayer(), true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
