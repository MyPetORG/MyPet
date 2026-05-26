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

import de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.PetLavaEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.entity.leashing.WildAngerCheck;
import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.EntityTickAccess;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import java.util.function.Supplier;

@ShopInfo
@DefaultInfo(food = {Material.BONE}, flySpeed = 0.6608D)
public class PetWarden extends PetImpl implements PetLavaEntity {

    private static final int DARKNESS_RADIUS = 20;
    private static final int DARKNESS_INTERVAL = 120;

    public static final WildAngerCheck<Warden> ANGER_CHECK =
            new WildAngerCheck<>(Warden.class, warden -> warden.getAngerLevel() != Warden.AngerLevel.CALM);

    /**
     * Strips Warden brain behaviors that autonomously target and attack
     * entities (including the owner). Vanilla Warden uses Brain not Goals,
     * so {@code PetGoalInstaller}'s {@code removeAllGoals} sweep leaves all
     * of vibration-driven anger management, sniff/roar/sonic-boom, and the
     * emerge/dig spawn-and-despawn animations intact.
     *
     * <p>Load-bearing entry: {@code SetRoarTarget} is the only behavior
     * that copies {@code AngerManagement} state into a memory the FIGHT
     * activity can act on ({@code ROAR_TARGET} → {@code Roar} →
     * {@code ATTACK_TARGET}). Stripping it breaks the targeting chain at
     * its root. The FIGHT-activity strips ({@code Roar}, {@code SonicBoom},
     * {@code MeleeAttack}) are belt-and-suspenders against any future
     * Mojang behavior that writes {@code ATTACK_TARGET} directly.
     *
     * <p>{@code Emerging} and {@code Digging} are quality-of-life strips:
     * without them, the pet would play a multi-second emergence animation
     * on spawn and periodically burrow into the ground when idle.
     */
    public static final PetBrainBehaviorRemoval BRAIN_BEHAVIOR_REMOVAL = new PetBrainBehaviorRemoval(
            "Warden",
            "SetRoarTarget",
            "Roar",
            "SonicBoom",
            "MeleeAttack",
            "Emerging",
            "Digging"
    );

    public static final Supplier<Listener> DARKNESS_EFFECT_SUPPRESSOR =
            PetListenerRegistry.register(DarknessEffectSuppressor::new);

    public PetWarden(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Suppresses the Warden's signature darkness pulse when the source is
     * a pet. Vanilla applies darkness from {@code Warden#tickServer} every
     * 120 ticks ({@code applyDarknessAround}) — an entity-tick path, not a
     * brain behavior, so the {@link #BRAIN_BEHAVIOR_REMOVAL} strip above
     * doesn't reach it. Bukkit exposes the apply call as
     * {@code EntityPotionEffectEvent} with {@code Cause.WARDEN}, but the
     * event doesn't carry the source Warden.
     *
     * <p>Attribution uses the same tick-rhythm formula vanilla uses to
     * decide when to apply: {@code (warden.tickCount + warden.id) % 120 == 0}.
     * Two Wardens collide on that formula only if their entity IDs differ
     * by an exact multiple of 120 — astronomically unlikely on a real
     * server, so at any given tick at most one nearby Warden is firing the
     * apply call. We read each candidate Warden's NMS {@code tickCount}
     * via {@link EntityTickAccess} (Bukkit's {@code getTicksLived()}
     * returns the load-time-frozen {@code totalEntityAge} field, not the
     * active per-tick counter) and pick the one whose modulo matches.
     *
     * <p>If reflection isn't available ({@code getTickCount} returns
     * {@code -1}), we fall back to "all nearby Wardens are pets → cancel"
     * — conservative, may over-suppress wild Warden darkness in mixed
     * scenarios but matches the user's expectation that the owner's
     * pet should never apply darkness.
     */
    public static final class DarknessEffectSuppressor implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onWardenAppliesDarkness(EntityPotionEffectEvent event) {
            if (event.getCause() != EntityPotionEffectEvent.Cause.WARDEN) return;
            Entity victim = event.getEntity();
            var nearbyWardens = victim.getWorld().getNearbyEntities(
                    victim.getLocation(), DARKNESS_RADIUS, DARKNESS_RADIUS, DARKNESS_RADIUS,
                    e -> e instanceof Warden);
            if (nearbyWardens.isEmpty()) return;
            for (var nearby : nearbyWardens) {
                int tickCount = EntityTickAccess.getTickCount(nearby);
                if (tickCount < 0) {
                    // Reflection unavailable — fall back to all-pets rule.
                    if (nearbyWardens.stream().allMatch(PetEntityMarker::isMarked)) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if ((tickCount + nearby.getEntityId()) % DARKNESS_INTERVAL == 0) {
                    if (PetEntityMarker.isMarked(nearby)) {
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }
}
