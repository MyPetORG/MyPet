/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.locale;

import java.util.*;

public class ResourceBundle extends java.util.ResourceBundle {
    private List<java.util.ResourceBundle> extensionBundles = new ArrayList<>();
    private boolean noParent = false;

    public ResourceBundle(java.util.ResourceBundle parent) {
        this.parent = parent;
    }

    public ResourceBundle() {
        noParent = true;
    }

    public void addExtensionBundle(java.util.ResourceBundle bundle) {
        if (bundle == null) {
            return;
        }

        this.extensionBundles.add(bundle);
    }

    @SuppressWarnings("unchecked")
    public Enumeration<String> getKeys() {
        Set keys = new HashSet();
        if (!noParent) {
            keys.addAll(this.parent.keySet());
        }

        for (java.util.ResourceBundle bundle : this.extensionBundles) {
            keys.addAll(bundle.keySet());
        }
        return Collections.enumeration(keys);
    }

    protected Object handleGetObject(String key) {
        Object object;

        if ((object = getObjectFromExtensionBundles(key)) != null) {
            return object;
        }
        return this.parent.getObject(key);
    }

    private Object getObjectFromExtensionBundles(String key) {
        if (this.extensionBundles.size() == 0) {
            return null;
        }
        try {
            Object object;
            for (java.util.ResourceBundle bundle : this.extensionBundles) {
                if ((object = bundle.getObject(key)) != null) {
                    return object;
                }
            }
        } catch (MissingResourceException ignored) {
        }
        return null;
    }
}