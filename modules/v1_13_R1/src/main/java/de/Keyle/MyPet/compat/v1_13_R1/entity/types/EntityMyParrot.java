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

package de.Keyle.MyPet.compat.v1_13_R1.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyParrot;
import de.Keyle.MyPet.compat.v1_13_R1.entity.EntityMyPet;
import net.minecraft.server.v1_13_R1.*;

import java.util.Optional;
import java.util.UUID;

@EntitySize(width = 0.5F, height = 0.9f)
public class EntityMyParrot extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyParrot.class, DataWatcherRegistry.i);
    protected static final DataWatcherObject<Byte> SIT_WATCHER = DataWatcher.a(EntityMyParrot.class, DataWatcherRegistry.a);
    protected static final DataWatcherObject<Optional<UUID>> OWNER_WATCHER = DataWatcher.a(EntityMyParrot.class, DataWatcherRegistry.o);
    private static final DataWatcherObject<Integer> VARIANT_WATCHER = DataWatcher.a(EntityMyParrot.class, DataWatcherRegistry.b);

    public EntityMyParrot(World world, MyPet myPet) {
        super(EntityTypes.PARROT, world, myPet);
    }

    public MyParrot getMyPet() {
        return (MyParrot) myPet;
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound() {
        return "entity.parrot.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.parrot.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.parrot.ambient";
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(AGE_WATCHER, false);
        this.datawatcher.register(SIT_WATCHER, (byte) 0);
        this.datawatcher.register(OWNER_WATCHER, Optional.empty());
        this.datawatcher.register(VARIANT_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.set(VARIANT_WATCHER, getMyPet().getVariant());
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Parrot.CAN_GLIDE)
        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }
    }

    /**
     * -> disable falldamage
     */
    public void c(float f, float f1) {
        if (!Configuration.MyPet.Parrot.CAN_GLIDE) {
            super.c(f, f1);
        }
    }
}