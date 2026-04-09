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

package de.Keyle.MyPet.compat.v1_21_R6.entity.types;

import de.Keyle.MyPet.api.entity.EntitySize;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.types.MyGiant;
import de.Keyle.MyPet.compat.v1_21_R6.entity.EntityMyPet;
import net.minecraft.world.level.Level;
import org.bukkit.Sound;

@EntitySize(width = 6.0f, height = 10.440001F)
public class EntityMyGiant extends EntityMyPet {

    public EntityMyGiant(Level world, MyPet myPet) {
        super(world, myPet);
    }

    @Override
    protected String getMyPetDeathSound() {
        return "entity.zombie.death";
    }

    @Override
    protected String getHurtSound() {
        return "entity.zombie.hurt";
    }

    @Override
    protected String getLivingSound() {
        return "entity.zombie.ambient";
    }

    @Override
    public void playPetStepSound() {
        getBukkitEntity().getWorld().playSound(getBukkitEntity().getLocation(), Sound.ENTITY_ZOMBIE_STEP, 0.15F, 1.0F);
    }

    @Override
    public void setPathfinder() {
        super.setPathfinder();
        if (myPet.getDamage() > 0) {
            var mobGoals = org.bukkit.Bukkit.getMobGoals();
            org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) getBukkitEntity();
            mobGoals.addGoal(mob, 5, new de.Keyle.MyPet.entity.ai.attack.PetMeleeAttackGoal(getBukkitEntity(), 0.1F, 8, 20));
        }
    }

    @Override
    public MyGiant getMyPet() {
        return (MyGiant) myPet;
    }
}
