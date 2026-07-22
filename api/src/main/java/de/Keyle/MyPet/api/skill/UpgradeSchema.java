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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Declarative description of a skill's upgrade JSON fields, registered alongside the
 * {@link UpgradeParser} and shipped to the web editor as skill metadata.
 */
public final class UpgradeSchema {

    public enum FieldType {
        NUMBER, INTEGER, BOOLEAN, ENUM, GROUP, LIST, STRING
    }

    /** One upgrade field: name (the exact JSON key the parser reads), type, and display hints. */
    public static final class Field {
        private final String name;
        private final FieldType type;
        private String label;
        private String suffix;
        private boolean cumulative;
        private List<String> enumValues = List.of();
        private List<Field> children = List.of();

        private Field(String name, FieldType type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public FieldType getType() {
            return type;
        }

        /** Literal English label; used as the "en" fallback when no translation key exists. */
        public String getLabel() {
            return label;
        }

        public String getSuffix() {
            return suffix;
        }

        public boolean isCumulative() {
            return cumulative;
        }

        public List<String> getEnumValues() {
            return enumValues;
        }

        public List<Field> getChildren() {
            return children;
        }
    }

    private final List<Field> fields;

    private UpgradeSchema(List<Field> fields) {
        this.fields = Collections.unmodifiableList(fields);
    }

    public List<Field> fields() {
        return fields;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder; {@code label}/{@code suffix}/{@code cumulative} modify the last-added field. */
    public static final class Builder {
        private final List<Field> fields = new ArrayList<>();
        private Field last;

        private Builder add(Field field) {
            fields.add(field);
            last = field;
            return this;
        }

        public Builder number(String name) {
            return add(new Field(name, FieldType.NUMBER));
        }

        public Builder integer(String name) {
            return add(new Field(name, FieldType.INTEGER));
        }

        /** A free-text field; the editor renders a text input. Used for pasted item strings. */
        public Builder string(String name) {
            return add(new Field(name, FieldType.STRING));
        }

        public Builder bool(String name) {
            return add(new Field(name, FieldType.BOOLEAN));
        }

        public Builder enumOf(String name, Class<? extends Enum<?>> enumClass) {
            Field field = new Field(name, FieldType.ENUM);
            field.enumValues = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
            return add(field);
        }

        /** An enum field whose allowed values come from a runtime list (e.g. a server registry dump). */
        public Builder enumValues(String name, List<String> values) {
            Field field = new Field(name, FieldType.ENUM);
            field.enumValues = List.copyOf(values);
            return add(field);
        }

        public Builder group(String name, Consumer<Builder> children) {
            Field field = new Field(name, FieldType.GROUP);
            Builder inner = new Builder();
            children.accept(inner);
            field.children = List.copyOf(inner.fields);
            return add(field);
        }

        /**
         * A repeatable list field: the editor renders zero or more rows, each row carrying the
         * {@code row} fields. Unlike {@link #group}, the value is an array of row objects, and the row's
         * numeric fields are absolute values (not additive {@code +n} modifiers).
         */
        public Builder list(String name, Consumer<Builder> row) {
            Field field = new Field(name, FieldType.LIST);
            Builder inner = new Builder();
            row.accept(inner);
            field.children = List.copyOf(inner.fields);
            return add(field);
        }

        public Builder label(String literal) {
            requireLast().label = literal;
            return this;
        }

        public Builder suffix(String suffix) {
            requireLast().suffix = suffix;
            return this;
        }

        public Builder cumulative() {
            requireLast().cumulative = true;
            return this;
        }

        private Field requireLast() {
            if (last == null) {
                throw new IllegalStateException("add a field before setting label/suffix/cumulative");
            }
            return last;
        }

        public UpgradeSchema build() {
            return new UpgradeSchema(new ArrayList<>(fields));
        }
    }
}
