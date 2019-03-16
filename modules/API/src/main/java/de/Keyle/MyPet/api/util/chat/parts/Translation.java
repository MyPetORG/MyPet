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

/*
 * This file is part of FancifulChat
 *
 * Copyright (C) 2011-2016 Keyle
 * FancifulChat is licensed under the GNU Lesser General Public License.
 *
 * FancifulChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FancifulChat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.chat.parts;

import de.Keyle.MyPet.api.util.chat.FancyMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Translation extends MessagePart {

    final String identifier;
    Object[] using = null;

    public Translation(final String identifier) {
        this.identifier = identifier;
    }

    public Translation(final String identifier, Object... using) {
        this.identifier = identifier;
        this.using = using;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("translate", identifier);
        if (using != null) {
            JSONArray using = new JSONArray();
            for (Object o : this.using) {
                if (o instanceof MessagePart) {
                    using.add(((MessagePart) o).toJson());
                } else if (o instanceof FancyMessage) {
                    using.add(((FancyMessage) o).toJSON());
                } else {
                    using.add(o.toString());
                }
            }
            json.put("with", using);
        }
        return json;
    }
}