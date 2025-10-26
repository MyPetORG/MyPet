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

package de.Keyle.MyPet.compat.v1_20_R4.skill.skills.ranged.bukkit;

import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.craftbukkit.v1_20_R4.entity.CraftWitherSkull;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import net.minecraft.world.entity.projectile.WitherSkull;
import org.bukkit.entity.SpawnCategory;
import org.jetbrains.annotations.NotNull;

@Compat("v1_20_R4")
public class CraftMyPetWitherSkull extends CraftWitherSkull implements CraftMyPetProjectile {

    public CraftMyPetWitherSkull(CraftServer server, WitherSkull entity) {
        super(server, entity);
    }

    @Override
	public EntityMyPetProjectile getMyPetProjectile() {
        return ((EntityMyPetProjectile) this.getHandle());
    }

    @Override
    public MyPetBukkitEntity getShootingMyPet() {
        MyPetMinecraftEntity shooter = getMyPetProjectile().getShooter();
        return shooter != null ? shooter.getBukkitEntity() : null;
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

    /* I have no clue why I need to override these other deprecated methods also don't need to be implemented so yea...*/
    @Override
    public void setVisibleByDefault(boolean b) {
    }

    @Override
    public boolean isVisibleByDefault() {
        return true;
    }
}
