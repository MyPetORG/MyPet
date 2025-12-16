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

package de.Keyle.MyPet.compat.v1_19_R1.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyStray;
import de.Keyle.MyPet.compat.v1_19_R1.entity.EntityMyPet;
import net.minecraft.world.level.Level;
import org.bukkit.Sound;

@EntitySize(width = 0.6F, height = 1.9F)
public class EntityMyStray extends EntityMyPet {

    public EntityMyStray(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.stray.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.stray.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.stray.ambient";
    }

    @Override
    public void playPetStepSound() {
        getBukkitEntity().getWorld().playSound(getBukkitEntity().getLocation(), Sound.ENTITY_STRAY_STEP, 0.15F, 1.0F);
    }

    @Override
    public MyStray getMyPet() {
        return (MyStray) myPet;
    }
}
