/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_9_R1.entity.types;

import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.types.MyGuardian;
import de.Keyle.MyPet.compat.v1_9_R1.entity.EntityMyPet;
import net.minecraft.server.v1_9_R1.*;

@EntitySize(width = 0.7F, height = 0.85F)
public class EntityMyGuardian extends EntityMyPet {
    private static final DataWatcherObject<Byte> elderWatcher = DataWatcher.a(EntityGuardian.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Integer> watcher = DataWatcher.a(EntityGuardian.class, DataWatcherRegistry.b);

    public EntityMyGuardian(World world, ActiveMyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        if (getMyPet().isElder()) {
            return "entity.guardian.elder.death";
        }
        return "entity.guardian.death";
    }

    @Override
    protected String getHurtSound() {
        if (getMyPet().isElder()) {
            return "entity.guardian.elder.hit";
        }
        return "entity.guardian.hit";
    }

    protected String getLivingSound() {
        if (getMyPet().isElder()) {
            return "entity.guardian.elder.idle";
        }
        return "entity.guardian.idle";
    }

    @Override
    public void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(elderWatcher, 0); // elder
        this.datawatcher.register(watcher, 0);      // N/A
    }

    @Override
    public void updateVisuals() {
        int old = this.datawatcher.get(elderWatcher).byteValue();
        if (getMyPet().isElder()) {
            this.datawatcher.set(elderWatcher, Byte.valueOf((byte) (old | 4)));
        } else {
            this.datawatcher.set(elderWatcher, Byte.valueOf((byte) (old & ~4)));
        }
    }

    public MyGuardian getMyPet() {
        return (MyGuardian) myPet;
    }
}