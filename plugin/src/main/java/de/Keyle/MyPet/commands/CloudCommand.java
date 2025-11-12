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

package de.Keyle.MyPet.commands;

/**
 * Interface for command handlers using Cloud Command Framework.
 * <p>
 * Commands should implement this interface and use the register method
 * to build their command structure using Cloud's builder API.
 */
public interface CloudCommand {

    /**
     * Registers this command with the Cloud command manager.
     * <p>
     * Implementations should use the manager's getCommandManager() to
     * access Cloud's builder API and construct their command.
     *
     * @param manager the Cloud command manager
     */
    void register(CloudCommandManager manager);
}
