/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.LeashEntityHook;
import org.bukkit.entity.LivingEntity;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.EntityManager;
import uk.antiperson.stackmob.entity.StackEntity;

@PluginHookName("StackMob")
public class StackMobHook implements LeashEntityHook {

    protected EntityManager entityManager;

    @Override
    public boolean onEnable() {
        StackMob sm = (StackMob) MyPetApi.getPluginHookManager().getPluginInstance("StackMob").get();
        this.entityManager = new EntityManager(sm);
        return true;
    }

    @Override
    public boolean prepare(LivingEntity leashedEntity) {
        boolean unstacked = false;

        if (entityManager.isStackedEntity(leashedEntity)) {
            StackEntity stackedEntity = entityManager.getStackEntity(leashedEntity);
            int currentSize = stackedEntity.getSize();
            if (currentSize > 1) {
                stackedEntity.setSize(currentSize - 1);
                leashedEntity.setHealth(leashedEntity.getMaxHealth());
                unstacked = true;
            }
        }

        return !unstacked;
    }
}