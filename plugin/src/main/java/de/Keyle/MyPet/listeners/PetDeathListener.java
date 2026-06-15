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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.event.PetRemoveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.PetInfoAccess;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import de.Keyle.MyPet.entity.visual.PetEntitySnapshot;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * Handles the full pet death pipeline: release-on-death, respawn timer
 * calculation (including duel-mode fast respawn), drop suppression,
 * XP loss, backpack drop, death message, auto-respawn economy,
 * cube-mob split suppression, and snapshot capture for respawn.
 */
public class PetDeathListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPetDeath(final EntityDeathEvent event) {
        Pet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        LivingEntity deadEntity = event.getEntity();

        // check health for death events where the pet isn't really dead (/killall)
        if (pet.getHealth() > 0) return;

        final MyPetPlayer owner = pet.getOwner();

        // Release-on-death: permanently remove the pet
        if (MyPetApi.getPetInfo().getReleaseOnDeath(pet.getPetType()) && !owner.isMyPetAdmin()) {
            PetRemoveEvent removeEvent = new PetRemoveEvent(pet, PetRemoveEvent.Source.DEATH);
            Bukkit.getServer().getPluginManager().callEvent(removeEvent);

            if (pet.getSkills().isActive(Backpack.class)) {
                pet.getSkills().get(BackpackImpl.class).dropContents(pet.getLocation().get());
            }
            if (pet instanceof PetEquipment) {
                ((PetEquipment) pet).dropEquipment();
            }

            pet.removePet();
            owner.setPetForWorldGroup(WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()), null);

            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Release.Dead", owner, pet.getDisplayName()));

            getPetManager().deactivatePet(owner, false);
            MyPetPlugin.getInstance().getRepository().removePet(pet.getUUID());

