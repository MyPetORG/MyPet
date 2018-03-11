/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;

import java.util.*;

@Load(Load.State.OnLoad)
@ServiceName("ShopService")
public class SkilltreeManager implements ServiceContainer {
    Map<String, Skilltree> skilltrees = new HashMap<>();

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

    public boolean hasSkilltree(String name) {
        return this.skilltrees.containsKey(name);
    }

    public void clearSkilltrees() {
        this.skilltrees.clear();
    }

    @Override
    public void onDisable() {
        clearSkilltrees();
    }

    @Override
    public String getServiceName() {
        return "SkilltreeManager";
    }
}