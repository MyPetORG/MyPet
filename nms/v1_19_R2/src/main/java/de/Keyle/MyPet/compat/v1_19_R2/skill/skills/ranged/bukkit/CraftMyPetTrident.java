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

package de.Keyle.MyPet.compat.v1_19_R2.skill.skills.ranged.bukkit;

import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftTrident;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.MyPetMinecraftEntity;
import de.Keyle.MyPet.api.entity.skill.ranged.CraftMyPetProjectile;
import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import net.minecraft.world.entity.projectile.ThrownTrident;

@Compat("v1_19_R2")
public class CraftMyPetTrident extends CraftTrident implements CraftMyPetProjectile {

    public CraftMyPetTrident(CraftServer server, ThrownTrident entity) {
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
    public ItemStack getItem() {
        return CraftItemStack.asBukkitCopy(getHandle().tridentItem);
    }

    @Override
    public void setItem(@NotNull ItemStack itemStack) {
        getHandle().tridentItem = CraftItemStack.asNMSCopy(itemStack);
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