            return;
        }

        // Capture an EntitySnapshot for respawn. PetDeathListener owns this for
        // the death path; Pet.removePet captures for the despawn/logout path.
        // Without this, pendingSnapshot is null at respawn time (consumed by the
        // prior spawn, never refilled), forcing fresh-spawn — which loses
        // live-only state like slime size, /petadmin variant changes, collar
        // colour, profession, etc.
        try {
            PetInfoAccess.write(pet, PetEntitySnapshot.capture((Mob) deadEntity));
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("Failed to capture EntitySnapshot for pet "
                    + pet.getUUID() + " on death — pet will respawn with default "
                    + "live-entity state. " + t.getMessage());
        }

        // Calculate respawn time
        pet.setRespawnTime((MyPetGlobal.Respawn.TIME_FIXED.get() + MyPetApi.getPetInfo().getCustomRespawnTimeFixed(pet.getPetType())) + (pet.getExperience().getLevel() * (MyPetGlobal.Respawn.TIME_FACTOR.get() + MyPetApi.getPetInfo().getCustomRespawnTimeFactor(pet.getPetType()))));
        pet.setStatus(PetState.Dead);

        if (deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent e) {
            if (e.getDamager() instanceof Player) {
                pet.setRespawnTime((MyPetGlobal.Respawn.TIME_PLAYER_FIXED.get() + MyPetApi.getPetInfo().getCustomRespawnTimeFixed(pet.getPetType())) + (pet.getExperience().getLevel() * (MyPetGlobal.Respawn.TIME_PLAYER_FACTOR.get() + MyPetApi.getPetInfo().getCustomRespawnTimeFactor(pet.getPetType()))));
            } else if (PetEntityMarker.isMarked(e.getDamager())) {
                Pet killerPet = getPetManager().getPetFromEntity(e.getDamager());
                if (pet.getSkills().isActive(Behavior.class) && killerPet.getSkills().isActive(Behavior.class)) {
                    Behavior killerBehaviorSkill = killerPet.getSkills().get(Behavior.class);
                    Behavior deadBehaviorSkill = pet.getSkills().get(Behavior.class);
                    if (deadBehaviorSkill.getBehavior() == BehaviorMode.Duel && killerBehaviorSkill.getBehavior() == BehaviorMode.Duel) {
                        Pet petForEntity = getPetManager().getPetFromEntity(deadEntity);
                        if (petForEntity != null && e.getDamager().equals(petForEntity.getPetTarget())) {
                            pet.setRespawnTime(10);
                            killerPet.setHealth(Double.MAX_VALUE);
                        }
                    }
                }
            }
        }

        // Suppress vanilla drops and XP
        event.setDroppedExp(0);
        event.getDrops().clear();

        // XP loss on death
        if (MyPetGlobal.LevelSystem.Experience.LOSS_FIXED.get() > 0 || MyPetGlobal.LevelSystem.Experience.LOSS_PERCENT.get() > 0) {
            double lostExpirience = MyPetGlobal.LevelSystem.Experience.LOSS_FIXED.get();
            lostExpirience += pet.getExperience().getRequiredExp() * MyPetGlobal.LevelSystem.Experience.LOSS_PERCENT.get() / 100;
            if (lostExpirience > pet.getExp()) {
                lostExpirience = pet.getExp();
            }
            if (pet.getSkilltree() != null) {
                int requiredLevel = pet.getSkilltree().getRequiredLevel();
                if (requiredLevel > 1) {
                    double minExp = pet.getExperience().getExpByLevel(requiredLevel);
                    lostExpirience = pet.getExp() - lostExpirience < minExp ? pet.getExp() - minExp : lostExpirience;
                }
            }
            if (MyPetGlobal.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE.get()) {
                lostExpirience = pet.getExperience().removeExp(lostExpirience);
            } else {
                lostExpirience = pet.getExperience().removeCurrentExp(lostExpirience);
            }
            if (MyPetGlobal.LevelSystem.Experience.DROP_LOST_EXP.get() && lostExpirience < 0) {
                event.setDroppedExp((int) (Math.abs(lostExpirience)));
            }
        }

        // Backpack drop on death
        if (pet.getSkills().isActive(Backpack.class)) {
            BackpackImpl inventorySkill = pet.getSkills().get(BackpackImpl.class);
            inventorySkill.closeInventory();
            if (inventorySkill.getDropOnDeath().getValue() && !owner.isMyPetAdmin()) {
                inventorySkill.dropContents(pet.getLocation().get());
            }
        }

        // Death message and respawn notification
        PetDeathMessageFormatter.sendDeathMessage(event);
        pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", owner.getPlayer(), pet.getDisplayName(), pet.getRespawnTime()));

        // Auto-respawn via economy
        if (MyPetApi.getHookHelper().isEconomyEnabled() && owner.hasAutoRespawnEnabled() && pet.getRespawnTime() <= owner.getAutoRespawnMin() && Permissions.has(owner.getPlayer(), "MyPet.command.respawn")) {
            double costs = pet.getRespawnTime() * MyPetGlobal.Respawn.COSTS_FACTOR.get() + MyPetGlobal.Respawn.COSTS_FIXED.get();
            if (MyPetApi.getHookHelper().getEconomy().canPay(owner, costs)) {
                MyPetApi.getHookHelper().getEconomy().pay(owner, costs);
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.Paid", owner.getPlayer(), pet.getDisplayName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                pet.setRespawnTime(1);
            } else {
                pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.NoMoney", owner.getPlayer(), pet.getDisplayName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
            }
        }
    }

    /**
     * Suppresses vanilla split-on-death for cube-mob pets (Slime, MagmaCube).
     * Vanilla {@code Slime#remove}/{@code Slime#die} fires {@link SlimeSplitEvent}
     * when a size-2+ slime dies, spawning 2–4 wild children one tier smaller.
     * Without this gate, killing a Slime or MagmaCube pet leaks hostile mobs
     * around the player.
     *
     * <p>Cancelling {@link SlimeSplitEvent} aborts the entire split routine
     * before any child entities are constructed — no post-hoc cleanup needed.
     *
     * <p>Covers both pet types via the {@code MagmaCube extends Slime} Bukkit
     * hierarchy: the event fires for both, and {@link PetEntityMarker#isMarked}
     * returns true for both pet variants.
     *
     * <p>No {@code WorldGroup.isDisabled} guard: the marker is the authoritative
     * "this is a pet" signal, and disabling MyPet for a world does not make
     * pet-death-spawns-mobs an acceptable outcome.
     *
     * <p>No config flag: the leak is unambiguously wrong (pet death should not
     * summon hostile mobs); no admin would legitimately want vanilla split
     * behavior for pets.
     */
    @EventHandler
    public void onPetSlimeSplit(SlimeSplitEvent event) {
        if (!PetEntityMarker.isMarked(event.getEntity())) return;
        event.setCancelled(true);
    }
}
