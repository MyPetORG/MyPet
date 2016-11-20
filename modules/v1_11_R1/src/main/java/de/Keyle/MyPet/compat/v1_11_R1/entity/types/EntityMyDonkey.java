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

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import net.minecraft.server.v1_11_R1.DataWatcher;
import net.minecraft.server.v1_11_R1.DataWatcherObject;
import net.minecraft.server.v1_11_R1.DataWatcherRegistry;
import net.minecraft.server.v1_11_R1.World;

@EntitySize(width = 1.4F, height = 1.6F)
public class EntityMyDonkey extends EntityMyHorse {

    private static final DataWatcherObject<Boolean> chestWatcher = DataWatcher.a(EntityMyMule.class, DataWatcherRegistry.h);

    public EntityMyDonkey(World world, MyPet myPet) {
        super(world, myPet);
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(chestWatcher, false);
    }

    @Override
    public void updateVisuals() {
        super.updateVisuals();
        this.datawatcher.set(chestWatcher, getMyPet().hasChest());
    }

    @Override
    protected String getDeathSound() {
        return "entity.donkey.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.donkey.hurt";
    }

    protected String getLivingSound() {
        return "entity.donkey.ambient";
    }
}