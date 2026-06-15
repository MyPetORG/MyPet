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

import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.config.ConfigKeyRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetMultiPassenger;
import de.Keyle.MyPet.api.entity.PetNaturallyRideable;
import de.Keyle.MyPet.api.entity.PetSaddleable;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.PetSaddleHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Coordination + shared-evaluation helper between {@link RideInteractListener}
 * (the saddle-mobInteract path) and {@link PetMountGateListener} (the
 * {@code EntityMountEvent} backstop for HappyGhast / Nautilus and any
 * future mount mechanic).
 *
 * <p><b>Why coordination is needed:</b> when {@code RideInteractListener}
 * approves a mount and calls {@code mob.addPassenger(player)}, vanilla
 * synchronously fires {@code EntityMountEvent} as a side effect.
 * {@code PetMountGateListener} would then re-run the same gate logic on the
 * already-approved mount. The ThreadLocal-depth flag below lets the second
 * listener short-circuit when it sees the event is the side effect of an
 * approval we already made on this thread.
 *
 * <p><b>Folia compatibility:</b> {@code addPassenger} runs on the entity's
 * region thread; {@code EntityMountEvent} fires synchronously from inside
 * that same call on the same thread. {@link ThreadLocal}'s per-thread
 * isolation is sound on Folia — each region thread has its own counter.
 *
 * <p><b>Re-entrancy:</b> the depth counter (rather than a boolean) handles
 * arbitrary nesting via save-on-enter, restore-on-exit. If a plugin handles
 * {@code EntityMountEvent} and triggers another approved mount inside its
 * handler, depth goes 1 → 2 inside the inner approve and restores correctly.
 */
public final class RideGate {

    private RideGate() {}

