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

package de.Keyle.MyPet.compat.v1_12_R1.entity.ai.navigation;

import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.entity.ai.navigation.NavigationParameters;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.GenericAttributes;
import net.minecraft.server.v1_12_R1.Navigation;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

@Compat("v1_12_R1")
public class VanillaNavigation extends AbstractNavigation {
    Navigation nav;

    public VanillaNavigation(EntityMyPet entityMyPet) {
        super(entityMyPet);
        nav = (Navigation) entityMyPet.getNavigation();
    }

    public VanillaNavigation(EntityMyPet entityMyPet, NavigationParameters parameters) {
        super(entityMyPet, parameters);
        nav = (Navigation) entityMyPet.getNavigation();
    }

    @Override
    public void stop() {
        nav.p();
    }

    @Override
    public boolean navigateTo(double x, double y, double z) {
        applyNavigationParameters();
        if (this.nav.a(x, y, z, 1.D)) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    @Override
    public boolean navigateTo(LivingEntity entity) {
        return navigateTo(((CraftLivingEntity) entity).getHandle());
    }

    public boolean navigateTo(EntityLiving entity) {
        if (this.nav.a(entity, 1.D)) {
            applyNavigationParameters();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        nav.d();
    }

    public void applyNavigationParameters() {
        this.nav.a(parameters.avoidWater());
        ((EntityMyPet) this.entityMyPet).getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(parameters.speed() + parameters.speedModifier());
    }
}