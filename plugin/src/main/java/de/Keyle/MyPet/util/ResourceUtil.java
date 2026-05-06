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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.api.util.ErrorUtil;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class ResourceUtil {

    private ResourceUtil() {
    }

    public static boolean copyResource(Plugin plugin, String resource, File destination) {
        try (InputStream template = plugin.getResource(resource);
             OutputStream out = Files.newOutputStream(destination.toPath())) {
            if (template == null) {
                return false;
            }
            template.transferTo(out);
            return true;
        } catch (IOException e) {
            ErrorUtil.report(e);
            return false;
        }
    }
}
