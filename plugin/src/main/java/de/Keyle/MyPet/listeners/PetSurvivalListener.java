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
import de.Keyle.MyPet.api.entity.PetAquaticEntity;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetSunSensitive;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Locale;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps pets alive through vanilla environmental hazards:
 * <ul>
 *   <li>Fall damage immunity when the pet has a JUMP_BOOST effect</li>
 *   <li>Suffocation respawn — despawns the pet and re-spawns it on the
 *       next tick to escape solid blocks</li>
 *   <li>Daylight burn suppression for {@link PetSunSensitive} pets when
 *       the per-type {@code PreventDaylightBurn} flag is on</li>
 *   <li>Out-of-water dry-out (and drowning) suppression for
 *       {@link PetAquaticEntity} pets when the per-type
 *       {@code PreventSuffocation} flag is on</li>
 * </ul>
 */
public class PetSurvivalListener implements Listener {

    /**
     * Pet UUID -> timestamp of the last suffocation rescue (despawn + delayed re-summon).
     * <p>
     * The rescue only helps when the pet can land somewhere it fits. When it cannot -- the
     * owner is standing in a fully walled-in spot -- the re-summoned pet suffocates again
     * immediately and schedules another rescue, and the pet spawn/despawn cycles forever
     * with a "Despawn" message per lap. A pet that suffocates again within
     * {@link #SUFFOCATION_LOOP_WINDOW_MS} of its own last rescue is therefore treated as
     * stuck: it stays away and the owner is told there is no space, instead of being
     * rescued into the same block a second time.
     * <p>
     * Entries older than the window are pruned on every suffocation, so the map holds at
     * most one entry per pet that suffocated in the last few seconds.
     */
    private static final Map<UUID, Long> LAST_SUFFOCATION_RESCUE = new ConcurrentHashMap<>();

    private static final long SUFFOCATION_LOOP_WINDOW_MS = 10_000L;

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

        // Water-breathers take DRYOUT out of water (the case players hit on
        // land); DROWNING only fires if one is genuinely submerged without
        // air. Cancel both for aquatic pets when PreventSuffocation is on.
        if ((event.getCause() == DamageCause.DRYOUT || event.getCause() == DamageCause.DROWNING)
                && pet instanceof PetAquaticEntity aquatic
                && aquatic.preventSuffocation()) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == DamageCause.SUFFOCATION) {
            if (pet.hasPetRider()) {
                event.setCancelled(true);
                return;
            }
            final MyPetPlayer myPetPlayer = pet.getOwner();

            long now = System.currentTimeMillis();
            LAST_SUFFOCATION_RESCUE.values().removeIf(rescued -> now - rescued > SUFFOCATION_LOOP_WINDOW_MS);
            // Only recent rescues survive the prune above, so a surviving entry means this
            // pet was already rescued moments ago and suffocated right back.
            boolean stuck = LAST_SUFFOCATION_RESCUE.remove(pet.getUUID()) != null;

            // removePet(false), not the no-arg removePet(): the no-arg form leaves
            // wantsToRespawn as it was, so a pet that got here after /petcall
            // (CommandCall -> removePet(true)) stayed flagged and MyPetPlayerImpl#schedule
            // re-summoned it once a second in parallel with the rescue below. Two
            // respawners for one pet is what made the despawn messages double and triple.
            pet.removePet(false);
            myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.Despawn", myPetPlayer.getLanguage(), pet.getDisplayName()));

            if (stuck) {
                // Second suffocation inside the window: the spot cannot hold the pet.
                // Leave it away -- the owner can /petcall once they have moved.
                myPetPlayer.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", myPetPlayer.getLanguage(), pet.getDisplayName()));
                return;
            }

            Player ownerPlayer = myPetPlayer.getPlayer();
            if (ownerPlayer == null) return;
            LAST_SUFFOCATION_RESCUE.put(pet.getUUID(), now);
            ownerPlayer.getScheduler().runDelayed(MyPetApi.getPlugin(), t -> {
                // Re-summon the pet that suffocated, not whichever pet happens to be
                // primary now — the owner may have others out.
                if (myPetPlayer.getPets().contains(pet)) {
                    Pet runPet = pet;
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
