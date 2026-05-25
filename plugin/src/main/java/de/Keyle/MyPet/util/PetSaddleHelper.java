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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.entity.types.PetHappyGhast;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Steerable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SaddledMountInventory;

/**
 * Pure-static helper that abstracts the three Bukkit-API saddle shapes
 * behind a single read/write/inspect surface:
 *
 * <ul>
 *   <li>{@link Steerable} (Pig, Strider) — boolean saddle flag</li>
 *   <li>{@link SaddledMountInventory} (AbstractHorseInventory, ArmoredSaddledMountInventory)
 *       — inventory slot saddle item</li>
 *   <li>{@link EquipmentSlot#SADDLE} — equipment-slot fallback (HappyGhast harness)</li>
 * </ul>
 *
 * <p>Callers (the rideable-pet listeners, {@code PetCreationOptions} auto-gen,
 * and the per-Pet {@code dropEquipment} overrides) never need to type-switch
 * on the underlying Bukkit class.
 *
 * <p><b>Implementation-time verification:</b> HappyGhast's harness slot is
 * assumed to be {@link EquipmentSlot#SADDLE} based on Mojang's 1.21.x
 * equipment-slot consolidation. If a live HappyGhast pet with an equipped
 * harness reports {@code isSaddled() == false}, switch to
 * {@code EquipmentSlot.BODY} or add a reflective {@code HappyGhastHarnessAccess}
 * helper modeled on {@code BrainAccess}.
 */
public final class PetSaddleHelper {

    private PetSaddleHelper() {}

    /**
     * Returns the {@link EquipmentSlot} that vanilla actually reads for the
     * given mob's saddle-equivalent slot — only relevant for the non-{@link Steerable}
     * branch, where the slot varies by mob type.
     *
     * <p>HappyGhast uses {@link EquipmentSlot#BODY} (the harness is treated as
     * body armor, gated by {@code HappyGhast.canUseSlot} which only accepts
     * BODY). All other inventory-saddled mobs (AbstractHorse-family, Camel,
     * Nautilus) use {@link EquipmentSlot#SADDLE}.
     *
     * <p>String comparison on entity-type name keeps this stable across Paper
     * versions and avoids a hard {@code instanceof HappyGhast} reference
     * (HappyGhast's Bukkit interface is empty so the reference itself isn't
     * load-bearing, but the string check is identical and feels less
     * compiler-magical).
     */
    private static EquipmentSlot slotFor(Mob mob) {
        if (mob.getType().name().equals("HAPPY_GHAST")) {
            return EquipmentSlot.BODY;
        }
        return EquipmentSlot.SADDLE;
    }

    /**
     * @return {@code true} if {@code mob} has a saddle/harness equipped via
     *         any of the three Bukkit saddle shapes.
     */
    public static boolean isSaddled(Mob mob) {
        if (mob instanceof Steerable s) {
            return s.hasSaddle();
        }
        ItemStack saddle = mob.getEquipment().getItem(slotFor(mob));
        return saddle != null && saddle.getType() != Material.AIR;
    }

    /**
     * Applies a saddle/harness to {@code mob}. For {@link Steerable} mobs the
     * item is discarded (vanilla tracks only a boolean); for everyone else
     * the supplied item is placed in the appropriate equipment slot ({@link #slotFor})
     * via the standard {@link org.bukkit.inventory.EntityEquipment} API.
     *
     * <p>Used by {@code PetCreationOptions} auto-gen (passes the default
     * stack from {@link #getDefaultSaddleStack}) and by {@code PetHappyGhast}'s
     * harness creation option (passes a {@code *_HARNESS} stack).
     */
    public static void applySaddle(Mob mob, ItemStack item) {
        if (mob instanceof Steerable s) {
            s.setSaddle(true);
            return;
        }
        ItemStack stack = item != null ? item : new ItemStack(Material.SADDLE);
        mob.getEquipment().setItem(slotFor(mob), stack);
    }

    /**
     * Removes the equipped saddle/harness from {@code mob}. Returns the
     * removed {@link ItemStack} so callers can drop it on despawn — for
     * {@link Steerable} mobs (no stored item) a fresh {@link Material#SADDLE}
     * stack is materialized so the player gets a saddle back when the pet
     * is despawned.
     *
     * @return the saddle/harness item to drop, or {@code null} if nothing
     *         was equipped
     */
    public static ItemStack removeSaddle(Mob mob) {
        if (mob instanceof Steerable s) {
            if (!s.hasSaddle()) return null;
            s.setSaddle(false);
            return new ItemStack(Material.SADDLE);
        }
        EquipmentSlot slot = slotFor(mob);
        ItemStack saddle = mob.getEquipment().getItem(slot);
        if (saddle == null || saddle.getType() == Material.AIR) return null;
        mob.getEquipment().setItem(slot, null);
        return saddle;
    }

    /**
     * Returns the default saddle stack that {@code PetCreationOptions} should
     * apply when an admin uses the generic {@code saddle:true} creation flag
     * — or {@code null} if the pet type doesn't accept a generic
     * {@link Material#SADDLE} item.
     *
     * <p>{@link PetHappyGhast} returns {@code null} because HappyGhast accepts
     * only {@code *_HARNESS} items, not a plain saddle — its admin-facing
     * creation option lives separately as {@code harness:<color>} declared in
     * {@code PetHappyGhast.CREATION_SPECS}. Every other {@code PetSaddleable}
     * implementer returns a fresh {@link Material#SADDLE} stack.
     */
    public static ItemStack getDefaultSaddleStack(Class<?> petClass) {
        if (PetHappyGhast.class.equals(petClass)) return null;
        return new ItemStack(Material.SADDLE);
    }

    /**
     * Returns {@code true} if {@code item} is a saddle-like item appropriate
     * for {@code mob} — used by {@code PetSaddleGateListener} to filter
     * right-click events to actual saddle-application attempts.
     *
     * <p>{@link Material#SADDLE} is accepted for all saddle-shaped mobs.
     * {@code *_HARNESS} materials are additionally accepted for HappyGhast.
     */
    public static boolean isSaddleLikeItem(ItemStack item, Mob mob) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material mat = item.getType();
        if (mat == Material.SADDLE) return true;
        if (mob.getType().name().equals("HAPPY_GHAST") && mat.name().endsWith("_HARNESS")) return true;
        return false;
    }
}
