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

import com.google.gson.JsonObject;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

@FunctionalInterface
public interface UpgradeParser<S extends Skill> {

    /**
     * Parses an {@link Upgrade} for skill {@code S} from a skilltree JSON node.
     *
     * <p>Called by the skilltree loader for each {@code Skills.<name>.Upgrades.<level>}
     * block in an {@code .st.json} file. The {@code upgradeJson} object is the JSON
     * object directly under the level rule (e.g. {@code {"damage": "+5"}}).
     *
     * <p>Return {@code null} if the JSON cannot be parsed into a valid upgrade —
     * the loader will log and skip the entry. Returning an upgrade with no
     * modifiers set (e.g., all fields absent from the JSON) is also valid; the
     * loader will install it as a no-op upgrade for that level.
     */
    Upgrade<S> parse(JsonObject upgradeJson);
}
