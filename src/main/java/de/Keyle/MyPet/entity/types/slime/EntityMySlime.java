/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.types.slime;

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.ai.attack.MeleeAttack;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import net.minecraft.server.v1_8_R1.World;

@EntitySize(width = 0.5100001F, length = 0.5100001F, height = 0.5100001F)
public class EntityMySlime extends EntityMyPet {
    int jumpDelay;

    public EntityMySlime(World world, MyPet myPet) {
        super(world, myPet);
        this.jumpDelay = (this.random.nextInt(20) + 10);
    }

    @Override
    protected String getDeathSound() {
        return "mob.slime." + (getMyPet().getSize() > 1 ? "big" : "small");

    }

    @Override
    protected String getHurtSound() {
        return getDeathSound();
    }

    protected String getLivingSound() {
        return null;
    }

    public void setSize(int value) {
        value = Math.max(1, value);
        this.datawatcher.watch(16, new Byte((byte) value));
        EntitySize es = EntityMySlime.class.getAnnotation(EntitySize.class);
        if (es != null) {
            this.a(es.width() * value, es.length() * value);
        }
        if (petPathfinderSelector != null && petPathfinderSelector.hasGoal("MeleeAttack")) {
            petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 3 + (getMyPet().getSize() * 0.6), 20));
        }
    }

    public float getHeadHeight() {
        return length;
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(16, new Byte((byte) 1)); //size
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();

        if (this.onGround && jumpDelay-- <= 0) {
            getControllerJump().a();
            jumpDelay = (this.random.nextInt(20) + 10);
            if (getGoalTarget() != null) {
                jumpDelay /= 3;
            }
            makeSound(getDeathSound(), bf(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
        }
    }

    public void setMyPet(MyPet myPet) {
        if (myPet != null) {
            super.setMyPet(myPet);

            setSize(getMyPet().getSize());
        }
    }

    public MySlime getMyPet() {
        return (MySlime) myPet;
    }

    public void setPathfinder() {
        super.setPathfinder();
        petPathfinderSelector.replaceGoal("MeleeAttack", new MeleeAttack(this, 0.1F, 2 + getMyPet().getSize(), 20));
    }
}