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

package de.Keyle.MyPet.compat.v1_9_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWither;
import de.Keyle.MyPet.compat.v1_9_R1.entity.EntityMyPet;
import net.minecraft.server.v1_9_R1.DataWatcher;
import net.minecraft.server.v1_9_R1.DataWatcherObject;
import net.minecraft.server.v1_9_R1.DataWatcherRegistry;
import net.minecraft.server.v1_9_R1.World;

@EntitySize(width = 0.9999F, height = 3.5F)
public class EntityMyWither extends EntityMyPet {
    private static final DataWatcherObject<Integer> targetWatcher = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> watcher_1 = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> watcher_2 = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);
    private static final DataWatcherObject<Integer> invulnerabilityWatcher = DataWatcher.a(EntityMyWither.class, DataWatcherRegistry.b);

    public EntityMyWither(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.wither.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.wither.hurt";
    }

    protected String getLivingSound() {
        return "entity.wither.ambient";
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(targetWatcher, 0);          // target entityID
        this.datawatcher.register(watcher_1, 0);              // N/A
        this.datawatcher.register(watcher_2, 0);              // N/A
        this.datawatcher.register(invulnerabilityWatcher, 0); // invulnerability (blue, size)
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(invulnerabilityWatcher, getMyPet().isBaby() ? 600 : 0);
    }

    /**
     * -> disable falldamage
     */
    public void e(float f, float f1) {
    }

    public MyWither getMyPet() {
        return (MyWither) myPet;
    }
}