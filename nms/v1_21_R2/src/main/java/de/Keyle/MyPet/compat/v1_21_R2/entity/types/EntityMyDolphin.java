/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.compat.v1_21_R2.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyDolphin;
import de.Keyle.MyPet.compat.v1_21_R2.entity.EntityMyAquaticPet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import org.bukkit.Particle;

@EntitySize(width = 0.9F, height = 0.6f)
public class EntityMyDolphin extends EntityMyAquaticPet {
    private static final EntityDataAccessor<BlockPos> TREASURE_POS_WATCHER = SynchedEntityData.defineId(EntityMyDolphin.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> GOT_FISH_WATCHER = SynchedEntityData.defineId(EntityMyDolphin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOISTNESS_WATCHER = SynchedEntityData.defineId(EntityMyDolphin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AGE_WATCHER = SynchedEntityData.defineId(EntityMyDolphin.class, EntityDataSerializers.BOOLEAN);
    public boolean canDolphinjump = false;

    public EntityMyDolphin(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.dolphin.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.dolphin.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.dolphin.ambient";
    }

    @Override
    public MyDolphin getMyPet() {
        return (MyDolphin) myPet;
    }

    @Override
    public void updateVisuals() {
        getEntityData().set(AGE_WATCHER, getMyPet().isBaby());
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!isInWater() && this.random.nextBoolean()) {
            myPet.getLocation().get().getWorld().spawnParticle(Particle.SPLASH, myPet.getLocation().get().add(0, 0.7, 0), 10, 0.2F, 0.2F, 0.2F, 0.5F);
        }
        if (!this.canDolphinjump &&
                (this.level().getBlockState(new BlockPos(this.getBlockX(), this.getBlockY() + 3, this.getBlockZ())).liquid())) {
            this.canDolphinjump = true;
        }
        if (this.canDolphinjump &&
                this.onGround && !this.isInWaterOrBubble()) {
            this.canDolphinjump = false;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(TREASURE_POS_WATCHER, BlockPos.ZERO);
        builder.define(GOT_FISH_WATCHER, false);
        builder.define(MOISTNESS_WATCHER, 2400);
        builder.define(AGE_WATCHER, false);
    }
}
