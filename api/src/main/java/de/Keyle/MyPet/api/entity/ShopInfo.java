/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define shop information for a pet type.
 * Applied to pet type interfaces (e.g., MyZombie, MyAllay) to enable
 * automatic inclusion in the generated pet-shops.yml file.
 * <p>
 * Only pets with this annotation will appear in the shop.
 * Pets implementing {@link MyPetBaby} will also appear in the "babies" shop.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShopInfo {

    /**
     * The description shown in the shop GUI lore.
     * If empty, defaults to "It's a {PetTypeName}!"
     */
    String description() default "";

    /**
     * The price to purchase this pet in the main shop.
     */
    double price() default 100.0;

    /**
     * The starting experience for the purchased pet.
     */
    double exp() default 0.0;

    /**
     * Additional options for the pet (e.g., "tamed", "size:2").
     * For baby variants, "baby" is added automatically.
     */
    String[] options() default {};

    /**
     * The display name in the shop GUI.
     * If empty, defaults to the pet type name with spaces (e.g., "ZombieVillager" → "Zombie Villager").
     */
    String displayName() default "";

    /**
     * The material name for the shop icon (e.g., "zombie_head").
     * If empty, uses the default icon based on the entity type.
     */
    String iconMaterial() default "";

    /**
     * The price for the baby variant in the babies shop.
     * If -1, defaults to price * 2.
     */
    double babyPrice() default -1.0;
}
