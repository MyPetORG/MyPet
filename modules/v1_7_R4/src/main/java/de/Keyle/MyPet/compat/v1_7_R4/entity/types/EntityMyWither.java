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

package de.Keyle.MyPet.compat.v1_7_R4.entity.types;

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyWither;
import de.Keyle.MyPet.compat.v1_7_R4.entity.EntityMyPet;
import net.minecraft.server.v1_7_R4.World;

@EntitySize(width = 1.9F, height = 3.5F)
public class EntityMyWither extends EntityMyPet {
    public EntityMyWither(World world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getDeathSound() {
        return "mob.wither.death";
    }

    @Override
    protected String getHurtSound() {
        return "mob.wither.hurt";
    }

    protected String getLivingSound() {
        return "mob.wither.idle";
    }

    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.a(17, 0);  // target entityID
        this.datawatcher.a(18, 0);  // N/A
        this.datawatcher.a(19, 0);  // N/A
        this.datawatcher.a(20, 0);  // invulnerability (blue, size)
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (Configuration.MyPet.Wither.CAN_GLIDE)
        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }
    }

    @Override
    public void updateVisuals() {
        this.datawatcher.watch(20, getMyPet().isBaby() ? 600 : 0);
    }

    /**
     * -> disable falldamage
     */
    public void b(float f) {
        if (!Configuration.MyPet.Wither.CAN_GLIDE) {
            super.b(f);
        }
    }

    public MyWither getMyPet() {
        return (MyWither) myPet;
    }
}