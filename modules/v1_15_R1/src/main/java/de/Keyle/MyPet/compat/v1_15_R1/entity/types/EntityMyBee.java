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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyBee;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import net.minecraft.server.v1_15_R1.DataWatcher;
import net.minecraft.server.v1_15_R1.DataWatcherObject;
import net.minecraft.server.v1_15_R1.DataWatcherRegistry;
import net.minecraft.server.v1_15_R1.World;

@EntitySize(width = 0.6F, height = 0.6f)
public class EntityMyBee extends EntityMyPet {

    private static final DataWatcherObject<Boolean> AGE_WATCHER = DataWatcher.a(EntityMyBee.class, DataWatcherRegistry.i);
    private static final DataWatcherObject<Byte> BEE_STATUS_WATCHER = DataWatcher.a(EntityMyBee.class, DataWatcherRegistry.a);
    private static final DataWatcherObject<Integer> ANGER_WATCHER = DataWatcher.a(EntityMyBee.class, DataWatcherRegistry.b);

    protected boolean isAngry = false;

    public EntityMyBee(World world, MyPet myPet) {
        super(world, myPet);
    }

    /**
     * Returns the sound that is played when the MyPet dies
     */
    @Override
    protected String getDeathSound() {
        return "entity.bee.death";
    }

    /**
     * Returns the sound that is played when the MyPet get hurt
     */
    @Override
    protected String getHurtSound() {
        return "entity.bee.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.bee.pollinate";
    }

    public float getSoundSpeed() {
        return super.getSoundSpeed() * 0.95F;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(AGE_WATCHER, false);
        getDataWatcher().register(BEE_STATUS_WATCHER, (byte) 0);
        getDataWatcher().register(ANGER_WATCHER, 0);
    }

    @Override
    public void updateVisuals() {
        getDataWatcher().set(AGE_WATCHER, getMyPet().isBaby());
        getDataWatcher().set(ANGER_WATCHER, (getMyPet().isAngry() || isAngry) ? 1 : 0);
        this.setBeeStatus(8, getMyPet().hasNectar());
        this.setBeeStatus(4, getMyPet().hasStung());
    }

    /**
     * Possible status flags:
     * 8: Nectar
     * 4: Stung
     * 2: ?
     */
    private void setBeeStatus(int status, boolean flag) {
        if (flag) {
            this.datawatcher.set(BEE_STATUS_WATCHER, (byte) (this.datawatcher.get(BEE_STATUS_WATCHER) | status));
        } else {
            this.datawatcher.set(BEE_STATUS_WATCHER, (byte) (this.datawatcher.get(BEE_STATUS_WATCHER) & ~status));
        }

    }

    public MyBee getMyPet() {
        return (MyBee) myPet;
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Bee.CAN_GLIDE) {
            if (!this.onGround && this.getMot().y < 0.0D) {
                this.setMot(getMot().d(1, 0.6D, 1));
            }
        }
    }

    protected void doMyPetTick() {
        super.doMyPetTick();
        BehaviorImpl skill = getMyPet().getSkills().get(BehaviorImpl.class);
        Behavior.BehaviorMode behavior = skill.getBehavior();
        if (behavior == Behavior.BehaviorMode.Aggressive) {
            if (!isAngry) {
                isAngry = true;
                this.updateVisuals();
            }
        } else {
            if (isAngry) {
                isAngry = false;
                this.updateVisuals();
            }
        }
    }

    /**
     * -> disable falldamage
     */
    public int e(float f, float f1) {
        if (!Configuration.MyPet.Bee.CAN_GLIDE) {
            super.e(f, f1);
        }
        return 0;
    }
}