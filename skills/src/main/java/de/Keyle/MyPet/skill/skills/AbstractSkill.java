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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.locale.Locale;
import net.kyori.adventure.text.Component;

/**
 * Shared base for MyPet's built-in skills. Holds the owning {@link Pet} and the
 * {@code getPet()} accessor that every skill otherwise repeats, plus the
 * {@link #upgradeMessage} helper for the common single-line upgrade notice.
 *
 * <p>Built-ins only — this class lives in the {@code skills} module and is not part
 * of the public API. Third-party skills implement {@link Skill} (and the capability
 * markers) directly.
 */
public abstract class AbstractSkill implements Skill {

    protected final Pet pet;

    protected AbstractSkill(Pet pet) {
        this.pet = pet;
    }

    @Override
    public Pet getPet() {
        return pet;
    }

    /**
     * Builds the standard one-line upgrade message: {@code key} formatted against the
     * owner's locale with the pet's display name as the first argument, followed by
     * {@code args}. Mirrors the {@code Locale.getFormattedComponent(key, language,
     * displayName, ...)} shape repeated across most skills' {@code getUpgradeMessage()}.
     */
    protected Component upgradeMessage(String key, Object... args) {
        Object[] values = new Object[args.length + 1];
        values[0] = pet.getDisplayName();
        System.arraycopy(args, 0, values, 1, args.length);
        return Locale.getFormattedComponent(key, pet.getOwner().getLanguage(), values);
    }
}
