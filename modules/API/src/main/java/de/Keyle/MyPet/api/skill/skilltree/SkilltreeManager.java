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

package de.Keyle.MyPet.api.skill.skilltree;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skilltree.requirements.Requirement;
import de.Keyle.MyPet.api.skill.skilltree.requirements.RequirementName;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;

import java.util.*;

@Load(Load.State.OnLoad)
@ServiceName("SkilltreeManager")
public class SkilltreeManager implements ServiceContainer {

    Map<String, Skilltree> skilltrees = new HashMap<>();
    Map<String, Requirement> requirements = new HashMap<>();

    @Override
    public void onDisable() {
        clearSkilltrees();
    }

    public void registerSkilltree(Skilltree skilltree) {
        this.skilltrees.put(skilltree.getName(), skilltree);
    }

    public Skilltree getSkilltree(String name) {
        return this.skilltrees.get(name);
    }

    public Set<String> getSkilltreeNames() {
        return this.skilltrees.keySet();
    }

    public List<String> getOrderedSkilltreeNames() {
        List<String> names = new LinkedList<>(this.skilltrees.keySet());
        names.sort(Comparator.comparingInt(o -> this.skilltrees.get(o).getOrder()));
        return names;
    }

    public Collection<Skilltree> getSkilltrees() {
        return this.skilltrees.values();
    }

    public List<Skilltree> getOrderedSkilltrees() {
        List<Skilltree> skilltrees = new LinkedList<>(this.skilltrees.values());
        skilltrees.sort(Comparator.comparingInt(Skilltree::getOrder));
        return skilltrees;
    }

    public Skilltree getRandomSkilltree(MyPet pet) {
        TreeMap<Double, Skilltree> skilltreeMap = new TreeMap<>();
        List<Skilltree> skilltrees = new ArrayList<>(MyPetApi.getSkilltreeManager().getSkilltrees());
        if (skilltrees.size() == 0) {
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
        try {
            num = skilltreeMap.floorKey(num);
            return skilltreeMap.get(num);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean hasSkilltree(String name) {
        return this.skilltrees.containsKey(name);
    }

    public void clearSkilltrees() {
        this.skilltrees.clear();
    }

    public void registerRequirement(Requirement Requirement) {
        String requirementName = getRequirementName(Requirement.getClass());
        requirements.put(requirementName.toLowerCase(), Requirement);
    }

    public Requirement getRequirement(String requirementName) {
        return requirements.get(requirementName.toLowerCase());
    }

    public String getRequirementName(Class clazz) {
        if (clazz == Object.class) {
            return null;
        }
        if (Requirement.class.isAssignableFrom(clazz)) {
            RequirementName requirementName = (RequirementName) clazz.getAnnotation(RequirementName.class);
            if (requirementName != null) {
                return requirementName.value();
            }
        }
        String requirementName = getRequirementName(clazz.getSuperclass());
        if (requirementName != null) {
            return requirementName;
        }
        for (Class c : clazz.getInterfaces()) {
            requirementName = getRequirementName(c);
            if (requirementName != null) {
                return requirementName;
            }
        }
        return null;
    }

    public void removeRequirement(String requirementName) {
        requirements.remove(requirementName);
    }

    public void removeRequirement(Requirement requirement) {
        String requirementName = getRequirementName(requirement.getClass());
        removeRequirement(requirementName);
    }
}