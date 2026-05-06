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

package de.Keyle.MyPet.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;

/**
 * A Brigadier {@link CommandExceptionType} that carries an Adventure
 * {@link Component} as its error message.
 *
 * <p>Brigadier's built-in exception types ({@code SimpleCommandExceptionType},
 * {@code DynamicCommandExceptionType}, etc.) accept only raw {@link Message}
 * instances. This record bridges the gap by accepting a rich Adventure
 * {@link Component} and converting it to a Brigadier {@link Message} via
 * Paper's {@link MessageComponentSerializer}. This allows command error
 * messages to use translatable components, colors, and other Adventure
 * formatting features.</p>
 *
 * <p>Two constructors are provided:</p>
 * <ul>
 *     <li>{@link #ComponentCommandExceptionType(Component)} &mdash; accepts an
 *         Adventure {@link Component} and serializes it to a Brigadier
 *         {@link Message} automatically.</li>
 *     <li>{@link #ComponentCommandExceptionType(Message)} &mdash; the canonical
 *         record constructor; accepts a pre-serialized {@link Message}
 *         directly.</li>
 * </ul>
 *
 * @param message the Brigadier-compatible error message displayed to the player
 */
public record ComponentCommandExceptionType(Message message) implements CommandExceptionType {

    /**
     * Convenience constructor that converts an Adventure {@link Component}
     * into a Brigadier {@link Message} using Paper's
     * {@link MessageComponentSerializer}.
     *
     * @param message the Adventure component to use as the error message
     */
    public ComponentCommandExceptionType(final Component message) {
        this(MessageComponentSerializer.message().serialize(message));
    }

    /**
     * Creates a {@link CommandSyntaxException} with this type's error message
     * and no input context (cursor position).
     *
     * @return a new command syntax exception ready to be thrown
     */
    public CommandSyntaxException create() {
        return new CommandSyntaxException(this, this.message);
    }

    /**
     * Creates a {@link CommandSyntaxException} with this type's error message
     * and the current parsing context from the given {@link ImmutableStringReader}.
     *
     * <p>The reader's string and cursor position are included in the
     * exception, enabling Brigadier to highlight the erroneous portion of
     * the player's input in the client-side error display.</p>
     *
     * @param reader the string reader whose position indicates where the
     *               error occurred in the command input
     * @return a new command syntax exception with input context
     */
    public CommandSyntaxException createWithContext(final ImmutableStringReader reader) {
        return new CommandSyntaxException(this, this.message, reader.getString(), reader.getCursor());
    }
}
