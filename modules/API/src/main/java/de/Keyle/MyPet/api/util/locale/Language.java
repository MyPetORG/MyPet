/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import java.util.HashMap;
import java.util.Map;

public class Language {
    private boolean loaded = false;
    private String code;
    private Map<String, Country> countries = new HashMap<>();
    private ResourceBundle translations = null;

    public Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String translate(String key, String country) {
        if (!countries.containsKey(country)) {
            countries.put(country, new Country(this, country));
        }

        String translated = countries.get(country).translate(key);
        if (!translated.equals(key)) {
            return translated;
        }

        load();

        if (translations != null) {
            if (translations.containsKey(key)) {
                translated = translations.getString(key);
            }
        }
        return translated;
    }

    public String translate(String key) {
        load();
        String translated = key;
        if (translations != null) {
            if (translations.containsKey(key)) {
                translated = translations.getString(key);
            }
        }
        return translated;
    }

    private void load() {
        if (!loaded) {
            loaded = true;
            translations = Translation.loadLocale(getCode());
        }
    }
}