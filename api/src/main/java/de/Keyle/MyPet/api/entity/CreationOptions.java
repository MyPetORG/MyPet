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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the per-type creation options accepted by
 * {@code /petadmin create <type> [options...]} and the petshop YAML
 * {@code Options:} list. Applied to {@code Pet<Type>} classes in the
 * plugin module alongside {@link ShopInfo} and {@link DefaultInfo}.
 * <p>
 * Read at startup by the command's tab-completion assembler and by the
 * petshop checkout flow to decide which option strings are accepted for
 * the type. Types that do not need any custom options simply omit the
 * annotation.
 * <p>
 * The {@code baby} option is contributed automatically for any class
 * that implements {@link PetBaby} — do not list it here.
 *
 * @see PetBaby for the marker that auto-contributes the {@code baby} option.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CreationOptions {

    /**
     * The option strings accepted by this pet type. Examples: {@code "variant:"},
     * {@code "saddle"}, {@code "type:red"}, {@code "main-gene:lazy"}. Order is
     * preserved in tab-completion suggestions.
     */
    String[] value() default {};
}
