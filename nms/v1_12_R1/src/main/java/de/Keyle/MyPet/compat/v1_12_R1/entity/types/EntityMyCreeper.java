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

package de.Keyle.MyPet.compat.v1_12_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyCreeper;
import de.Keyle.MyPet.compat.v1_12_R1.entity.EntityMyPet;
import net.minecraft.server.v1_12_R1.DataWatcher;
import net.minecraft.server.v1_12_R1.DataWatcherObject;
import net.minecraft.server.v1_12_R1.DataWatcherRegistry;
import net.minecraft.server.v1_12_R1.World;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyCreeper extends EntityMyPet {

    private static final DataWatcherObject<Integer> FUSE_WATCHER = DataWatcher.a(EntityMyCreeper.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Boolean> POWERED_WATCHER = DataWatcher.a(EntityMyCreeper.class, DataWatcherRegistry.h);
    private static final DataWatcherObject<Boolean> UNUSED_WATCHER = DataWatcher.a(EntityMyCreeper.class, DataWatcherRegistry.h);

    public EntityMyCreeper(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.creeper.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.creeper.hurt";
    }

    @Override
    protected String getLivingSound() {
        return null;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(FUSE_WATCHER, -1);        // fuse
        this.datawatcher.register(POWERED_WATCHER, false);  // powered
        this.datawatcher.register(UNUSED_WATCHER, false);         // N/A
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(POWERED_WATCHER, getMyPet().isPowered());
    }

    public MyCreeper getMyPet() {
        return (MyCreeper) myPet;
    }
}