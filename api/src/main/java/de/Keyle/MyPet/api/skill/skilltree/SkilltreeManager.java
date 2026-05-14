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

package de.Keyle.MyPet.api.skill.skilltree;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;
import de.Keyle.MyPet.api.skill.skilltree.requirements.RequirementName;
import de.Keyle.MyPet.api.util.AnnotationLookup;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;

import java.util.*;

/**
 * Central service for managing all registered {@link Skilltree}s and {@link Requirement}s.
 *
 * <p>Skilltrees are loaded from {@code .st.json} files during plugin startup and registered
 * here by name. The manager provides lookup, ordering, weighted random selection, and
 * requirement evaluation for the pet skilltree selection UI.
 *
 * <p>Requirements are registered by their {@link RequirementName} annotation and evaluated
 * at runtime to determine if a pet qualifies for a given skilltree.
 *
 * <p>Loaded during {@link Load.State#OnLoad} so that skilltrees are available before pet
 * data is restored from the repository.
 */
@Load(Load.State.OnLoad)
@ServiceName("SkilltreeManager")
public class SkilltreeManager implements ServiceContainer {

    final Map<String, Skilltree> skilltrees = new HashMap<>();
    final Map<String, Requirement> requirements = new HashMap<>();

    @Override
    public void onDisable() {
        clearSkilltrees();
    }

    /** Registers a skilltree, keyed by its name. Overwrites any existing tree with the same name. */
    public void registerSkilltree(Skilltree skilltree) {
        this.skilltrees.put(skilltree.getName(), skilltree);
    }

    /** Returns the skilltree with the given name, or {@code null} if none is registered. */
    public Skilltree getSkilltree(String name) {
        return this.skilltrees.get(name);
    }

    /** Returns the set of all registered skilltree names. */
    public Set<String> getSkilltreeNames() {
        return this.skilltrees.keySet();
    }

    /** Returns skilltree names sorted by their {@link Skilltree#getOrder()} value. */
    public List<String> getOrderedSkilltreeNames() {
        List<String> names = new LinkedList<>(this.skilltrees.keySet());
        names.sort(Comparator.comparingInt(o -> this.skilltrees.get(o).getOrder()));
        return names;
    }

    /** Returns all registered skilltree instances (unordered). */
    public Collection<Skilltree> getSkilltrees() {
        return this.skilltrees.values();
    }

    /** Returns all registered skilltrees sorted by their {@link Skilltree#getOrder()} value. */
    public List<Skilltree> getOrderedSkilltrees() {
        List<Skilltree> skilltrees = new LinkedList<>(this.skilltrees.values());
        skilltrees.sort(Comparator.comparingInt(Skilltree::getOrder));
        return skilltrees;
    }

    /**
     * Selects a random skilltree for the given pet using weighted probability.
     *
     * <p>Only skilltrees that match the pet's type, pass all requirements, and have a
     * positive weight are candidates. The selection uses a cumulative-weight random
     * algorithm so that trees with higher weights are proportionally more likely.
     *
     * @param pet the pet to select a skilltree for
     * @return a randomly selected skilltree, or {@code null} if no candidates qualify
     */
    public Skilltree getRandomSkilltree(Pet pet) {
        TreeMap<Double, Skilltree> skilltreeMap = new TreeMap<>();
        List<Skilltree> skilltrees = new ArrayList<>(MyPetApi.getSkilltreeManager().getSkilltrees());
        if (skilltrees.isEmpty()) {
            return null;
        }

        double totalWeight = 0;
        for (Skilltree skilltree : skilltrees) {
            if (skilltree.getMobTypes().contains(pet.getPetType()) && skilltree.checkRequirements(pet) && skilltree.getWeight() > 0) {
                skilltreeMap.put(totalWeight, skilltree);
                totalWeight += skilltree.getWeight();
            }
        }

        double num = (1 - Util.getRandom().nextDouble()) * totalWeight;
        Double key = skilltreeMap.floorKey(num);
        if (key == null) {
            return null;
        }
        return skilltreeMap.get(key);
    }

    /** Returns {@code true} if a skilltree with the given name is registered. */
    public boolean hasSkilltree(String name) {
        return this.skilltrees.containsKey(name);
    }

    /** Removes all registered skilltrees. Typically called during reload or shutdown. */
    public void clearSkilltrees() {
        this.skilltrees.clear();
    }

    /**
     * Registers a requirement implementation, keyed by the name from its {@link RequirementName}
     * annotation (case-insensitive).
     *
     * @param Requirement the requirement instance to register
     */
    public void registerRequirement(Requirement Requirement) {
        String requirementName = getRequirementName(Requirement.getClass());
        if (requirementName == null) {
            throw new IllegalArgumentException(
                    Requirement.getClass().getName() + " is not annotated with @RequirementName");
        }
        requirements.put(requirementName.toLowerCase(), Requirement);
    }

    /**
     * Looks up a requirement by name (case-insensitive).
     *
     * @param requirementName the requirement name to look up
     * @return the requirement instance, or {@code null} if not registered
     */
    public Requirement getRequirement(String requirementName) {
        return requirements.get(requirementName.toLowerCase());
    }

    /**
     * Resolves the requirement name for a class by searching its type hierarchy for a
     * {@link RequirementName} annotation.
     *
     * <p>Searches the class itself, then its superclass chain, then all implemented interfaces
     * recursively.
     *
     * @param clazz the class to inspect
     * @return the annotated requirement name, or {@code null} if not found
     */
    public String getRequirementName(Class<?> clazz) {
        return AnnotationLookup.findName(clazz, RequirementName.class, Requirement.class, RequirementName::value);
    }

    /** Unregisters a requirement by name. */
    public void removeRequirement(String requirementName) {
        requirements.remove(requirementName);
    }

    /** Unregisters a requirement by resolving the name from its class annotation. */
    public void removeRequirement(Requirement requirement) {
        String requirementName = getRequirementName(requirement.getClass());
        removeRequirement(requirementName);
    }
}