/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;

public class Float extends AIGoal {
    private EntityMyPet entityMyPet;
    private EntityPlayer owner;

    private int lavaCounter = 10;
    private boolean inLava = false;

    public Float(EntityMyPet entityMyPet) {
        this.entityMyPet = entityMyPet;
        entityMyPet.getNavigation().e(true);
        this.owner = ((CraftPlayer) entityMyPet.getOwner().getPlayer()).getHandle();
    }

    @Override
    public boolean shouldStart() {
        return entityMyPet.world.containsLiquid(entityMyPet.boundingBox);
    }

    @Override
    public void finish() {
        inLava = false;
    }

    @Override
    public void tick() {
        entityMyPet.motY += 0.05D;

        if (inLava && lavaCounter-- <= 0) {
            if (entityMyPet.petNavigation.navigateTo(owner)) {
                lavaCounter = 10;
            }
        }
        if (!inLava && entityMyPet.world.e(entityMyPet.boundingBox)) // e -> is in Fire/Lava
        {
            inLava = true;
        }
    }
}