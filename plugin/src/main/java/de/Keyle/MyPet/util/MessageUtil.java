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

package de.Keyle.MyPet.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Utility class for creating standardized Adventure API messages
 */
public class MessageUtil {

    /**
     * Standard MyPet plugin prefix: [MyPet]
     * Format: [<AQUA>MyPet<RESET>]
     *
     * @return Component with plugin prefix
     */
    public static Component pluginPrefix() {
        return Component.text()
                .append(Component.text("["))
                .append(Component.text("MyPet").color(NamedTextColor.AQUA))
                .append(Component.text("]"))
                .build();
    }

    /**
     * Creates a success message with optional plugin prefix
     * Format: [MyPet] ✔ <message in GREEN> (if withPrefix=true)
     * ✔ <message in GREEN> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with success message in GREEN
     */
    public static Component success(String message, boolean withPrefix) {
        return success(Component.text(message), withPrefix);
    }

    /**
     * Creates a success message with optional plugin prefix
     * Format: [MyPet] ✔ <message in GREEN> (if withPrefix=true)
     * ✔ <message in GREEN> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with success message in GREEN
     */
    public static Component success(Component message, boolean withPrefix) {
        Component successMessage = Component.text()
                .append(Component.text("✔ ").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN))
                .append(message.color(NamedTextColor.GREEN))
                .build();

        if (withPrefix) {
            return pluginPrefix()
                    .append(Component.space())
                    .append(successMessage);
        }
        return successMessage;
    }

    /**
     * Creates an error message with optional plugin prefix
     * Format: [MyPet] ✘ <message in RED> (if withPrefix=true)
     * ✘ <message in RED> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with error message in RED
     */
    public static Component error(String message, boolean withPrefix) {
        return error(Component.text(message), withPrefix);
    }

    /**
     * Creates an error message with optional plugin prefix
     * Format: [MyPet] ✘ <message in RED> (if withPrefix=true)
     * ✘ <message in RED> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with error message in RED
     */
    public static Component error(Component message, boolean withPrefix) {
        Component errorMessage = Component.text()
                .append(Component.text("✘ ").decorate(TextDecoration.BOLD).color(NamedTextColor.RED))
                .append(message.color(NamedTextColor.RED))
                .build();

        if (withPrefix) {
            return pluginPrefix()
                    .append(Component.space())
                    .append(errorMessage);
        }
        return errorMessage;
    }

    /**
     * Creates an info message with optional plugin prefix
     * Format: [MyPet] <message in GRAY> (if withPrefix=true)
     * <message in GRAY> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with info message in GRAY
     */
    public static Component info(String message, boolean withPrefix) {
        return info(Component.text(message), withPrefix);
    }

    /**
     * Creates an info message with optional plugin prefix
     * Format: [MyPet] <message in GRAY> (if withPrefix=true)
     * <message in GRAY> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with info message in GRAY
     */
    public static Component info(Component message, boolean withPrefix) {
        Component coloredMessage = message.color(NamedTextColor.GRAY);
        if (withPrefix) {
            return pluginPrefix()
                    .append(Component.space())
                    .append(coloredMessage);
        }
        return coloredMessage;
    }

    /**
     * Creates a warning message with optional plugin prefix
     * Format: [MyPet] <message in YELLOW> (if withPrefix=true)
     * <message in YELLOW> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with warning message in YELLOW
     */
    public static Component warning(String message, boolean withPrefix) {
        return warning(Component.text(message), withPrefix);
    }

    /**
     * Creates a warning message with optional plugin prefix
     * Format: [MyPet] <message in YELLOW> (if withPrefix=true)
     * <message in YELLOW> (if withPrefix=false)
     *
     * @param message    Message to display
     * @param withPrefix If true, the message will be prefixed with the plugin prefix
     * @return Component with warning message in YELLOW
     */
    public static Component warning(Component message, boolean withPrefix) {
        Component coloredMessage = message.color(NamedTextColor.YELLOW);
        if (withPrefix) {
            return pluginPrefix()
                    .append(Component.space())
                    .append(coloredMessage);
        }
        return coloredMessage;
    }

    /**
     * Creates a pet name component in AQUA color
     * This is the standard color for pet names throughout MyPet
     *
     * @param name Pet name
     * @return Component with pet name in AQUA
     */
    public static Component petName(String name) {
        return Component.text(name).color(NamedTextColor.AQUA);
    }

    /**
     * Creates a value component in GOLD color
     * This is the standard color for values/numbers in MyPet displays
     *
     * @param value Value to display
     * @return Component with value in GOLD
     */
    public static Component value(Object value) {
        return Component.text(String.valueOf(value)).color(NamedTextColor.GOLD);
    }

    /**
     * Creates a header component (typically used in help/info commands)
     * Format: ===== <header text in GOLD> =====
     *
     * @param text Header text
     * @return Component with header text in GOLD and bold style
     */
    public static Component header(String text) {
        return header(Component.text(text));
    }

    /**
     * Creates a header component with a Component title
     * Format: ===== <header Component> =====
     *
     * @param text Header text
     * @return Component with header text in GOLD and bold style
     */
    public static Component header(Component text) {
        return Component.text()
                .append(Component.text("===== ").color(NamedTextColor.GRAY))
                .append(text.color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" =====").color(NamedTextColor.GRAY))
                .build();
    }

    /**
     * Creates a label:value pair with standard formatting
     * Format: <label>: <value in GOLD>
     *
     * @param label Label text
     * @param value Value text
     * @return Component with label:value pair
     */
    public static Component labelValue(String label, Object value) {
        return Component.text()
                .append(Component.text(label + ": "))
                .append(Component.text(String.valueOf(value)).color(NamedTextColor.GOLD))
                .build();
    }

    /**
     * Creates a separator line
     * Format: ------------------
     *
     * @return Component with separator line
     */
    public static Component separator() {
        return Component.text("------------------").color(NamedTextColor.GRAY);
    }
}
