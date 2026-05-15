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
import de.Keyle.MyPet.api.entity.PetAquaticEntity;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetSunSensitive;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Locale;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

/**
 * Keeps pets alive through vanilla environmental hazards:
 * <ul>
 *   <li>Fall damage immunity when the pet has a JUMP_BOOST effect</li>
 *   <li>Suffocation respawn — despawns the pet and re-spawns it on the
 *       next tick to escape solid blocks</li>
 *   <li>Daylight burn suppression for {@link PetSunSensitive} pets when
 *       the per-type {@code PreventDaylightBurn} flag is on</li>
 *   <li>Out-of-water suffocation suppression for {@link PetAquaticEntity}
 *       pets when the per-type {@code PreventSuffocation} flag is on</li>
 *   <li>Dry-out (out-of-water) suppression for Axolotl pets when
 *       {@code MyPet.Pets.Axolotl.PreventDryOut} is on</li>
 * </ul>
 */
public class PetSurvivalListener implements Listener {

    // JUMP was renamed to JUMP_BOOST in newer API versions
    private static final PotionEffectType JUMP_EFFECT;
    static {
        PotionEffectType jump = PotionEffectType.getByName("JUMP_BOOST");
        if (jump == null) {
            jump = PotionEffectType.getByName("JUMP");
        }
        JUMP_EFFECT = jump;
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        Pet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;

        LivingEntity bukkitEntity = (LivingEntity) event.getEntity();

        if (event.getCause() == DamageCause.FALL && JUMP_EFFECT != null && bukkitEntity.hasPotionEffect(JUMP_EFFECT)) {
            event.setCancelled(true);
            return;
        }

        // Bukkit reuses DROWNING for both directions (land-breather under
        // water, water-breather in air); for aquatic pets the latter case is
        // the only one that fires in vanilla — strict water-breathers cannot
        // enter the land-breather state.
        if (event.getCause() == DamageCause.DROWNING
                && pet instanceof PetAquaticEntity aquatic
                && aquatic.preventSuffocation()) {
            event.setCancelled(true);
            return;
        }

        // Vanilla Axolotls take DRYOUT damage while on land — the only mob
        // that emits this cause. Entity-type check is defensive in case Mojang
        // extends DRYOUT to additional mobs in a future MC version.
        if (event.getCause() == DamageCause.DRYOUT
                && bukkitEntity instanceof Axolotl
                && Configuration.MyPet.Axolotl.PREVENT_DRY_OUT) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == DamageCause.SUFFOCATION) {
            if (pet.hasPetRider()) {
                event.setCancelled(true);
                return;
            }
            final MyPetPlayer myPetPlayer = pet.getOwner();

            pet.removePet();
            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Despawn", myPetPlayer.getLanguage(), pet.getDisplayName()));

            Player ownerPlayer = myPetPlayer.getPlayer();
            if (ownerPlayer == null) return;
            ownerPlayer.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> {
                if (myPetPlayer.hasPet()) {
                    Pet runPet = myPetPlayer.getPet();
                    switch (runPet.createEntity()) {
                        case Canceled:
                            runPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", pet.getOwner(), runPet.getDisplayName()));
                            break;
                        case NoSpace:
                            runPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", pet.getOwner(), runPet.getDisplayName()));
                            break;
                        case NotAllowed:
                            runPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", pet.getOwner(), pet.getDisplayName()));
                            break;
                        case Flying:
                            runPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Spawn.Flying", pet.getOwner(), pet.getDisplayName()));
                            break;
                        case Success:
                            if (runPet != pet) {
                                runPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Call.Success", pet.getOwner(), runPet.getDisplayName()));
                            }
                            break;
                    }
                }
            }, null, 10L);
        }
    }

    // Hooked at default priority with ignoreCancelled so other plugins can
    // pre-empt. Block-caused (lava, magma) and entity-caused (flame arrow)
    // combust fire as the typed subclasses; the bare EntityCombustEvent is
    // what vanilla raises for sunlight burning, so the instanceof exclusions
    // are how we narrow to "natural" causes.
    @EventHandler(ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        if (event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent) {
            return;
        }
        Pet pet = PetListenerGuards.markedPet(event.getEntity()).orElse(null);
        if (pet == null) return;
        if (WorldGroup.getGroupByWorld(event.getEntity().getWorld()).isDisabled()) return;
        if (pet instanceof PetSunSensitive sunSensitive && sunSensitive.preventDaylightBurn()) {
            event.setCancelled(true);
        }
    }
}
