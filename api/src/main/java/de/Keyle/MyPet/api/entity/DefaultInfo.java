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
 * {@code Pet<Type>} classes in the plugin module alongside
 * {@link ShopInfo}. Values are read reflectively by
 * {@code PetInfoImpl} at startup and used as fallbacks when no
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

    /**
     * Starting max health (half-hearts). The default {@code -1} means "inherit the
     * vanilla entity's natural max-health" (e.g. Wolf 8, Cow 10) — resolved from the
     * Bukkit entity type at config load. Set an explicit non-negative value only to
     * override that per species.
     */
    double hp() default -1D;

    /**
     * Leash flag names that must pass before a player can tame this mob.
     * Defaults to {@code "LowHp"} — the mob must be at low health.
     */
    String[] leashFlags() default {"LowHp"};

    /** Base movement speed (Bukkit MOVEMENT_SPEED attribute value). */
    double walkSpeed() default 0.30D;

    /**
     * If {@code true}, the Ride skill's flight controller uses
     * {@link #flySpeed()} verbatim as the base ride speed for this pet,
     * skipping vanilla-attribute derivation entirely. If {@code false}
     * (the default), the controller reads the mob's {@code FLYING_SPEED}
     * attribute (naturally-flying species) or {@code MOVEMENT_SPEED}
     * attribute (ground/aquatic pets that gained flight via the Ride
     * skilltree's {@code CanFly} upgrade) and scales it into
     * direct-velocity units — third-party plugins setting either attribute
     * propagate to ride speed automatically.
     * <p>
     * Use the override when a specific pet type's vanilla attribute value
     * produces a feel that's wrong for ridden flight — e.g. naturally
     * aggressive flyers like Phantom whose vanilla {@code FLYING_SPEED}
     * is high enough to feel uncontrollable when scaled.
     */
    boolean overrideFlySpeed() default false;

    /**
     * Base ride speed (blocks per tick, direct-velocity units) used by the
     * Ride skill's flight controller when {@link #overrideFlySpeed()} is
     * {@code true}. Ignored when {@code overrideFlySpeed} is {@code false}
     * — in that case the controller derives speed from the live Bukkit
     * mob's {@code FLYING_SPEED} / {@code MOVEMENT_SPEED} attribute
     * scaled by vanilla-physics conversion factors.
     * <p>
     * Authored in direct-per-tick-velocity units, NOT vanilla's
     * force-coefficient units — e.g. {@code 0.6} ≈ creative-fly cruise.
     */
    double flySpeed() default 0.6D;

    /** Item that forces a baby pet to grow up when right-clicked. */
    Material growUpItem() default Material.EXPERIENCE_BOTTLE;

    /**
     * Material name used by the selection-GUI egg-icon service when the
     * standard {@code <TYPE>_SPAWN_EGG} doesn't exist for this pet
     * (e.g., EnderDragon, SnowGolem, Wither). Empty falls through to a
     * glowing BARRIER icon.
     */
    String fallbackIconMaterial() default "";

    /**
     * Whether the fallback icon should glow. Ignored if
     * {@link #fallbackIconMaterial()} is empty.
     */
    boolean fallbackIconGlow() default false;
}