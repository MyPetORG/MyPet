/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util.locale;

import de.Keyle.MyPet.util.logger.DebugLogger;

public class Country {
    private Language language;
    private String code;
    private boolean loaded = false;
    private ResourceBundle translations = null;

    public Country(Language language, String code) {
        this.language = language;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String translate(String key) {
        load();

        if (translations != null) {
            if (translations.containsKey(key)) {
                return translations.getString(key);
            }
        }
        return key;
    }

    private void load() {
        if (!loaded) {
            loaded = true;
            translations = Translation.loadLocale(language.getCode() + "_" + getCode());
            DebugLogger.info("Locale \"" + language.getCode() + "_" + getCode() + "\" " + (translations != null ? "loaded" : "not found"));
        }
    }
}