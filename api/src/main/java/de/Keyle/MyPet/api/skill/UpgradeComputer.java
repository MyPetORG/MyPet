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

package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.api.skill.modifier.UpgradeModifier;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks the cumulative value of a single skill property (e.g. damage, chance,
 * cooldown) as upgrade modifiers are applied and removed. The computer maintains
 * an ordered history of all applied {@link UpgradeModifier}s and recomputes the
 * current value from the {@link #baseValue} whenever modifiers change, making
 * skilltree resets fully reversible.
 *
 * <p>Registered {@link UpgradeCallback}s are notified after every value change,
 * allowing skills to react immediately (e.g. update an attribute on the pet entity).
 *
 * @param <T> the value type (typically {@link Number}, {@link Boolean}, or an enum)
 * @see UpgradeModifier
 * @see Upgrade
 */
public class UpgradeComputer<T> {

    final List<UpgradeModifier<T>> upgrades = new LinkedList<>();
    // CopyOnWriteArrayList: callbacks are iterated on every upgrade apply but mutated
    // rarely (pet spawn/despawn adds/removes the Ride activation watcher), and those two
    // can race on different region threads under Folia — a plain LinkedList would CME.
    final List<UpgradeCallback<T>> callbacks = new CopyOnWriteArrayList<>();
    T currentValue;
    final T baseValue;

    /**
     * Creates a new computer with the given base (default) value. The current value
     * starts equal to the base value and is recalculated as modifiers are added or
     * removed.
     *
     * @param baseValue the initial value before any upgrades
     */
    public UpgradeComputer(T baseValue) {
        this.baseValue = baseValue;
        this.currentValue = this.baseValue;
    }

    /** Returns the current computed value after all applied modifiers. */
    public T getValue() {
        return this.currentValue;
    }

    /**
     * Appends a single modifier to the history, updates the current value by
     * applying it, and notifies all callbacks with {@link CallbackReason#Add}.
     *
     * @param upgrade the modifier to add (ignored if {@code null})
     */
    public void addUpgrade(UpgradeModifier<T> upgrade) {
        if (upgrade != null) {
            this.currentValue = upgrade.modify(this.currentValue);
            this.upgrades.add(upgrade);
            if (!this.callbacks.isEmpty()) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Add);
                }
            }
        }
    }

    /**
     * Appends multiple modifiers to the history in order, recomputes the current
     * value, and notifies callbacks once with {@link CallbackReason#Add}.
     *
     * @param upgrades the modifiers to add (ignored if {@code null} or empty)
     */
    public void addUpgrades(Collection<UpgradeModifier<T>> upgrades) {
        if (upgrades != null && !upgrades.isEmpty()) {
            for (UpgradeModifier<T> upgrade : upgrades) {
                this.currentValue = upgrade.modify(this.currentValue);
                this.upgrades.add(upgrade);
            }
            if (!this.callbacks.isEmpty()) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Add);
                }
            }
        }
    }

    /**
     * Removes the <em>last</em> occurrence of the given modifier from the history,
     * then recomputes the current value from scratch (base + remaining modifiers).
     * Notifies callbacks with {@link CallbackReason#Remove}.
     *
     * @param upgrade the modifier to remove (ignored if {@code null})
     */
    public void removeUpgrade(UpgradeModifier<T> upgrade) {
        if (upgrade != null) {
            int last = this.upgrades.lastIndexOf(upgrade);
            this.upgrades.remove(last);
            this.currentValue = this.baseValue;
            for (UpgradeModifier<T> u : this.upgrades) {
                this.currentValue = u.modify(this.currentValue);
            }
            if (!this.callbacks.isEmpty()) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Remove);
                }
            }
        }
    }

    /**
     * Removes the last occurrence of each modifier in the collection, then
     * recomputes the current value from scratch. Notifies callbacks once with
     * {@link CallbackReason#Remove}.
     *
     * @param upgrades the modifiers to remove (ignored if {@code null} or empty)
     */
    public void removeUpgrades(Collection<UpgradeModifier<T>> upgrades) {
        if (upgrades != null && !upgrades.isEmpty()) {
            for (UpgradeModifier<T> upgrade : upgrades) {
                int last = this.upgrades.lastIndexOf(upgrade);
                this.upgrades.remove(last);
            }
            this.currentValue = this.baseValue;
            for (UpgradeModifier<T> u : this.upgrades) {
                this.currentValue = u.modify(this.currentValue);
            }
            if (!this.callbacks.isEmpty()) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Remove);
                }
            }
        }
    }

    /**
     * Clears all modifiers and resets the current value to the base value.
     * Notifies all callbacks with {@link CallbackReason#Remove}.
     */
    public void removeAllUpgrades() {
        this.upgrades.clear();
        this.currentValue = this.baseValue;
        for (UpgradeCallback<T> callback : this.callbacks) {
            callback.run(this.currentValue, CallbackReason.Remove);
        }
    }

    /** Registers a callback to be notified whenever the computed value changes. */
    public void addCallback(UpgradeCallback<T> callback) {
        this.callbacks.add(callback);
    }

    /** Removes a previously registered callback. */
    public void removeCallback(UpgradeCallback<T> callback) {
        this.callbacks.remove(callback);
    }

    /** Indicates whether the value change was caused by adding or removing modifiers. */
    public enum CallbackReason {
        Add, Remove
    }

    /**
     * Listener interface notified whenever the {@link UpgradeComputer}'s current
     * value changes due to modifier addition or removal.
     *
     * @param <T> the value type matching the owning computer
     */
    public interface UpgradeCallback<T> {

        /**
         * Called after the computed value changes.
         *
         * @param newValue the recomputed current value
         * @param reason   whether modifiers were added or removed
         */
        void run(T newValue, CallbackReason reason);
    }
}
