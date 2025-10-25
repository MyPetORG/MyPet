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

package de.Keyle.MyPet.api.util.locale;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Properties;

public class TranslationBundle {
    HashMap<String, String> translations = new HashMap<>();

    public TranslationBundle() {
    }

    public TranslationBundle(Reader reader) {
        load(reader);
    }

    public void load(Reader reader) {
        Properties properties = new Properties();

        try {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Object o : properties.keySet()) {
            translations.put(o.toString().toLowerCase(), properties.get(o).toString());
        }
    }

    public boolean containsKey(String key) {
        return translations.containsKey(key.toLowerCase());
    }

    public String getString(String key) {
        return translations.get(key.toLowerCase());
    }
}