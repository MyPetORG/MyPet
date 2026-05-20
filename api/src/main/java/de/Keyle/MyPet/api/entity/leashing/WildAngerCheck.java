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

package de.Keyle.MyPet.api.entity.leashing;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.function.Predicate;

/**
 * Per-species predicate that decides whether a wild Bukkit mob is currently
 * "angry" — consumed by capture-time leash flags (see {@code AngryFlag}).
 * Wraps the species-specific Bukkit API (e.g. {@code Wolf#isAngry},
 * {@code Bee#getAnger}, {@code Warden#getAngerLevel}) so callers can ask
 * without an {@code instanceof} chain.
 *
 * <p>Declared as a static field on the matching {@code PetXxx} class:
 *
 * <pre>{@code
 * public static final WildAngerCheck<Wolf> ANGER_CHECK =
 *     new WildAngerCheck<>(Wolf.class, Wolf::isAngry);
 * }</pre>
 *
 * <p>Note the semantic inversion vs. other per-pet handles: this describes
 * the wild mob's state <em>before</em> it becomes a pet, not the pet's own
 * runtime state. The pet class is still the right declaration site because
 * it is v4's single source of truth for everything species-specific.
 *
 * <p>Construction self-registers with {@link WildAngerCheckRegistry}; the
 * leash-flag check looks the entry up via
 * {@link WildAngerCheckRegistry#forEntity}.
 *
 * @param <T> the Bukkit mob type this check applies to
 */
public final class WildAngerCheck<T extends Mob> {

    private final Class<T> mobClass;
    private final Predicate<T> predicate;

    public WildAngerCheck(Class<T> mobClass, Predicate<T> predicate) {
        this.mobClass = mobClass;
        this.predicate = predicate;
        WildAngerCheckRegistry.register(this);
    }

    public Class<T> mobClass() {
        return mobClass;
    }

    public boolean test(LivingEntity entity) {
        return predicate.test(mobClass.cast(entity));
    }
}
