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

import de.Keyle.MyPet.api.entity.PetType;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers per-pet-type permission nodes programmatically — one per
 * {@link PetType} for each of {@code MyPet.leash.*},
 * {@code MyPet.command.trade.offer.*}, and
 * {@code MyPet.command.trade.receive.*}.
 *
 * <p>The wildcard parents are declared in {@code plugin.yml} with
 * descriptions only; this class attaches the per-pet children at
 * runtime via {@link Permission#addParent(String, boolean)} so
 * {@code plugin.yml} stays clean of pet-type enumerations and
 * third-party pet types registered via
 * {@link PetType#register(String, Class)} pick up matching permissions
 * automatically.
 */
public final class PetPermissions {

    private static final String[] PARENT_PERMISSIONS = {
            "MyPet.leash",
            "MyPet.command.trade.offer",
            "MyPet.command.trade.receive"
    };

    private PetPermissions() {
    }

    /**
     * Registers per-pet permissions for every currently-registered
     * {@link PetType} and attaches each as a child of the matching
     * wildcard parent. Safe to call multiple times — already-registered
     * permissions are skipped silently.
     *
     * <p>Call after {@code BuiltInPetTypes.register()} and again whenever
     * a third-party plugin extends {@link PetType} (typically that
     * plugin's own {@code onEnable}).
     */
    public static void registerAll() {
        PluginManager pm = Bukkit.getPluginManager();
        Map<String, Permission> parents = resolveParents(pm);
        for (PetType petType : PetType.values()) {
            registerFor(pm, parents, petType);
        }
    }

    /**
     * Registers the three permission nodes for {@code petType} and links
     * each to its wildcard parent. Used by {@link #registerAll()} and by
     * third-party integrations that add pet types after MyPet's
     * {@code onLoad} has already run.
     */
    public static void registerFor(PluginManager pm, PetType petType) {
        registerFor(pm, resolveParents(pm), petType);
    }

    private static void registerFor(PluginManager pm, Map<String, Permission> parents, PetType petType) {
        for (String parent : PARENT_PERMISSIONS) {
            String name = parent + "." + petType.name();
            Permission permission = pm.getPermission(name);
            if (permission == null) {
                permission = new Permission(name);
                try {
                    pm.addPermission(permission);
                } catch (IllegalArgumentException ignored) {
                    permission = pm.getPermission(name);
                    if (permission == null) continue;
                }
            }
            Permission parentPermission = parents.get(parent);
            if (parentPermission != null) {
                permission.addParent(parentPermission, true);
            }
        }
    }

    /**
     * Pre-fetches each wildcard parent permission so child registrations
     * can use the {@link Permission#addParent(Permission, boolean)} overload
     * — the string overload auto-creates a missing parent and re-registers
     * it via {@code PluginManager}, triggering "already registered"
     * warnings against the plugin.yml-declared wildcards.
     */
    private static Map<String, Permission> resolveParents(PluginManager pm) {
        Map<String, Permission> parents = new HashMap<>();
        for (String parent : PARENT_PERMISSIONS) {
            Permission wildcard = pm.getPermission(parent + ".*");
            if (wildcard != null) {
                parents.put(parent, wildcard);
            }
        }
        return parents;
    }
}
