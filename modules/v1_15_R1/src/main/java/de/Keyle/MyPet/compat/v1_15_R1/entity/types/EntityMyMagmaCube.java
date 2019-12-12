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
import de.Keyle.MyPet.api.entity.types.MyMagmaCube;
import de.Keyle.MyPet.compat.v1_15_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_15_R1.entity.ai.attack.MeleeAttack;
import net.minecraft.server.v1_15_R1.*;

@EntitySize(width = 0.5100001F, height = 0.5100001F)
public class EntityMyMagmaCube extends EntityMyPet {

    private static final DataWatcherObject<Integer> SIZE_WATCHER = DataWatcher.a(EntityMyMagmaCube.class, DataWatcherRegistry.b);

    int jumpDelay;

    public EntityMyMagmaCube(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "entity.magma_cube.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.magma_cube.hurt";
    }

    protected String getLivingSound() {
        return null;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        getDataWatcher().register(SIZE_WATCHER, 1); //size
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (this.onGround && jumpDelay-- <= 0) {
            getControllerJump().jump();
            jumpDelay = (this.random.nextInt(20) + 50);
            this.makeSound("entity.magma_cube.jump", 1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
        }
    }

    @Override
    public void updateVisuals() {
        int size = Math.max(1, getMyPet().getSize());
        getDataWatcher().set(SIZE_WATCHER, size);
        this.updateSize();
        if (petPathfinderSelector != null && petPathfinderSelector.hasGoal("MeleeAttack")) {
            petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3 + (getMyPet().getSize() * 0.51), 20));
        }
    }

    public net.minecraft.server.v1_15_R1.EntitySize a(EntityPose entitypose) {
        EntitySize es = this.getClass().getAnnotation(EntitySize.class);
        if (es != null) {
            int size = Math.max(1, getMyPet().getSize());
            float width = es.width();
            float height = java.lang.Float.isNaN(es.height()) ? width : es.height();
            return new net.minecraft.server.v1_15_R1.EntitySize(width * size, height * size, false);
        }
        return super.a(entitypose);
    }

    public MyMagmaCube getMyPet() {
        return (MyMagmaCube) myPet;
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3 + (getMyPet().getSize() * 0.51), 20));
    }
}