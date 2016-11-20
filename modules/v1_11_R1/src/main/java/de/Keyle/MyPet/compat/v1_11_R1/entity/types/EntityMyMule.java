/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_11_R1.entity.types;

import com.google.common.base.Optional;
import de.Keyle.MyPet.api.entity.MyPet;
import net.minecraft.server.v1_11_R1.DataWatcher;
import net.minecraft.server.v1_11_R1.DataWatcherObject;
import net.minecraft.server.v1_11_R1.DataWatcherRegistry;
import net.minecraft.server.v1_11_R1.World;

public class EntityMyMule extends EntityMyHorse {

    private static final DataWatcherObject<Boolean> chestWatcher = DataWatcher.a(EntityMyMule.class, DataWatcherRegistry.h);

    public EntityMyMule(World world, MyPet myPet) {
        super(world, myPet);
    }

    protected void initDatawatcher() {
        this.datawatcher.register(ageWatcher, false);
        this.datawatcher.register(saddleChestWatcher, (byte) 0);
        this.datawatcher.register(ownerWatcher, Optional.absent());
        this.datawatcher.register(chestWatcher, false);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(ageWatcher, getMyPet().isBaby());
        this.datawatcher.set(chestWatcher, getMyPet().hasChest());
        applyVisual(8, getMyPet().hasChest());
        applyVisual(4, getMyPet().hasSaddle());
    }

    @Override
    protected String getDeathSound() {
        return "entity.mule.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.mule.hurt";
    }

    protected String getLivingSound() {
        return "entity.mule.ambient";
    }
}