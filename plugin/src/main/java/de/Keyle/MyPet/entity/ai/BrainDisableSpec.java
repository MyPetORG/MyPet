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

package de.Keyle.MyPet.entity.ai;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.config.ConfigKeyRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.entity.Mob;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a pet's {@code MyPet.Pets.<Type>.Brain.Disabled} list to its vanilla
 * brain at spawn.
 *
 * <p>Entries are {@code activity:<name>} (empties a whole brain activity — the
 * only mechanism that reaches {@code BehaviorBuilder}-generated anonymous
 * behaviors) or {@code behavior:<SimpleClassName>} (removes a named
 * {@code Behavior} subclass). Anything else is malformed.
 *
 * <p>An entry whose name is empty after the prefix ({@code "behavior:"}) is
 * rejected rather than bucketed. {@code Class#getSimpleName} returns
 * {@code ""} for every anonymous class, and vanilla builds most modern
 * behaviors with {@code BehaviorBuilder} — so an empty name would match
 * nearly the whole brain and strip it wholesale. It would also remove
 * plenty, making the count non-zero, so the unmatched-entry warning below
 * would stay silent: a one-character config typo would lobotomise the mob
 * with no diagnostic at all. The {@code activity:} branch is guarded the
 * same way for consistency — an empty activity name is harmless today
 * (it matches nothing and warns correctly), but the two branches should
 * not differ on what counts as a valid entry.
 *
 * <p>Entries that remove nothing are warned about once per
 * {@code (petType, entry)} per server session — {@code apply} runs on every
 * spawn, so unconditional logging would spam. Silent failure is deliberately
 * not an option here: it is what hid a dead {@code SetRoarTarget} declaration
 * on the Warden for an entire release line.
 */
public final class BrainDisableSpec {

    private BrainDisableSpec() {}

    public static final String KEY = "Brain.Disabled";
    private static final String ACTIVITY_PREFIX = "activity:";
    private static final String BEHAVIOR_PREFIX = "behavior:";

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    public static void apply(Pet pet, Mob mob) {
        String petType = pet.getPetType().name();
        ConfigKey<?> key = ConfigKeyRegistry.lookup(petType, KEY);
        if (key == null) return;                       // goal-driven pet — no key declared
        if (!(key.get() instanceof List<?> raw) || raw.isEmpty()) return;

        Set<String> activities = new LinkedHashSet<>();
        Set<String> behaviors = new LinkedHashSet<>();
        for (Object element : raw) {
            if (!(element instanceof String entry)) continue;
            String trimmed = entry.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith(ACTIVITY_PREFIX)) {
                String name = trimmed.substring(ACTIVITY_PREFIX.length()).trim();
                if (name.isEmpty()) {
                    warnOnce(petType, trimmed, "has an empty name after the prefix", mob);
                    continue;
                }
                activities.add(name);
            } else if (lower.startsWith(BEHAVIOR_PREFIX)) {
                String name = trimmed.substring(BEHAVIOR_PREFIX.length()).trim();
                if (name.isEmpty()) {
                    warnOnce(petType, trimmed, "has an empty name after the prefix", mob);
                    continue;
                }
                behaviors.add(name);
            } else {
                warnOnce(petType, trimmed, "is not prefixed 'activity:' or 'behavior:'", mob);
            }
        }

        // One entry at a time so an unmatched entry can be named precisely.
        for (String activity : activities) {
            if (BrainAccess.removeBehaviorsByActivity(mob, Set.of(activity)) == 0) {
                warnOnce(petType, ACTIVITY_PREFIX + activity, "matched no brain activity", mob);
            }
        }
        for (String behavior : behaviors) {
            if (BrainAccess.removeBehaviorsByClassName(mob, Set.of(behavior)) == 0) {
                warnOnce(petType, BEHAVIOR_PREFIX + behavior, "matched no brain behavior", mob);
            }
        }
    }

    private static void warnOnce(String petType, String entry, String problem, Mob mob) {
        if (!WARNED.add(petType + "|" + entry)) return;
        String hint = "";
        if (problem.endsWith("activity")) {
            List<String> names = BrainAccess.activityNames(mob);
            // Empty on total reflection failure — omit the hint rather than render
            // "This pet's brain has: .", which would look like a real (empty) answer.
            if (!names.isEmpty()) {
                hint = " This pet's brain has: " + String.join(", ", names) + ".";
            }
        }
        MyPetApi.getLogger().warning("Brain.Disabled for " + petType + ": '" + entry + "' "
                + problem + "." + hint);
    }
}
