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

package de.Keyle.MyPet.entity.ai.navigation;

import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.entity.PetAttributes;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Version-independent {@link AbstractNavigation} that drives a pet's
 * movement through Paper's {@link com.destroystokyo.paper.entity.Pathfinder}
 * API. Wraps a real vanilla Bukkit {@link Mob}.
 */
public class PaperNavigation extends AbstractNavigation {

    private final Mob mob;

    public PaperNavigation(Mob mob, double walkSpeed) {
        super(walkSpeed);
        this.mob = mob;
        parameters.setOnSpeedChange(this::applyNavigationParameters);
    }

    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
        applyNavigationParameters();
    }

    @Override
    public boolean navigateTo(double x, double y, double z) {
        if (mob.getPathfinder().moveTo(new Location(mob.getWorld(), x, y, z))) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    @Override
    public boolean navigateTo(LivingEntity entity) {
        if (mob.getPathfinder().moveTo(entity)) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        // No-op. Paper's Pathfinder advances automatically per tick.
    }

    @Override
    public void applyNavigationParameters() {
        mob.getPathfinder().setCanFloat(!parameters.avoidWater());
        var speedAttr = mob.getAttribute(PetAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(parameters.getSpeed() + parameters.speedModifier());
        }
    }
}
