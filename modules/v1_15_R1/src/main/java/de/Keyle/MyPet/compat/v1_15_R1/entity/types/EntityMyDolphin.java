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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.compat.ParticleCompat;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import net.minecraft.server.v1_15_R1.*;

@EntitySize(width = 0.9F, height = 0.6f)
public class EntityMyDolphin extends EntityMyPet {

    private static final DataWatcherObject<BlockPosition> TREASURE_POS_WATCHER = DataWatcher.a(EntityMyDolphin.class, DataWatcherRegistry.l);
    private static final DataWatcherObject<Boolean> GOT_FISH_WATCHER = DataWatcher.a(EntityMyDolphin.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Integer> MOISTNESS_WATCHER = DataWatcher.a(EntityMyDolphin.class, DataWatcherRegistry.b);

    public EntityMyDolphin(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.dolphin.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.dolphin.hurt";
    }

    protected String getLivingSound() {
        return "entity.dolphin.ambient";
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!isInWater() && this.random.nextBoolean()) {
            MyPetApi.getPlatformHelper().playParticleEffect(myPet.getLocation().get().add(0, 0.7, 0), ParticleCompat.WATER_SPLASH.get(), 0.2F, 0.2F, 0.2F, 0.5F, 10, 20);
        }
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();

        getDataWatcher().register(TREASURE_POS_WATCHER, BlockPosition.ZERO);
        getDataWatcher().register(GOT_FISH_WATCHER, false);
        getDataWatcher().register(MOISTNESS_WATCHER, 2400);
    }
}