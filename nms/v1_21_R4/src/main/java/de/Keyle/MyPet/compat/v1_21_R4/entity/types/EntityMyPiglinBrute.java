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

package de.Keyle.MyPet.compat.v1_21_R4.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyPiglinBrute;
import de.Keyle.MyPet.compat.v1_21_R4.entity.EntityMyPet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyPiglinBrute extends EntityMyPet {
    private static final EntityDataAccessor<Boolean> NO_SHAKE_WATCHER = SynchedEntityData.defineId(EntityMyPiglinBrute.class, EntityDataSerializers.BOOLEAN);

    public EntityMyPiglinBrute(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.piglin_brute.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.piglin_brute.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.piglin_brute.ambient";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NO_SHAKE_WATCHER, false);
    }

    /**
     * Returns the speed of played sounds
     * The faster the higher the sound will be
     */
    @Override
    public float getSoundSpeed() {
        return super.getSoundSpeed() + 0.4F;
    }

    @Override
    public void updateVisuals() {
        this.getEntityData().set(NO_SHAKE_WATCHER, getMyPet().isShakeImmune());
        super.updateVisuals();
    }

    @Override
    public MyPiglinBrute getMyPet() {
        return (MyPiglinBrute) myPet;
    }

    @Override
    protected boolean checkInteractCooldown() {
        boolean val = super.checkInteractCooldown();
        this.interactCooldown = 5;
        return val;
    }
}