    private static final ThreadLocal<Integer> APPROVAL_DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * Thread-local flag set by {@code /petride} (the command, not the right-click
     * path) so {@link #evaluate} can skip the {@code RequireRideItem} check. The
     * command itself is the explicit owner-driven ride trigger — the player
     * doesn't need to also hold the configured ride item. Other gates still apply.
     *
     * <p>Set/clear with try-finally in the command's executor; never leak past
     * the {@code evaluate} call.
     */
    private static final ThreadLocal<Boolean> COMMAND_TRIGGER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Marks the current thread as inside a {@code /petride} command execution
     * for the duration of {@code body.run()}. While the marker is set,
     * {@link #evaluate} skips the {@code RequireRideItem} check.
     */
    public static void runAsCommandTrigger(Runnable body) {
        Boolean previous = COMMAND_TRIGGER.get();
        COMMAND_TRIGGER.set(Boolean.TRUE);
        try {
            body.run();
        } finally {
            COMMAND_TRIGGER.set(previous);
        }
    }

    /** @return {@code true} while inside a {@link #runAsCommandTrigger} body on the current thread. */
    public static boolean isCommandTrigger() {
        return COMMAND_TRIGGER.get();
    }

    /**
     * PDC key for the per-pet owner-controlled "allow passengers" toggle, set
     * via the {@code /petride passengers allow|disallow} command. Stored on the
     * Bukkit mob so it round-trips automatically through {@code PetEntitySnapshot}
     * — no schema changes required. Defaults to {@code false} (disallowed) when
     * unset, so non-owners cannot mount until the owner explicitly opts in.
     */
    public static final NamespacedKey ALLOW_PASSENGERS_KEY = new NamespacedKey("mypet", "allow_passengers");

    /**
     * PDC key for the per-pet owner-controlled "allow non-owner driver" toggle,
     * set via the {@code /petride driver allow|disallow} command. Same storage
     * shape as {@link #ALLOW_PASSENGERS_KEY} (byte on the mob's PDC). Defaults
     * to {@code false} (disallowed) when unset — even when the admin-side
     * {@code AllowNonOwnerPrimaryMount} flag is {@code true}, the owner must
     * explicitly run {@code /petride driver allow} on the pet for non-owners
     * to mount the driver seat. Owner consent is opt-in, not opt-out.
     */
    public static final NamespacedKey ALLOW_DRIVER_KEY = new NamespacedKey("mypet", "allow_driver");

    /**
     * Reads the owner's per-pet "allow passengers" toggle from the mob's PDC.
     * Defaults to {@code false} (disallowed) when unset — owners must explicitly
     * opt in via {@code /petride passengers allow}.
     */
    public static boolean isPassengersAllowed(Mob mob) {
        Byte raw = mob.getPersistentDataContainer().get(ALLOW_PASSENGERS_KEY, PersistentDataType.BYTE);
        return raw != null && raw != 0;
    }

    /**
     * Writes the owner's per-pet "allow passengers" toggle to the mob's PDC.
     * The value persists across despawn/respawn via {@code PetEntitySnapshot}'s
     * full-NBT round-trip.
     */
    public static void setPassengersAllowed(Mob mob, boolean allowed) {
        mob.getPersistentDataContainer().set(ALLOW_PASSENGERS_KEY, PersistentDataType.BYTE,
                allowed ? (byte) 1 : (byte) 0);
    }

    /**
     * Reads the owner's per-pet "allow non-owner driver" toggle from the mob's PDC.
     * Defaults to {@code false} (disallowed) when unset — owners must explicitly
     * opt in via {@code /petride driver allow}.
     */
    public static boolean isDriverAllowed(Mob mob) {
        Byte raw = mob.getPersistentDataContainer().get(ALLOW_DRIVER_KEY, PersistentDataType.BYTE);
        return raw != null && raw != 0;
    }

    /**
     * Writes the owner's per-pet "allow non-owner driver" toggle to the mob's PDC.
     * The value persists across despawn/respawn via {@code PetEntitySnapshot}'s
     * full-NBT round-trip.
     */
    public static void setDriverAllowed(Mob mob, boolean allowed) {
        mob.getPersistentDataContainer().set(ALLOW_DRIVER_KEY, PersistentDataType.BYTE,
                allowed ? (byte) 1 : (byte) 0);
    }

    /**
     * Mount {@code player} on {@code mob} with the approval flag set so
     * {@link PetMountGateListener} skips re-gating when vanilla fires
     * {@code EntityMountEvent} as a side effect.
     */
    public static void approve(Mob mob, Player player) {
        int previous = APPROVAL_DEPTH.get();
        APPROVAL_DEPTH.set(previous + 1);
        try {
            mob.addPassenger(player);
        } finally {
            APPROVAL_DEPTH.set(previous);
        }
    }

    /**
     * @return {@code true} if the current thread is inside an
     *         {@link #approve} call's {@code addPassenger} synchronous
     *         vanilla event dispatch chain.
     */
    public static boolean isInsideApproval() {
        return APPROVAL_DEPTH.get() > 0;
    }

    /**
     * Rejection reasons returned by {@link #evaluate}. Listener callers can
     * route per-reason messaging if desired; passing back a typed value
     * (rather than just a boolean) makes the gate's intent clear at the
     * call site and lets log/debug code differentiate causes.
     */
    public enum Rejection {
        /** Admin's {@code AllowNonOwnerPrimaryMount=false} blocked the mount. Silent — leaks admin config. */
        NO_PRIMARY_MOUNT_ADMIN,
        /** Owner's {@code /petride driver disallow} blocked the mount. Visible — owner is the actor the non-owner can negotiate with. */
        NO_PRIMARY_MOUNT_OWNER,
        /** Admin's {@code AllowNonOwnerSecondaryMount=false}, or the pet isn't multi-passenger. Silent. */
        NO_SECONDARY_MOUNT_ADMIN,
        /** Owner's {@code /petride passengers disallow} blocked the mount. Visible. */
        NO_SECONDARY_MOUNT_OWNER,
        SADDLE_REQUIRED_OWNER,
        SADDLE_REQUIRED_NON_OWNER,
        RIDE_SKILL_NOT_ACTIVE,
        RIDE_ITEM_REQUIRED,
        EXTENDED_RIDE_PERMISSION_MISSING
    }

    /**
     * Sends an appropriate locale-translated rejection message to the player.
     * The owner and non-owner paths each have their own switch — different
     * keys, different recipients, different throttling.
     *
     * <p><b>Owner messages</b> route through {@link MyPetPlayer#sendMessage(Component, int)}
     * with a 2-second cooldown so repeat right-clicks don't spam the chat.
     *
     * <p><b>Non-owner messages</b> route through {@link Player#sendMessage(Component)}
     * directly (the non-owner has no {@link MyPetPlayer} association with this
     * pet, and looking one up just for throttling isn't worth the indirection).
     * Most non-owner gates currently stay silent to avoid leaking the owner's
     * per-pet config; the exceptions are the seat-permission rejections, where
     * the player already knows it's someone else's pet (visible on the nameplate)
     * and silent failure is more confusing than helpful.
     */
    public static void sendRejectionMessage(Pet pet, Player player, Rejection rejection, boolean isOwner) {
        Component msg;
        if (isOwner) {
            msg = switch (rejection) {
                case RIDE_SKILL_NOT_ACTIVE -> Locale.getFormattedComponent(
                        "Message.No.Skill", player,
                        pet.getDisplayName(),
                        Locale.getComponent("Name.Skill.Ride", player));
                case SADDLE_REQUIRED_OWNER -> Locale.getFormattedComponent(
                        "Message.No.HasSaddle", player,
                        pet.getDisplayName());
                case RIDE_ITEM_REQUIRED -> Locale.getFormattedComponent(
                        "Message.No.RideItem", player,
                        rideItemName(),
                        pet.getDisplayName());
                case EXTENDED_RIDE_PERMISSION_MISSING -> Locale.getComponent("Message.No.CanUse", player);
                // The non-owner-only rejections should never reach the owner branch —
                // silent fallback defends against future changes.
                default -> null;
            };
        } else {
            msg = switch (rejection) {
                case NO_PRIMARY_MOUNT_OWNER,
                     NO_SECONDARY_MOUNT_OWNER -> Locale.getFormattedComponent(
                        "Message.No.RidePermission", player,
                        pet.getOwner() != null ? pet.getOwner().getName() : "?",
                        pet.getDisplayName());
                case SADDLE_REQUIRED_NON_OWNER -> Locale.getFormattedComponent(
                        "Message.No.HasSaddle", player,
                        pet.getDisplayName());
                case RIDE_SKILL_NOT_ACTIVE -> Locale.getFormattedComponent(
                        "Message.No.Skill", player,
                        pet.getDisplayName(),
                        Locale.getComponent("Name.Skill.Ride", player));
                // Admin-denial paths (NO_PRIMARY_MOUNT_ADMIN, NO_SECONDARY_MOUNT_ADMIN)
                // stay silent — telling a non-owner "the admin disabled this" leaks
                // server config to nosy players. The visible cases above all point
                // to a peer (the owner) the non-owner can talk to: "hey, can you
                // saddle up?", "hey, can I ride?", "hey, can you set up the Ride
                // skill?".
                //
                // RIDE_ITEM_REQUIRED and EXTENDED_RIDE_PERMISSION_MISSING stay
                // silent for non-owners — the former leaks the configured server-side
                // ride item (and is self-fixable by the player rather than via the
                // owner), the latter is an admin permission grant the non-owner
                // also can't negotiate with the owner about.
                default -> null;
            };
        }
        if (msg == null) {
            return;
        }
        if (isOwner) {
            MyPetPlayer mpOwner = pet.getOwner();
            if (mpOwner != null) {
                mpOwner.sendMessage(msg, 2000);
            }
        } else {
            player.sendMessage(msg);
        }
    }

    /**
     * Returns the configured {@code MyPetGlobal.Skilltree.Skill.Ride.RIDE_ITEM.get()}
     * as a translatable {@link Component} so chat rendering uses the player's
     * client locale (e.g., "Lead" in English, "Leine" in German) without MyPet
     * shipping its own item-name translations.
     *
     * <p>Falls back to the literal text "ride item" if the configured item is
     * missing or {@code AIR} — defensive, since {@link Rejection#RIDE_ITEM_REQUIRED}
     * is only emitted when the configured item is non-null in the first place.
     */
    private static Component rideItemName() {
        if (MyPetGlobal.Skilltree.Skill.Ride.RIDE_ITEM.get() == null) {
            return Component.text("ride item");
        }
        ItemStack item = MyPetGlobal.Skilltree.Skill.Ride.RIDE_ITEM.get().getItem();
        if (item == null || item.getType() == Material.AIR) {
            return Component.text("ride item");
        }
        return Component.translatable(item.translationKey());
    }

    /**
     * Runs the full mount-gate check chain. Returns {@code null} if the mount
     * is allowed, or a {@link Rejection} value identifying the failing gate.
     *
     * <p>Used by both listeners — the only difference between them is whether
     * the caller cancels a {@code PlayerInteractEntityEvent} or an
     * {@code EntityMountEvent} on rejection.
     *
     * @param pet           the resolved pet
     * @param mob           the live Bukkit mob backing {@code pet}
     * @param player        the player attempting to mount
     * @param isOwner       whether {@code player} is the pet's owner
     * @param isDriverSeat  whether the next seat to be filled is the driver/primary
     *                      seat (i.e., {@code mob.getPassengers().isEmpty()})
     */
    /**
     * A pet is mountable if it is a naturally-rideable vanilla species or has
     * the Ride skilltree skill active. Shared by every mount-eligibility gate
     * so the predicate can't drift between call sites.
     */
    public static boolean isMountable(Pet pet) {
        return pet instanceof PetNaturallyRideable || pet.getSkills().isActive(Ride.class);
    }

    public static Rejection evaluate(Pet pet, Mob mob, Player player,
                                     boolean isOwner, boolean isDriverSeat) {
        String petType = pet.getPetType().name();

        // ---- Seat gating (non-owner only) ----
        // Non-owner secondary mounts (passengers) take a different gate path
        // from primary mounts: they're joining a ride the owner has already
        // initiated, so the "ride trigger" gates (RideItem, Saddle, RideSkill)
        // don't apply. Only the seat permission and extended-ride permission
        // gate passenger mounts. The split happens here to keep the per-gate
        // logic below uncluttered.
        if (!isOwner) {
            if (isDriverSeat) {
                // Primary mount path: admin flag AND owner runtime toggle must both
                // allow non-owner driving. Split rejections by source so the message
                // layer can message owner-denial visibly while keeping admin-denial
                // silent (admin-denial leaks server config; owner-denial is the
                // actor the non-owner can negotiate with directly).
                if (!ConfigKeyRegistry.readBool(petType, "AllowNonOwnerPrimaryMount", false)) {
                    return Rejection.NO_PRIMARY_MOUNT_ADMIN;
                }
                if (!isDriverAllowed(mob)) {
                    return Rejection.NO_PRIMARY_MOUNT_OWNER;
                }
            } else {
                // Secondary mount path: same split. Single-passenger pets are folded
                // into the admin branch — the pet type's incapacity is an
                // admin-config-level fact (which Pet classes implement
                // PetMultiPassenger), not an owner choice.
                if (!(pet instanceof PetMultiPassenger)
                        || !ConfigKeyRegistry.readBool(petType, "AllowNonOwnerSecondaryMount", true)) {
                    return Rejection.NO_SECONDARY_MOUNT_ADMIN;
                }
                if (!isPassengersAllowed(mob)) {
                    return Rejection.NO_SECONDARY_MOUNT_OWNER;
                }
                // Passenger early-out: skip ride-item / saddle / ride-skill gates.
                if (!Permissions.hasExtended(player, "MyPet.extended.ride")) {
                    return Rejection.EXTENDED_RIDE_PERMISSION_MISSING;
                }
                return null;
            }
        }

        // ---- Ride-item gating ----
        // When RequireRideItem is true (default), the mounting player must be
        // holding the configured Skilltree.Skill.Ride.RIDE_ITEM. This closes
        // the vanilla "right-click with empty hand on a saddled mount" mount
        // path that bypasses MyPet's explicit ride trigger. Applies to owners
        // and to non-owners taking the primary seat (passengers were handled
        // above by the secondary-mount early-out).
        //
        // /petride command execution is its own explicit ride trigger — when
        // the command runs evaluate inside runAsCommandTrigger, this check is
        // skipped (the command itself is the trigger).
        if (!isCommandTrigger()
                && ConfigKeyRegistry.readBool(petType, "RequireRideItem", true)) {
            if (MyPetGlobal.Skilltree.Skill.Ride.RIDE_ITEM.get() != null
                    && !MyPetGlobal.Skilltree.Skill.Ride.RIDE_ITEM.get().compare(player.getInventory().getItemInMainHand())) {
                return Rejection.RIDE_ITEM_REQUIRED;
            }
        }

        // ---- Saddle gating ----
        // Single knob (RequireSaddle) governs both owner and non-owner primary
        // mounts. The original design used the saddle as an implicit
        // owner-authorization signal for non-owners, but a future per-pet
        // owner command (/petride <something>) will own that responsibility
        // explicitly, so the saddle requirement is now purely a vanilla-feel
        // toggle. Passengers (non-owner secondary mounts) are still exempt —
        // they're handled by the early-out above.
        if (pet instanceof PetSaddleable
                && ConfigKeyRegistry.readBool(petType, "RequireSaddle", false)
                && !PetSaddleHelper.isSaddled(mob)) {
            return isOwner ? Rejection.SADDLE_REQUIRED_OWNER : Rejection.SADDLE_REQUIRED_NON_OWNER;
        }

        // ---- Ride skill gating ----
        if (ConfigKeyRegistry.readBool(petType, "RequireRideSkill", true)
                && !pet.getSkills().isActive(Ride.class)) {
            return Rejection.RIDE_SKILL_NOT_ACTIVE;
        }

        // ---- Permission gating ----
        if (!Permissions.hasExtended(player, "MyPet.extended.ride")) {
            return Rejection.EXTENDED_RIDE_PERMISSION_MISSING;
        }

        return null;
    }
}
