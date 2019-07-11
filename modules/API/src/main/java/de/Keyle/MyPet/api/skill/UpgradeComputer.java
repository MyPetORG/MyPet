/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

public class UpgradeComputer<T> {

    List<UpgradeModifier<T>> upgrades = new LinkedList<>();
    List<UpgradeCallback<T>> callbacks = new LinkedList<>();
    T currentValue;
    T baseValue;

    public enum CallbackReason {
        Add, Remove
    }

    public UpgradeComputer(T baseValue) {
        this.baseValue = baseValue;
        this.currentValue = this.baseValue;
    }

    public T getValue() {
        return this.currentValue;
    }

    public void addUpgrade(UpgradeModifier<T> upgrade) {
        if (upgrade != null) {
            this.currentValue = upgrade.modify(this.currentValue);
            this.upgrades.add(upgrade);
            if (this.callbacks.size() > 0) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Add);
                }
            }
        }
    }

    public void addUpgrades(Collection<UpgradeModifier<T>> upgrades) {
        if (upgrades != null && upgrades.size() > 0) {
            for (UpgradeModifier<T> upgrade : upgrades) {
                this.currentValue = upgrade.modify(this.currentValue);
                this.upgrades.add(upgrade);
            }
            if (this.callbacks.size() > 0) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Add);
                }
            }
        }
    }

    public void removeUpgrade(UpgradeModifier<T> upgrade) {
        if (upgrade != null) {
            int last = this.upgrades.lastIndexOf(upgrade);
            this.upgrades.remove(last);
            this.currentValue = this.baseValue;
            for (UpgradeModifier<T> u : this.upgrades) {
                this.currentValue = u.modify(this.currentValue);
            }
            if (this.callbacks.size() > 0) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Remove);
                }
            }
        }
    }

    public void removeUpgrades(Collection<UpgradeModifier<T>> upgrades) {
        if (upgrades != null && upgrades.size() > 0) {
            for (UpgradeModifier<T> upgrade : upgrades) {
                int last = this.upgrades.lastIndexOf(upgrade);
                this.upgrades.remove(last);
            }
            this.currentValue = this.baseValue;
            for (UpgradeModifier<T> u : this.upgrades) {
                this.currentValue = u.modify(this.currentValue);
            }
            if (this.callbacks.size() > 0) {
                for (UpgradeCallback<T> callback : this.callbacks) {
                    callback.run(this.currentValue, CallbackReason.Remove);
                }
            }
        }
    }

    public void removeAllUpgrades() {
        this.upgrades.clear();
        this.currentValue = this.baseValue;
        if (this.callbacks != null) {
            for (UpgradeCallback<T> callback : this.callbacks) {
                callback.run(this.currentValue, CallbackReason.Remove);
            }
        }
    }

    public void addCallback(UpgradeCallback<T> callback) {
        this.callbacks.add(callback);
    }

    public void removeCallback(UpgradeCallback<T> callback) {
        this.callbacks.remove(callback);
    }

    public interface UpgradeCallback<T> {

        void run(T newValue, CallbackReason reason);
    }

    @Override
    public String toString() {
        return "" + currentValue;
    }
}
