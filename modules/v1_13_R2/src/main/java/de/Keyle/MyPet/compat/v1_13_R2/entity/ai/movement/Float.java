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

package de.Keyle.MyPet.compat.v1_13_R2.entity.ai.movement;

import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_13_R2.entity.EntityMyPet;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.Navigation;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;

@Compat("v1_13_R2")
public class Float implements AIGoal {

    private EntityMyPet entityMyPet;
    private EntityPlayer owner;

    private int lavaCounter = 10;
    private boolean inLava = false;

    public Float(EntityMyPet entityMyPet) {
        this.entityMyPet = entityMyPet;
        //entityMyPet.getNavigation().e(true);
        this.owner = ((CraftPlayer) entityMyPet.getOwner().getPlayer()).getHandle();
        ((Navigation) entityMyPet.getNavigation()).c(true);
    }

    @Override
    public boolean shouldStart() {
        return entityMyPet.isInWater();
    }

    @Override
    public void finish() {
        inLava = false;
    }

    @Override
    public void tick() {
        entityMyPet.motY += 0.05D;

        if (inLava && lavaCounter-- <= 0) {
            if (entityMyPet.getPetNavigation().navigateTo(owner.getBukkitEntity())) {
                lavaCounter = 10;
            }
        }
        if (!inLava && entityMyPet.ax()) {
            inLava = true;
        }
    }
}