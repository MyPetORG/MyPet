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

package de.Keyle.MyPet.api.skill.modifier;

import java.math.BigDecimal;

public class UpgradeNumberModifier implements UpgradeModifier<Number> {
    public enum Type {
        Add, Subtract
    }

    Number value;
    Type type;

    public UpgradeNumberModifier(Number value, Type type) {
        this.value = value;
        this.type = type;
    }

    public Number getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public Number modify(Number n) {
        switch (type) {
            case Add:
                return new BigDecimal(n.toString()).add(new BigDecimal(value.toString()));
            case Subtract:
                return new BigDecimal(n.toString()).subtract(new BigDecimal(value.toString()));
            default:
                return value;
        }
    }

    public Number invert(Number n) {
        switch (type) {
            case Add:
                return new BigDecimal(n.toString()).subtract(new BigDecimal(value.toString()));
            case Subtract:
                return new BigDecimal(n.toString()).add(new BigDecimal(value.toString()));
            default:
                return value;
        }
    }

    @Override
    public String toString() {
        return "{" + type.name() + ": " + value + '}';
    }
}