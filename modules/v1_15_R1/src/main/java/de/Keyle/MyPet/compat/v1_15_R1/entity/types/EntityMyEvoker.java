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

package de.Keyle.MyPet.compat.v1_15_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyEvoker;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.DataWatcher;
import net.minecraft.server.v1_15_R1.DataWatcherObject;
import net.minecraft.server.v1_15_R1.DataWatcherRegistry;
import net.minecraft.server.v1_15_R1.World;

@EntitySize(width = 0.6F, height = 1.95F)
public class EntityMyEvoker extends EntityMyPet {

    protected static final DataWatcherObject<Boolean> RAID_WATCHER = DataWatcher.a(EntityMyEvoker.class, DataWatcherRegistry.i);
    protected static final DataWatcherObject<Byte> SPELL_WATCHER = DataWatcher.a(EntityMyEvoker.class, DataWatcherRegistry.a);

    public EntityMyEvoker(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound() {
        return "entity.evoker.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.evoker.hurt";
    }

    /**
     * Returns the default sound of the MyPet
     */
    protected String getLivingSound() {
        return "entity.evoker.ambient";
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(RAID_WATCHER, false);
        getDataWatcher().register(SPELL_WATCHER, (byte) 0);
    }

    public MyEvoker getMyPet() {
        return (MyEvoker) myPet;
    }
}