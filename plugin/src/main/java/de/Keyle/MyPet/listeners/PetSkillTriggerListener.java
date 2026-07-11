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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.event.PetDamageEvent;
import de.Keyle.MyPet.api.event.PetOnHitSkillEvent;
import de.Keyle.MyPet.api.skill.OnDamageByEntitySkill;
import de.Keyle.MyPet.api.skill.OnHitSkill;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * Skill dispatch pipeline for pet damage events:
 * <ul>
 *   <li><b>NORMAL priority</b> — {@link OnDamageByEntitySkill} dispatch: triggers
 *       defensive skills (Thorns, etc.) when a pet takes damage from an entity.</li>
 *   <li><b>MONITOR priority</b> — {@link OnHitSkill} dispatch + {@link PetDamageEvent}
 *       emission: triggers offensive skills (Poison, Bleed, etc.) and emits the
 *       custom damage event when a pet deals damage to something.</li>
 * </ul>
 *
 * <p>The {@code isSkillActive} flag prevents reentrancy: Bukkit fires events
 * synchronously, so if a skill's {@code apply()} calls {@code target.damage()},
 * a new {@code EntityDamageByEntityEvent} fires and re-enters this handler
 * before {@code apply()} returns. The flag short-circuits that recursion.
 * Wrapped in try/finally to prevent permanent skill lockout if a skill throws.
 */
public class PetSkillTriggerListener implements Listener {

    private boolean isSkillActive = false;

    /**
     * Dispatches {@link OnDamageByEntitySkill} skills when a marked pet takes
     * damage from a living entity. Only runs if the event has not been
     * cancelled (e.g. by PvP policy) and the hook-plugin canHurt check passes.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPetTakesDamage(final EntityDamageByEntityEvent event) {
        Pet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        if (!(event.getDamager() instanceof LivingEntity damager)) return;

        if (damager instanceof Player) {
            if (!MyPetApi.getHookHelper().canHurt(pet.getOwner().getPlayer(), (Player) damager, true)) {
                return;
            }
        }

        if (!isSkillActive) {
            for (OnDamageByEntitySkill damageByEntitySkill : pet.getSkills().getOnDamageByEntitySkills()) {
                if (damageByEntitySkill.trigger()) {
                    isSkillActive = true;
                    try {
                        damageByEntitySkill.apply(damager, event);
                    } finally {
                        isSkillActive = false;
                    }
                }
            }
        }
    }

    /**
     * At MONITOR priority, when a marked pet deals damage:
     * <ol>
     *   <li>Emits {@link PetDamageEvent} so other plugins can adjust pet damage</li>
     *   <li>Dispatches {@link OnHitSkill} skills (Poison, Bleed, Fire, etc.)</li>
     * </ol>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPetDealsDamage(final EntityDamageByEntityEvent event) {
        @SuppressWarnings("ConstantConditions")
        boolean nullEntity = event.getEntity() == null;
        if (nullEntity) return;

        Entity target = event.getEntity();
        if (WorldGroup.getGroupByWorld(target.getWorld()).isDisabled()) return;
        if (!(target instanceof LivingEntity)) return;

        Entity source = event.getDamager();
        if (source instanceof Projectile) {
            ProjectileSource projectileSource = ((Projectile) source).getShooter();
            if (projectileSource instanceof Entity) {
                source = (Entity) projectileSource;
            }
        }

        if (!PetEntityMarker.isMarked(source)) return;
        Pet pet = getPetManager().getPetFromEntity(source);
        if (pet == null || pet.getStatus() != PetState.Here) return;

        // Emit PetDamageEvent so other plugins can adjust pet damage
        PetDamageEvent petDamageEvent = new PetDamageEvent(pet, target, event.getOriginalDamage(EntityDamageEvent.DamageModifier.BASE));
        Bukkit.getPluginManager().callEvent(petDamageEvent);
        if (petDamageEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        } else {
            event.setDamage(petDamageEvent.getDamage());
        }

        // Dispatch OnHitSkill skills
        if (!isSkillActive) {
            for (OnHitSkill onHitSkill : pet.getSkills().getOnHitSkills()) {
                if (onHitSkill.trigger()) {
                    PetOnHitSkillEvent skillEvent = new PetOnHitSkillEvent(pet, onHitSkill, (LivingEntity) target);
                    Bukkit.getPluginManager().callEvent(skillEvent);
                    if (!skillEvent.isCancelled()) {
                        isSkillActive = true;
                        try {
                            onHitSkill.apply((LivingEntity) target);
                        } finally {
                            isSkillActive = false;
                        }
                    }
                }
            }
        }
    }
}
