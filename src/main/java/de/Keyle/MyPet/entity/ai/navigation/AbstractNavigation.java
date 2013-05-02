/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.entity.types.EntityMyPet;
import net.minecraft.server.v1_5_R3.EntityLiving;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public abstract class AbstractNavigation
{
    protected EntityMyPet entityMyPet;
    NavigationParameters parameters;

    public abstract void stop();

    public abstract boolean navigateTo(double x, double y, double z);

    public abstract void applyNavigationParameters();

    public AbstractNavigation(EntityMyPet entityMyPet)
    {
        this.entityMyPet = entityMyPet;
        parameters = new NavigationParameters(entityMyPet.getWalkSpeed());
    }

    public AbstractNavigation(EntityMyPet entityMyPet, NavigationParameters parameters)
    {
        this.entityMyPet = entityMyPet;
        this.parameters = parameters;
    }

    public boolean navigateTo(Location loc)
    {
        return navigateTo(loc.getX(), loc.getY(), loc.getZ());
    }

    public boolean navigateTo(LivingEntity entity)
    {
        return navigateTo(entity.getLocation());
    }

    public boolean navigateTo(EntityLiving entity)
    {
        return navigateTo((LivingEntity) entity.getBukkitEntity());
    }

    public NavigationParameters getParameters()
    {
        return parameters;
    }

    public abstract void tick();
}
