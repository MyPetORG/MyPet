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

package de.Keyle.MyPet.api.entity;

import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the out-of-box defaults for a pet type. Applied to
 * {@code My<Type>} classes in the plugin module alongside
 * {@link ShopInfo}. Values are read reflectively by
 * {@code MyPetInfoImpl} at startup and used as fallbacks when no
 * per-type override exists in {@code pet-config.yml}.
 * <p>
 * Admins may override every value via config; this annotation merely
 * seeds the initial defaults so the plugin is usable without any YAML
 * customization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultInfo {

    /** Items that restore saturation when fed to this pet type. */
    Material[] food() default {};

    /** Starting max health (half-hearts). */
    double hp() default 20D;

    /**
     * Leash flag names that must pass before a player can tame this mob.
     * Defaults to {@code "LowHp"} — the mob must be at low health.
     */
    String[] leashFlags() default {"LowHp"};

    /** Base movement speed (Bukkit MOVEMENT_SPEED attribute value). */
    double walkSpeed() default 0.30D;

    /** Item that forces a baby pet to grow up when right-clicked. */
    Material growUpItem() default Material.EXPERIENCE_BOTTLE;
}