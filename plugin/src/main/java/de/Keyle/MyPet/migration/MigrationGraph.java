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

package de.Keyle.MyPet.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MigrationGraph {

    public static class MigrationEntry {
        private final Class<?> migrationClass;
        private final Migration annotation;
        private final MigrationDomain domain;
        private final String id;

        public MigrationEntry(Class<?> migrationClass, Migration annotation, MigrationDomain domain) {
            this.migrationClass = migrationClass;
            this.annotation = annotation;
            this.domain = domain;
            this.id = migrationClass.getSimpleName();
        }

        public Class<?> getMigrationClass() {
            return migrationClass;
        }

        public Migration getAnnotation() {
            return annotation;
        }

        public MigrationDomain getDomain() {
            return domain;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return annotation.version();
        }

        public String getMinecraftVersion() {
            return annotation.minecraftVersion();
        }

        public boolean hasVersion() {
            return !annotation.version().isEmpty();
        }

        public boolean hasMinecraftVersion() {
            return !annotation.minecraftVersion().isEmpty();
        }

        public boolean isMcVersionOnly() {
            return !hasVersion() && hasMinecraftVersion();
        }

        public String getSortVersion() {
            return hasVersion() ? annotation.version() : annotation.minecraftVersion();
        }
    }

    /**
     * Resolves execution order for pending migrations, using the full discovered set
     * for dependency validation. Already-completed migrations are excluded from the
     * result but their IDs are recognized as valid dependsOn targets.
     */
    public static List<MigrationEntry> resolveForPending(List<MigrationEntry> allEntries,
                                                          Set<String> completedIds) {
        List<MigrationEntry> ordered = resolve(allEntries);
        return ordered.stream()
                .filter(e -> !completedIds.contains(e.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves execution order via the four-phase algorithm:
     * 1. Separate version-tied and MC-version-only migrations
     * 2. Sort each group by relevant version
     * 3. Within each version, sort by domain priority
     * 4. Apply dependency overrides via topological sort
     *
     * @throws IllegalStateException if circular dependencies or invalid references are detected
     */
    public static List<MigrationEntry> resolve(List<MigrationEntry> entries) {
        Map<String, MigrationEntry> byId = new HashMap<>();
        for (MigrationEntry entry : entries) {
            if (byId.containsKey(entry.getId())) {
                throw new IllegalStateException("Duplicate migration ID: " + entry.getId());
            }
            byId.put(entry.getId(), entry);
        }

        for (MigrationEntry entry : entries) {
            if (!entry.hasVersion() && !entry.hasMinecraftVersion()) {
                throw new IllegalStateException(
                        "Migration " + entry.getId() + " must have at least one of version or minecraftVersion set.");
            }
        }

        for (MigrationEntry entry : entries) {
            for (String dep : entry.getAnnotation().dependsOn()) {
                MigrationEntry depEntry = byId.get(dep);
                if (depEntry == null) {
                    throw new IllegalStateException(
                            "Migration " + entry.getId() + " depends on unknown migration: " + dep);
                }
                if (!entry.isMcVersionOnly() && !depEntry.isMcVersionOnly()) {
                    if (depEntry.hasVersion() && entry.hasVersion()
                            && compareVersions(depEntry.getVersion(), entry.getVersion()) > 0) {
                        throw new IllegalStateException(
                                "Migration " + entry.getId() + " (v" + entry.getVersion()
                                        + ") depends on " + dep + " (v" + depEntry.getVersion()
                                        + ") which is a later version.");
                    }
                } else if (entry.isMcVersionOnly() && depEntry.isMcVersionOnly()) {
                    if (compareVersions(depEntry.getMinecraftVersion(), entry.getMinecraftVersion()) > 0) {
                        throw new IllegalStateException(
                                "Migration " + entry.getId() + " (MC " + entry.getMinecraftVersion()
                                        + ") depends on " + dep + " (MC " + depEntry.getMinecraftVersion()
                                        + ") which is a later MC version.");
                    }
                }
            }
        }

        List<MigrationEntry> versionTied = new ArrayList<>();
        List<MigrationEntry> mcVersionOnly = new ArrayList<>();
        for (MigrationEntry entry : entries) {
            if (entry.isMcVersionOnly()) {
                mcVersionOnly.add(entry);
            } else {
                versionTied.add(entry);
            }
        }

        List<MigrationEntry> presorted = new ArrayList<>();
        presorted.addAll(sortByVersionAndDomain(versionTied, MigrationEntry::getVersion));
        presorted.addAll(sortByVersionAndDomain(mcVersionOnly, MigrationEntry::getMinecraftVersion));

        return topologicalSort(presorted, byId);
    }

    private static List<MigrationEntry> sortByVersionAndDomain(
            List<MigrationEntry> entries,
            Function<MigrationEntry, String> versionExtractor) {
        if (entries.isEmpty()) {
            return entries;
        }

        Map<String, List<MigrationEntry>> byVersion = new LinkedHashMap<>();
        entries.stream()
                .map(versionExtractor)
                .distinct()
                .sorted(MigrationGraph::compareVersions)
                .forEach(v -> byVersion.put(v, new ArrayList<>()));
        for (MigrationEntry entry : entries) {
            byVersion.get(versionExtractor.apply(entry)).add(entry);
        }

        for (List<MigrationEntry> versionEntries : byVersion.values()) {
            versionEntries.sort(Comparator
                    .comparingInt((MigrationEntry e) -> e.getDomain().getPriority())
                    .thenComparing(MigrationEntry::getId));
        }

        List<MigrationEntry> result = new ArrayList<>();
        for (List<MigrationEntry> versionEntries : byVersion.values()) {
            result.addAll(versionEntries);
        }
        return result;
    }

    private static List<MigrationEntry> topologicalSort(
            List<MigrationEntry> presorted, Map<String, MigrationEntry> byId) {
        Map<String, Set<String>> adjList = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (MigrationEntry entry : presorted) {
            adjList.put(entry.getId(), new HashSet<>());
            inDegree.put(entry.getId(), 0);
        }

        for (MigrationEntry entry : presorted) {
            for (String dep : entry.getAnnotation().dependsOn()) {
                if (adjList.containsKey(dep)) {
                    adjList.get(dep).add(entry.getId());
                    inDegree.merge(entry.getId(), 1, Integer::sum);
                }
            }
        }

        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < presorted.size(); i++) {
            positionMap.put(presorted.get(i).getId(), i);
        }

        PriorityQueue<String> queue = new PriorityQueue<>(
                Comparator.comparingInt(positionMap::get));

        for (MigrationEntry entry : presorted) {
            if (inDegree.get(entry.getId()) == 0) {
                queue.add(entry.getId());
            }
        }

        List<MigrationEntry> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(byId.get(current));

            for (String neighbor : adjList.get(current)) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (result.size() != presorted.size()) {
            Set<String> resolvedIds = result.stream()
                    .map(MigrationEntry::getId)
                    .collect(Collectors.toSet());
            List<String> remaining = presorted.stream()
                    .map(MigrationEntry::getId)
                    .filter(id -> !resolvedIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalStateException(
                    "Circular migration dependency detected! Involved migrations: "
                            + String.join(" → ", remaining));
        }

        return result;
    }

    /**
     * Compare two dot-separated numeric version strings (e.g., "4.0.0" vs "4.1.0").
     * Missing components are treated as 0, so "4.0" compares equal to "4.0.0".
     */
    static int compareVersions(String a, String b) {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        int length = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < length; i++) {
            int aPart = i < aParts.length ? parsePart(aParts[i]) : 0;
            int bPart = i < bParts.length ? parsePart(bParts[i]) : 0;
            if (aPart != bPart) {
                return Integer.compare(aPart, bPart);
            }
        }
        return 0;
    }

    private static int parsePart(String part) {
        // Strip any non-digit suffix (e.g., "1.21.1-R0.1-SNAPSHOT" → "1", "21", "1")
        StringBuilder sb = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.length() == 0 ? 0 : Integer.parseInt(sb.toString());
    }
}
