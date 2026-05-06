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

package de.Keyle.MyPet.api.player;

import com.google.common.collect.BiMap;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.NBTStorage;
import de.Keyle.MyPet.api.util.Scheduler;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player who owns (or may own) pets. Wraps a Bukkit
 * {@link Player} with MyPet-specific preferences, per-world-group pet
 * bindings, and extended addon data. Persisted to the repository
 * alongside the player's pet records.
 * <p>
 * Obtained via {@code MyPetApi.getPlayerManager().getMyPetPlayer(Player)}
 * or by UUID lookup. A single instance exists per online player; offline
 * players may be loaded on demand for repository operations.
 */
public interface MyPetPlayer extends Scheduler, NBTStorage {

    /** Returns the player's current display name. */
    String getName();

    /** Whether this player has any non-default custom data persisted. */
    boolean hasCustomData();

    // ─── Preferences ────────────────────────────────────────────────────────────

    /** Enables or disables automatic respawning after death timer expires. */
    void setAutoRespawnEnabled(boolean flag);

    /** Returns {@code true} if the pet will auto-respawn after its timer. */
    boolean hasAutoRespawnEnabled();

    /** Returns the minimum respawn time (seconds) the player has configured. */
    int getAutoRespawnMin();

    /** Sets the minimum respawn time (seconds) before auto-respawn fires. */
    void setAutoRespawnMin(int value);

    /** Returns the volume multiplier for the pet's ambient living sounds. */
    float getPetLivingSoundVolume();

    /** Sets the volume multiplier for the pet's ambient living sounds. */
    void setPetLivingSoundVolume(float volume);

    /** Returns {@code true} if the under-name health bar is displayed. */
    boolean isHealthBarActive();

    /** Toggles the under-name health bar display. */
    void setHealthBarActive(boolean showHealthBar);

    /** Returns {@code true} if the leash-flag helper overlay is shown. */
    boolean isCaptureHelperActive();

    /** Toggles the leash-flag helper overlay during taming attempts. */
    void setCaptureHelperActive(boolean captureHelperMode);

    // ─── World-Group Bindings ───────────────────────────────────────────────────

    /** Binds a pet UUID as the active pet for the given world group name. */
    void setPetForWorldGroup(String worldGroup, UUID petUUID);

    /** Binds a pet UUID as the active pet for the given world group. */
    void setPetForWorldGroup(WorldGroup worldGroup, UUID petUUID);

    /** Returns the active pet UUID for the given world group, or {@code null}. */
    UUID getPetForWorldGroup(String worldGroup);

    /** Returns the active pet UUID for the given world group, or {@code null}. */
    UUID getPetForWorldGroup(WorldGroup worldGroup);

    /** Returns the full world-group → pet UUID mapping (bidirectional). */
    BiMap<String, UUID> getPetsForWorldGroups();

    /** Returns the world group name that a pet UUID is bound to. */
    String getWorldGroupForPet(UUID petUUID);

    /** Returns {@code true} if this player has a pet in the named world group. */
    boolean hasPetInWorldGroup(String worldGroup);

    /** Returns {@code true} if this player has a pet in the given world group. */
    boolean hasPetInWorldGroup(WorldGroup worldGroup);

    // ─── Extended Addon Data ────────────────────────────────────────────────────

    /**
     * Stores a per-addon NBT value on this player. Keys are namespaced by
     * {@code owner} so two addons cannot collide on the same key.
     */
    void addExtendedInfo(Plugin owner, String key, BinaryTag tag);

    /**
     * Reads a per-addon NBT value from this player's namespaced bucket.
     * Returns empty if the addon never wrote {@code key}.
     */
    Optional<BinaryTag> getExtendedInfo(Plugin owner, String key);

    // ─── Session State ──────────────────────────────────────────────────────────

    /** Returns {@code true} if the underlying Bukkit player is currently online. */
    boolean isOnline();

    /** Returns the player's Mojang UUID. */
    UUID getUniqueId();

    /** Returns the player's locale code (e.g. {@code "en_us"}). */
    String getLanguage();

    /** Returns {@code true} if this player has the MyPet admin permission. */
    boolean isMyPetAdmin();

    /** Returns {@code true} if this player has an active (live) pet. */
    boolean hasPet();

    /**
     * Returns the player's currently active pet, or {@code null} if no
     * pet is active in the current world group.
     */
    Pet getPet();

    /**
     * Returns the underlying Bukkit player. Only valid when
     * {@link #isOnline()} is {@code true}.
     */
    Player getPlayer();

    /** Sends an Adventure Component chat message to the player. */
    void sendMessage(Component message);

    /**
     * Sends a message with a cooldown (seconds). Returns {@code true} if
     * the message was sent, {@code false} if suppressed by the cooldown.
     */
    boolean sendMessage(Component message, int cooldown);

    /** Sends an Adventure Component as an action bar message. */
    void sendActionBar(Component message);
}