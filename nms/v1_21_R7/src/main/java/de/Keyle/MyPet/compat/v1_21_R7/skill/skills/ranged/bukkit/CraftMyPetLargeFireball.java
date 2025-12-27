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

package de.Keyle.MyPet.compat.v1_21_R7.skill.skills.ranged.bukkit;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftLargeFireball;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

@Compat("v1_21_R7")
public class CraftMyPetLargeFireball extends CraftLargeFireball implements CraftMyPetProjectile {

    public CraftMyPetLargeFireball(CraftServer server, LargeFireball entity) {
        super(server, entity);
    }

    @Override
    public EntityMyPetProjectile getMyPetProjectile() {
        return ((EntityMyPetProjectile) this.getHandle());
    }

    @Override
    public MyPetBukkitEntity getShootingMyPet() {
        Entity shooter = getMyPetProjectile().getShooter();
        return shooter instanceof MyPetBukkitEntity myPetBukkit ? myPetBukkit : null;
    }

    @Override
    public boolean isInWater() {
        return getHandle().isInWater();
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void setPersistent(boolean b) {
    }

    @NotNull
    @Override
    public SpawnCategory getSpawnCategory() {
        return SpawnCategory.MISC;
    }
}
