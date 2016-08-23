/*
 * This file is part of mypet-api_main
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api_main is licensed under the GNU Lesser General Public License.
 *
 * mypet-api_main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api_main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util.location;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class EntityLocationHolder extends LocationHolder<Entity> {

    private Entity entity;

    public EntityLocationHolder(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Location getLocation() {
        return entity.getLocation();
    }

    @Override
    public Entity getHolder() {
        return entity;
    }

    @Override
    public boolean isValid() {
        return entity != null && !entity.isDead();
    }
}